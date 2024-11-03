import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import utils.TwoTuple;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.IntStream;


public class SolverCplex {
    private Double T_SLA;
    private WorkflowInfo jobs;
    private ServiceInfo serviceInfo;
    private double cpu_weight;
    private double mem_weight;
    private double disk_weight;

    private List<String> kind_list;
    private int[][] kind_matrix;
    private float[] deal_time;
    private int[][] deal_order;
    private int dim;
    private int M=9999999;

    private IloCplex cplex;
    private boolean out_flage;
    private String out_file_path;
    private int limit_time;
    private Map<TwoTuple<Integer, TwoTuple<Integer,Integer>>,Integer> z_map;         //用于存储选择变量Z对应的位置信息
    private double[][] x;
    private double[][] w_t;
//    private String object_state;

    public SolverCplex(Parameters4Solver all_parameters_f){
        //读取任务流
        jobs=all_parameters_f.getJobs();
        serviceInfo=all_parameters_f.getServiceInfo();
        cpu_weight=all_parameters_f.getCpu_weight();
        mem_weight=all_parameters_f.getMem_weight();
        disk_weight=all_parameters_f.getDisk_weight();
        T_SLA=jobs.getT_SLA();
        limit_time=all_parameters_f.getCplex_limit_time();
        kind_list=jobs.getAllJobKindsList();
        dim=jobs.getAllJobIdSet().size();

        deal_time=jobs.getAllJobTime();
        deal_order=jobs.getAllDealOrder();

        out_flage=all_parameters_f.getOut_flage();
        out_file_path= all_parameters_f.getOut_file_path();
//        object_state=IloCplex.Status.Infeasible.toString();
        initKindMatrix();
        initZmap();

    }
    private void initKindMatrix(){
        kind_matrix=new int[dim][dim];
        int i=0;
        for (String kind_x:kind_list){
            int j=0;
            for (String kind_y:kind_list){
                if (kind_x.equals(kind_y)){
                    kind_matrix[i][j]=1;
                }else{
                    kind_matrix[i][j]=0;
                }
                j++;
            }
            i++;
        }
        //System.out.println("kind_matrix initialization successds!");
    }
    private void initZmap(){
        z_map=new HashMap<>();
        int z_order=0;
        for(int i=0;i<dim;i++) {
            for (int j = 0; j < dim; j++) {
                if (kind_matrix[i][j] == 0) {
                    continue;
                }
                for (int l = 0; l < j; l++) {
                    if (kind_matrix[j][l]==0){
                        continue;
                    }
                    z_map.put(new TwoTuple<>(i,new TwoTuple<>(j,l)),z_order);
                    z_order+=1;

                }
            }
        }
    }

    public double run(){
        double solution=-1;
        try{
            System.out.println("Starting Cplex Solver ...");
            long start_time=System.currentTimeMillis();
            cplex = new IloCplex();
//            cplex.setParam(IloCplex.IntParam.Threads,50);
            if (!out_flage){
                cplex.setOut(null);
            }

            //cplex.setParam(IloCplex.DoubleParam.EpGap,0.90);
            cplex.setParam(IloCplex.DoubleParam.WorkMem,1024*2);
            cplex.setParam(IloCplex.DoubleParam.TreLim,1024*32);
            cplex.setParam(IloCplex.IntParam.NodeFileInd,2);
//            cplex.setParam(IloCplex.IntParam.Threads,40);


            if (limit_time>0){
                cplex.setParam(IloCplex.DoubleParam.TimeLimit,limit_time);
            }

            //定义决策变量
            IloNumVar[][] X=new IloNumVar[dim][dim];          //0,1变量
            IloNumVar[][] work_time=new IloNumVar[dim][2];          //浮点数变量
            IloNumVar[] Z=new IloNumVar[z_map.size()];            //0,1变量

            //变量初始化
            for (int i:IntStream.range(0,dim).toArray()){
                for (int j:IntStream.range(0,dim).toArray()){
                    X[i][j]=cplex.numVar(0,1,IloNumVarType.Int);
                }
                work_time[i][0]=cplex.numVar(0,T_SLA,IloNumVarType.Float); //这里就是int
                work_time[i][1]=cplex.numVar(0,T_SLA,IloNumVarType.Float);
            }

            for (int i=0;i<z_map.size();i++){
                Z[i]=cplex.numVar(0,1,IloNumVarType.Int);
            }

            //目标

            double[] points={0,1};
            double[] slopes={0,1,0};
            IloNumExpr obj=cplex.numExpr();
            for (int i:IntStream.range(0,dim).toArray()){
                IloNumExpr temp=cplex.numExpr();
                for(int j:IntStream.range(0,dim).toArray()){
                    temp=cplex.sum(temp,cplex.prod(X[i][j],kind_matrix[i][j]));
                }
                //对分段线性函数进行求和
                String service_kind=jobs.getJobKind(i);
                double value=cpu_weight*serviceInfo.getCpu_resource_map().get(service_kind)/ServiceInfo.getCpu_ave()+
                        mem_weight*serviceInfo.getMem_resource_map().get(service_kind)/ServiceInfo.getMem_ave()+
                        disk_weight*serviceInfo.getDisk_resource_map().get(service_kind)/ServiceInfo.getDisk_ave();
                obj=cplex.sum(obj,cplex.prod(cplex.piecewiseLinear(temp,points,slopes,0,0),value));
            }
            cplex.addMinimize(obj);

            //约束1：一个任务仅由一个服务执行
            for (int j:IntStream.range(0,dim).toArray()){
                IloNumExpr constrain1=cplex.numExpr();
                for (int i:IntStream.range(0,dim).toArray()){
                    if (kind_matrix[i][j]==1){
                        constrain1=cplex.sum(constrain1,cplex.prod(X[i][j],kind_matrix[i][j]));
                    }else{
                        cplex.addEq(X[i][j],0);
                    }

                }
                cplex.addEq(constrain1,1);
            }
            //约束2：时间约束
            for (int j:IntStream.range(0,dim).toArray()){
                //工作执行时间约束
                cplex.addLe(cplex.sum(work_time[j][0],deal_time[j]),work_time[j][1]);//这里需要测试
                //约束时间范围内完成所有工作。SLA约束(work time 变量范围设置时已经包含这个约束了，所以不用加了)
//                cplex.addLe(work_time[j][1],T_SLA);
            }
            //执行逻辑约束
            for (int jj:IntStream.range(0,deal_order.length).toArray()){
                cplex.addLe(work_time[deal_order[jj][0]][1],work_time[deal_order[jj][1]][0]);
            }
            //约束3：服务独占约束
            for (int i:IntStream.range(0,dim).toArray()){
                for (int j:IntStream.range(0,dim).toArray()){
                    if(kind_matrix[i][j]==0){
                        continue;
                    }
                    for (int l:IntStream.range(0,j).toArray()){
                        if (kind_matrix[j][l]==0){
                            continue;
                        }


                        IloNumExpr constrain3_1=cplex.sum(work_time[j][0],cplex.prod(-1,work_time[l][1]));
                        constrain3_1=cplex.sum(constrain3_1,cplex.prod(cplex.sum(1,cplex.prod(-1,X[i][j])),M));
                        constrain3_1=cplex.sum(constrain3_1,cplex.prod(cplex.sum(1,cplex.prod(-1,X[i][l])),M));
                        constrain3_1=cplex.sum(constrain3_1,cplex.prod(Z[z_map.get(new TwoTuple<>(i,new TwoTuple<>(j,l)))],M));
                        cplex.addLe(0,constrain3_1);

                        IloNumExpr constrain3_2=cplex.sum(work_time[l][0],cplex.prod(-1,work_time[j][1]));
                        constrain3_2=cplex.sum(constrain3_2,cplex.prod(cplex.sum(1,cplex.prod(-1,X[i][j])),M));
                        constrain3_2=cplex.sum(constrain3_2,cplex.prod(cplex.sum(1,cplex.prod(-1,X[i][l])),M));
                        constrain3_2=cplex.sum(constrain3_2,cplex.prod(cplex.sum(1,cplex.prod(-1,Z[z_map.get(new TwoTuple<>(i,new TwoTuple<>(j,l)))])),M));
                        cplex.addLe(0,constrain3_2);


                    }
                }
            }

            //long start_time2=System.currentTimeMillis();
            if (cplex.solve()){
//                if (cplex.getStatus().equals(IloCplex.Status.Infeasible)) {
//                    System.out.println("No Solution");
//                    return solution;
//                }

                long stop_time=System.currentTimeMillis();

                //System.out.println("cplex solver time{s}: "+(stop_time-start_time2)/1000.0);
                //System.out.println("cplex time{s}: "+(stop_time-start_time)/1000.0);
                x=new double[dim][dim];
                w_t=new double[dim][2];
                for(int i:IntStream.range(0,dim).toArray()){
                    x[i]=cplex.getValues(X[i]);
                    w_t[i][0]=cplex.getValues(work_time[i])[0];
                    w_t[i][1]=cplex.getValues(work_time[i])[1];
                }
//                object_state=cplex.getStatus().toString();
                if (cplex.getStatus().equals(IloCplex.Status.Optimal)){
                    solution=cplex.getObjValue();
                }else{
                    //这里采用一个效trick，通过返回值判断是否为最优解。
                    solution=cplex.getObjValue()+5000;
                }



                if (out_flage){
                    List<String> service_name=jobs.getAllJobKindsList();
                    Map<String,List<Integer>> solution_service_map=new HashMap<>();
                    for (int i=0;i<dim;i++){
                        if (Arrays.stream(x[i]).sum()>=1){
                            if (!solution_service_map.keySet().contains(service_name.get(i))){
                                List<Integer> service_id=new ArrayList<>();
                                service_id.add(i);
                                solution_service_map.put(service_name.get(i),service_id);
                            }else{
                                solution_service_map.get(service_name.get(i)).add(i);
                            }

                        }
                    }


                    File file_out=new File(out_file_path);
                    //System.out.println(file_path+"out.txt");
                    if (!file_out.exists()){
                        file_out.createNewFile();
                    }
                    FileWriter writer=new FileWriter(file_out);
                    writer.write(jobs.getFilePath().substring(1)+"\n");
                    writer.write("cplexcon time{s}: "+(stop_time-start_time)/1000.0+"\n");
                    writer.write("best service num: " + cplex.getObjValue()+"\n");
                    for (String service_name_temp:solution_service_map.keySet()){
                        writer.write(service_name_temp+":"+solution_service_map.get(service_name_temp).toString()+"\t");
                    }
                    writer.write("\n");
                    writer.write("T_SLA: "+T_SLA+"\n");

                    writer.write("Solution status: " + cplex.getStatus()+"\n");
                    System.out.println("Solution status: " + cplex.getStatus());

                    writer.write("work_time的值为："+"\n");
                    for (int i:IntStream.range(0,dim).toArray()){
                        writer.write(i+":"+Arrays.toString(w_t[i])+"\n");
                    }
                    writer.write("X的值为："+"\n");
                    for (int i:IntStream.range(0,dim).toArray()){
                        writer.write(Arrays.toString(x[i])+"\n");
                    }
                    writer.flush();
                    writer.close();
                }

            }else{
//                object_state=cplex.getStatus().toString();
                solution=M;
            }

            cplex.end();

        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("Cplex Solver stop");
        return solution;
    }

    public int[] getPop(){
        int[] pop=new int[dim];
        List<String> service_name=jobs.getAllJobKindsList();

        for (int i=0;i<dim;i++){
            if (Arrays.stream(x[i]).sum()>=1){
                pop[i]=1;
            }else{
                pop[i]=0;
            }
        }
        return pop;
    }

//    public String getObject_state() {
//        return object_state;
//    }
}
