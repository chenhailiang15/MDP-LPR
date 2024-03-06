package Paper1;


import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import utils.TwoTuple;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.IntStream;

/**
 * @projectName: CodeForPaper
 * @package: Paper1
 * @className: GAOorModel
 * @author: chl
 * @description: TODO
 * @date: 2023/9/28 8:19
 * @version: 1.0
 */
public class SolverHeuristic {
    private WorkflowInfo jobs;
    private ServiceInfo serviceInfo;
    private double cpu_weight;
    private double mem_weight;
    private double disk_weight;
    private List<String> kind_list;
    private int[][] kind_matrix;
    private float[] deal_time;
    private int[][] deal_order;
    private Set<Integer> init_jobs;
    private Map<Integer,List<Integer>> job_child_map;
    private Map<Integer,List<Integer>> job_parent_map;
    private int dim;
    private int M=999999;

    //算法参数
    private Double T_SLA;

    private double[] fitness_value;
    private float[][] best_work_time_h;

    private Random random;
    private long seed;
    private boolean out_flage;
    private String out_file_path;
    private int repeat_time;
    boolean plus_model;

    private List<Map<String,Integer>> middle_results;
    private int cplex_limit_time;
    int[] best_pop;
    private Map<Integer,Double> resource_weight_value;
    String ha_name;

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

    /**
     * @param pop_f:
     * @return double数组，0：是适应度值，1：是自适应的变异值
     * @author chl
     * @description TODO
     * @date 2023/10/7 8:41
     */
    private double fitness(int[] pop_f){
        double value;
        List<TwoTuple<Integer,Float>> job_queue=new ArrayList<>();
        Set<Integer> job_done=new HashSet<>();

        for (Integer job_id:init_jobs){
            job_queue.add(new TwoTuple<>(job_id,0.0f));
        }
        float[][] work_time=new float[dim][2];
        float[] service_end_time=new float[dim];

        for (int i=0; i<dim; i++){
            service_end_time[i]=0;
        }

        while(job_queue.size() != 0){
            //先排序
            job_queue.sort(new Comparator<TwoTuple<Integer, Float>>() {
                @Override
                public int compare(TwoTuple<Integer, Float> o1, TwoTuple<Integer, Float> o2) {
                    return o1.second.compareTo(o2.second);
                }
            });
            int i_queue;
            for (i_queue=0;i_queue<job_queue.size();i_queue++){
                boolean flage_done_parent=true;
                for (Integer parent:job_parent_map.get(job_queue.get(i_queue).first)){
                    if (!job_done.contains(parent)){
                        flage_done_parent=false;
                        break;
                    }
                }
                if (flage_done_parent){
                    break;
                }
            }

            TwoTuple<Integer, Float> job_f=job_queue.remove(i_queue);
            job_done.add(job_f.first);

            //寻找可以最早执行本任务的服务
            int best_service_id=-1;
            float earliest_start_time=Float.POSITIVE_INFINITY;
            for (int i=0;i<dim;i++){
                //判断第i个服务是否可以执行当前任务
                if (pop_f[i] == 1 && kind_matrix[job_f.first][i] == 1 && service_end_time[i] <= earliest_start_time){
                    earliest_start_time=service_end_time[i];
                    best_service_id=i;
                }
            }
            //如果没有对应服务，增加巨大的惩罚值
            if (best_service_id==-1){
                value=Arrays.stream(pop_f).sum()+M;
                return value;
            }
            float this_job_end_time;
            float this_job_start_time;
            if (earliest_start_time< job_f.second){
                this_job_start_time=job_f.second;
                this_job_end_time=job_f.second+deal_time[job_f.first];
            }else{
                this_job_start_time=earliest_start_time;
                this_job_end_time=earliest_start_time+deal_time[job_f.first];
            }

            service_end_time[best_service_id]=this_job_end_time;
            work_time[job_f.first][0]=this_job_start_time;
            work_time[job_f.first][1]=this_job_end_time;

            //循环每个子节点，在队列里的修改最早开始时间，不在队列里的加入队列。
            for (Integer child:job_child_map.get(job_f.first)){
                boolean flage=false;
                for (int i =0; i<job_queue.size();i++){
                    if (job_queue.get(i).first == child){
                        flage = true;
                        if (job_queue.get(i).second<this_job_end_time){
                            job_queue.set(i,new TwoTuple<>(child,this_job_end_time));
                        }
                        break;

                    }

                }
                if (flage == false){
                    job_queue.add(new TwoTuple<>(child,this_job_end_time));
                }
            }
        }

        float end_jobs_time=0;
        for (int i=0;i<dim;i++){
            if (work_time[i][1]>end_jobs_time){
                end_jobs_time=work_time[i][1];
            }
        }

        best_work_time_h=work_time;
        return end_jobs_time;

    }


    private int[] getInitPopByHeuristic(){
        List<Integer> left_service_index=new ArrayList<>();
        List<Double> prob_for_jobs=new ArrayList<>();
        int[] init_pop_f=new int[dim];
        float max_time=Float.NEGATIVE_INFINITY;
        float min_time=Float.POSITIVE_INFINITY;
        for(int i=0;i<dim;i++){
            max_time=deal_time[i]>max_time?deal_time[i] : max_time;
            min_time=deal_time[i]<min_time?deal_time[i] : min_time;
        }
        float time_top=max_time+min_time;
        double prob_sum=0;
        double prob=0;


        for(int i=0;i<dim;i++){
            left_service_index.add(i);
            if (ha_name.equals("time_resource")){
                prob=(time_top-deal_time[i])*resource_weight_value.get(i);
            }else if(ha_name.equals("resource")){
                prob=resource_weight_value.get(i);
            }else if(ha_name.equals("time")){
                prob=(time_top-deal_time[i]);
            }else if(ha_name.equals("timev_resource")){
                prob=deal_time[i]*resource_weight_value.get(i);
            }else if(ha_name.equals("const")){
                prob=1;
            }else{
                System.out.println("ha_name wrong!");
                System.exit(-1);
            }

            prob_for_jobs.add(prob);
            init_pop_f[i]=1;
        }

        double random_temp;
        while(left_service_index.size()>0){
            //System.out.println("left:"+left_service_index.size());
            prob_sum= prob_for_jobs.stream().mapToDouble(Double::doubleValue).sum();
            random_temp=random.nextDouble()*prob_sum;
            double prob_sum_temp=0;
            //这里的i是元素。同一个意思
            for(int i:left_service_index){
                prob_sum_temp+=prob_for_jobs.get(i);//这里的i是index
                if (prob_sum_temp>=random_temp){
                    prob_for_jobs.set(i,0.0);
                    left_service_index.remove(new Integer(i));
                    init_pop_f[i]=0;
                    //fitness返回最早完成时间
                    if (fitness(init_pop_f)>T_SLA){
                        init_pop_f[i]=1;
                    }

                    break;
                }
            }

        }

        return init_pop_f;
    }

    private void initRandom(long seed_f){
        if (seed_f>0){
            random=new Random(seed_f);
        }else{
            random=new Random();
        }

    }

    private void initJobInfo(WorkflowInfo jobs_f){
        jobs=jobs_f;
        kind_list=jobs.getAllJobKindsList();
        dim=jobs.getAllJobIdSet().size();
        initKindMatrix();
        deal_time=jobs.getAllJobTime();
        deal_order=jobs.getAllDealOrder();
        init_jobs=jobs.getInitJobsinOrder();
        job_child_map=jobs.getMapWithChildsList();
        job_parent_map=jobs.getMapWithParentList();
    }

    private boolean isListContainsMap(List<Map<String,Integer>> results_list, Map<String,Integer> service_config){
        for (Map<String, Integer> ser_con : results_list){
            if (ser_con.size()!=service_config.size()){
                continue;
            }
            for(String service_name : ser_con.keySet()){
                if (!service_config.containsKey(service_name) || ser_con.get(service_name)!=service_config.get(service_name)){
                    continue;
                }
            }
            return true;
        }
        return false;
    }

    private int[] config2pop(Map<String, Integer> one_config){
        int[] pop_f=new int[dim];
        Map<String, Integer> one_config_copy=new HashMap<>();
        one_config_copy.putAll(one_config);
        for(int i=0;i<dim;i++){
            int want_more_service=one_config_copy.get(jobs.getJobKind(i));
            if (want_more_service>0){
                one_config_copy.put(jobs.getJobKind(i),want_more_service-1);
                pop_f[i]=1;
            }else{
                pop_f[i]=0;
            }
        }
        return pop_f;
    }

    private Map<String, Integer> pop2config(int[] pop_f){
        Map<String,Integer> service_config_f=new HashMap<>();
        for(int j=0;j<dim;j++){
            if (pop_f[j]==1){
                String kind_t=jobs.getJobKind(j);
                if (!service_config_f.keySet().contains(kind_t)){
                    service_config_f.put(kind_t,1);
                }else{
                    service_config_f.put(kind_t,service_config_f.get(kind_t)+1);
                }
            }
        }
        return service_config_f;
    }

    private List<Map<String, Integer>> optimize_config(Map<String, Integer> one_config, Set<String> stable_kind_f){
        List<Map<String, Integer>> better_configs=new ArrayList<>();
        for (String service_name:one_config.keySet()){
            if (one_config.get(service_name)>=2 && !stable_kind_f.contains(service_name)){
                Map<String, Integer> new_config=new HashMap<>();
                new_config.putAll(one_config);
                new_config.put(service_name,new_config.get(service_name)-1);
                int[] new_pop=config2pop(new_config);
                JudgeFeasibility judge_feasibility=new JudgeFeasibility(jobs,new_pop,cplex_limit_time);
                boolean isfeasible= judge_feasibility.cplexJudgeRun();
                if (isfeasible){
                    better_configs.add(new_config);
                }else{
                    //删减失败则放入稳定类型集合
                    stable_kind_f.add(service_name);
                }
            }else{
                //只剩一个则放入稳定类型集合
                stable_kind_f.add(service_name);
            }
        }
        return better_configs;

    }

    private boolean judgeFeasible(){
        int[] init_pop_f=new int[dim];
        for(int i=0;i<dim;i++){
            init_pop_f[i]=1;
        }
        if (fitness(init_pop_f)>T_SLA){
            return false;
        }
        return true;
    }

    public double run(){
        double best_value=Double.POSITIVE_INFINITY;
        if (judgeFeasible()==false){
            System.out.println("初步判定无解！很大程度代表实际情况");
            return best_value;
        }

        int[] pop_t;
        best_pop=new int[dim];
        float[][] best_work_time=new float[dim][2];
        try{
            System.out.print("Starting Solver Heuristic ...");
            System.out.println("("+plus_model+")");
            long start_time=System.currentTimeMillis();

            for(int i=0;i<repeat_time;i++){
//                System.out.println(i);
                initRandom(seed);
                pop_t=getInitPopByHeuristic();
                fitness_value[i]= getObjectResourceConsump(pop_t);
                //如果目标函数值小于当前最优，则进行记录。
                if (fitness_value[i]<best_value){
                    best_value=fitness_value[i];
                    best_pop=pop_t.clone();
                    //再计算一遍，保存worktime
                    fitness(best_pop);
                    best_work_time=best_work_time_h;
                }
                if (plus_model){
                    //如果plus模式，对每次结果进行记录，然后在后面进行进一步优化。
                    Map<String,Integer> service_config=pop2config(pop_t);
                    if (!isListContainsMap(middle_results,service_config)){
                        middle_results.add(service_config);
                    }
                }

            }

            if(plus_model){
                //如果是plus模式，需要对所有前一阶段的解进行进一步优化。
                double best_value_plus=best_value;
                Set<String> stable_kind=new HashSet<>();
                Map<String, Integer> best_config_plus=new HashMap<>();
                while(middle_results.size()!=0){
                    List<Map<String, Integer>> middle_results_next=new ArrayList<>();

                    for (Map<String,Integer> one_config:middle_results){
                        List<Map<String, Integer>> better_configs=optimize_config(one_config,stable_kind);
                        for (Map<String, Integer> one_beter_config:better_configs){
                            //如果有更优解，则更新最优解
                            double obj_value=getObjectResourceConsump(config2pop(one_beter_config));
                            if (obj_value<best_value_plus){
                                best_config_plus.putAll(one_beter_config);
                                best_value_plus=obj_value;
                            }
                            //判断解是否在已有结果中
                            if (!isListContainsMap(middle_results_next,one_beter_config)){
                                middle_results_next.add(one_beter_config);
                            }

                        }

                    }
                    middle_results=middle_results_next;


                }
                //不等于0说明有更好的解产生
                if (best_config_plus.size()!=0){
                    best_value=best_value_plus;
                    best_pop=config2pop(best_config_plus);
                }
            }
            //输出本次迭代最优
            if (out_flage){
                System.out.println("fitness value: "+ best_value);
            }

            long stop_time=System.currentTimeMillis();
            System.out.print("Heuristic Solver stop!");
            System.out.println("("+plus_model+")");

            if (out_flage){
                List<String> service_name=jobs.getAllJobKindsList();
                Map<String,List<Integer>> solution_service_map=new HashMap<>();
                for (int i=0;i<dim;i++){
                    if (best_pop[i]==1){
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
                writer.write("repeat time:"+repeat_time+"\n");
                writer.write("Heuristic time{s}: "+(stop_time-start_time)/1000.0+"\n");
                writer.write("best result: "+ best_value+"\n");
                writer.write("all result: "+ Arrays.toString(fitness_value)+"\n");
                for (String service_name_temp:solution_service_map.keySet()){
                    writer.write(service_name_temp+":"+solution_service_map.get(service_name_temp).toString()+"\t");
                }
                writer.write("\n");
                writer.write("T_SLA: "+T_SLA+"\n");
                writer.write("work_time的值为："+"\n");

                if (!(best_work_time==null)){
                    for (int i=0;i<dim;i++){
                        writer.write(i+":"+Arrays.toString(best_work_time[i])+"\n");
                    }

                }

                writer.flush();
                writer.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return best_value;
    }


    public SolverHeuristic(Parameters4Solver all_parameters_f){

        serviceInfo=all_parameters_f.getServiceInfo();
        cpu_weight=all_parameters_f.getCpu_weight();
        mem_weight=all_parameters_f.getMem_weight();
        disk_weight=all_parameters_f.getDisk_weight();
        T_SLA=all_parameters_f.getJobs().getT_SLA();
        out_flage=all_parameters_f.getOut_flage();
        seed=all_parameters_f.getSeed();
        out_file_path= all_parameters_f.getOut_file_path();
        repeat_time=all_parameters_f.getRepeat_time();
        fitness_value=new double[repeat_time];
        plus_model=all_parameters_f.getPlus_model();

        middle_results=new ArrayList<>();
        cplex_limit_time=all_parameters_f.getCplex_limit_time();
        ha_name=all_parameters_f.getHa_name();
        initJobInfo(all_parameters_f.getJobs());
        initResourceMap();

    }
    private void initResourceMap(){
        resource_weight_value=new HashMap<>();
        for (int i=0;i<dim;i++){
            String service_kind=jobs.getJobKind(i);
            double value=cpu_weight*serviceInfo.getCpu_resource_map().get(service_kind)/ServiceInfo.getCpu_ave()+
                    mem_weight*serviceInfo.getMem_resource_map().get(service_kind)/ServiceInfo.getMem_ave()+
                    disk_weight*serviceInfo.getDisk_resource_map().get(service_kind)/ServiceInfo.getDisk_ave();
            resource_weight_value.put(i,value);
        }
    }
    private double getObjectResourceConsump(int[] pop_f){
        double pop_resource_consump=0.0;
        for (int i=0;i<dim;i++){
            if (pop_f[i]==1){
                pop_resource_consump+=resource_weight_value.get(i);
            }
        }
        return pop_resource_consump;
    }
    public int[] getPop(){
        return best_pop;
    }


}


class JudgeFeasibility {
    private WorkflowInfo jobs;
    private List<String> kind_list;
    private int[][] kind_matrix;
    private float[] deal_time;
    private int[][] deal_order;
    private int dim;
    private int M=9999999;
    private IloCplex cplex;
    private Double time_limit;
    private int[] pop;
    private Map<Integer, Integer> service_dim2short_map;
    private Map<Integer, Integer> service_short2dim_map;
    private Map<TwoTuple<Integer, TwoTuple<Integer,Integer>>,Integer> z_map;
    private float[][] optimized_work_time;
    private Set<Integer> init_jobs;
    private Map<Integer,List<Integer>> job_child_map;
    private Map<Integer,List<Integer>> job_parent_map;
    private double end_time_temp;

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
    }

    private void initMap(){
        service_dim2short_map=new HashMap<>();
        service_short2dim_map=new HashMap<>();
        z_map=new HashMap<>();

        int service_order=0;
        for(int i=0;i<dim;i++){
            if (pop[i]==1){
                service_dim2short_map.put(i,service_order);
                service_short2dim_map.put(service_order,i);
                service_order+=1;
            }
        }
        int z_order=0;
        for(int i=0;i<service_short2dim_map.size();i++){
            for(int j=0;j<dim;j++){
                if (kind_matrix[service_short2dim_map.get(i)][j]==0){
                    continue;
                }
                for(int l=0;l<j;l++){

                    if(kind_matrix[j][l]==0){
                        continue;
                    }

                    z_map.put(new TwoTuple<>(i,new TwoTuple<>(j,l)),z_order);
                    z_order+=1;

                }
            }
        }

    }

    private boolean isfeasible(){
        Set<String> kind_set=new HashSet<>();
        int kind_num=jobs.getAllJobKindsSet().size();
        for (int i=0;i<dim;i++){
            if (pop[i]==1){
                kind_set.add(jobs.getJobKind(i));
                if (kind_set.size()==kind_num){
                    return true;
                }
            }
        }
        return false;
    }

    public boolean cplexJudgeRun(){

        if (!isfeasible()){
            return false;
        }
        initMap();
        int service_num=service_short2dim_map.size();
        int z_num=z_map.size();
        try{

            cplex = new IloCplex();
            cplex.setOut(null);
            if (time_limit>0){
                cplex.setParam(IloCplex.DoubleParam.TimeLimit,time_limit);
            }

//            cplex.setParam(IloCplex.DoubleParam.EpGap,0.99);
//            cplex.setParam(IloCplex.DoubleParam.TimeLimit,time_limit);
//            cplex.setParam(IloCplex.IntParam.SolutionType,1);

            cplex.setParam(IloCplex.DoubleParam.WorkMem,1024*2);
            cplex.setParam(IloCplex.DoubleParam.TreLim,1024*32);
            cplex.setParam(IloCplex.IntParam.NodeFileInd,2);
//            cplex.setParam(IloCplex.IntParam.Threads,40);

            //定义决策变量
            IloNumVar[][] X=new IloNumVar[service_num][dim];          //0,1变量
            IloNumVar[][] work_time=new IloNumVar[dim][2];          //浮点数变量
            IloNumVar[] Z=new IloNumVar[z_num];            //0,1变量
            IloNumVar TotalTime=cplex.numVar(0,Float.POSITIVE_INFINITY);

            //变量初始化
            for (int i: IntStream.range(0,dim).toArray()){
                for (int j:IntStream.range(0,service_num).toArray()){
                    X[j][i]=cplex.numVar(0,1, IloNumVarType.Int);
                }
                work_time[i][0]=cplex.numVar(0,Float.POSITIVE_INFINITY,IloNumVarType.Float);
                work_time[i][1]=cplex.numVar(0,Float.POSITIVE_INFINITY,IloNumVarType.Float);
            }

            for (int i:IntStream.range(0,z_num).toArray()){
                Z[i]=cplex.numVar(0,1,IloNumVarType.Int);
            }

            //目标,设置常数

            IloNumVar const_value=cplex.numVar(0,Float.POSITIVE_INFINITY);
            cplex.addEq(const_value,1);
            cplex.addMinimize(const_value);

            //约束1：一个任务仅由一个服务执行
            for (int j:IntStream.range(0,dim).toArray()){
                IloNumExpr constrain1=cplex.numExpr();
                for (int i:IntStream.range(0,service_num).toArray()){
                    //判断是否属于同一类型
                    if (kind_matrix[j][service_short2dim_map.get(i)]==1){
                        constrain1=cplex.sum(constrain1,cplex.prod(X[i][j],kind_matrix[service_short2dim_map.get(i)][j]));
                    }else{
                        cplex.addEq(X[i][j],0);
                    }

                }
                cplex.addEq(constrain1,1);
            }
            //约束2：时间约束
            for (int j:IntStream.range(0,dim).toArray()){
                //工作执行时间约束
                cplex.addLe(cplex.sum(work_time[j][0],deal_time[j]),work_time[j][1]);
                //约束时间范围内完成所有工作。SLA约束
//                cplex.addLe(work_time[j][1],TotalTime);
                cplex.addLe(work_time[j][1],jobs.getT_SLA());
            }
            //执行逻辑约束
            for (int jj:IntStream.range(0,deal_order.length).toArray()){
                cplex.addLe(work_time[deal_order[jj][0]][1],work_time[deal_order[jj][1]][0]);
            }
            //约束3：服务独占约束
            for (int i:IntStream.range(0,service_num).toArray()){
                for (int j:IntStream.range(0,dim).toArray()){
                    if (kind_matrix[service_short2dim_map.get(i)][j]==0){
                        continue;
                    }
                    for (int l:IntStream.range(0,j).toArray()){
                        if (kind_matrix[j][l]==0){
                            continue;
                        }
                        //进入这里代表i, j, l都是同类

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
                if (cplex.getStatus().equals(IloCplex.Status.Infeasible)) {
                    System.out.println(cplex.getStatus().toString());
                    System.out.println("@@@@@@@超出意料No Solution");
                    System.exit(-1);
                }
//                System.out.println("解状态：#################################################"+cplex.getStatus());
//
                optimized_work_time=new float[dim][2];
                for(int i:IntStream.range(0,dim).toArray()){
                    optimized_work_time[i][0]=(float)cplex.getValues(work_time[i])[0];
                    optimized_work_time[i][1]=(float)cplex.getValues(work_time[i])[1];
                }
                //这里存疑，理论上说，本问题的可行解就是最优解，但是竟然输出了可行解。
                if (cplex.getStatus().equals(IloCplex.Status.Optimal)){

                    cplex.end();
                    return true;
                }else{
                    System.out.println(cplex.getStatus().toString());
                    System.out.println("cplex状态为可行解，但理论上应该是最优解。（超出意料）");
//                    System.exit(-1);
                }

            }
//            System.out.println("解状态：#################################################"+cplex.getStatus());
            cplex.end();
            return false;

        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }


    public boolean heurJudgeRun(){
        end_time_temp=Double.POSITIVE_INFINITY;
        if (!isfeasible()){
            return false;
        }
        double value;
        List<TwoTuple<Integer,Float>> job_queue=new ArrayList<>();
        Set<Integer> job_done=new HashSet<>();

        for (Integer job_id:init_jobs){
            job_queue.add(new TwoTuple<>(job_id,0.0f));
        }
        float[][] work_time=new float[dim][2];
        float[] service_end_time=new float[dim];

        for (int i=0; i<dim; i++){
            service_end_time[i]=0;
        }

        while(job_queue.size() != 0){
            //先排序
            job_queue.sort(new Comparator<TwoTuple<Integer, Float>>() {
                @Override
                public int compare(TwoTuple<Integer, Float> o1, TwoTuple<Integer, Float> o2) {
                    return o1.second.compareTo(o2.second);
                }
            });
            int i_queue;
            for (i_queue=0;i_queue<job_queue.size();i_queue++){
                boolean flage_done_parent=true;
                for (Integer parent:job_parent_map.get(job_queue.get(i_queue).first)){
                    if (!job_done.contains(parent)){
                        flage_done_parent=false;
                        break;
                    }
                }
                if (flage_done_parent){
                    break;
                }
            }

            TwoTuple<Integer, Float> job_f=job_queue.remove(i_queue);
            job_done.add(job_f.first);

            //寻找可以最早执行本任务的服务
            int best_service_id=-1;
            float earliest_start_time=Float.POSITIVE_INFINITY;
            for (int i=0;i<dim;i++){
                //判断第i个服务是否可以执行当前任务
                if (pop[i] == 1 && kind_matrix[job_f.first][i] == 1 && service_end_time[i] <= earliest_start_time){
                    earliest_start_time=service_end_time[i];
                    best_service_id=i;
                }
            }
            //如果没有对应服务，增加巨大的惩罚值
            if (best_service_id==-1){
                value=Arrays.stream(pop).sum()+M;
                return false;
            }
            float this_job_end_time;
            float this_job_start_time;
            if (earliest_start_time< job_f.second){
                this_job_start_time=job_f.second;
                this_job_end_time=job_f.second+deal_time[job_f.first];
            }else{
                this_job_start_time=earliest_start_time;
                this_job_end_time=earliest_start_time+deal_time[job_f.first];
            }

            service_end_time[best_service_id]=this_job_end_time;
            work_time[job_f.first][0]=this_job_start_time;
            work_time[job_f.first][1]=this_job_end_time;

            //循环每个子节点，在队列里的修改最早开始时间，不在队列里的加入队列。
            for (Integer child:job_child_map.get(job_f.first)){
                boolean flage=false;
                for (int i =0; i<job_queue.size();i++){
                    if (job_queue.get(i).first == child){
                        flage = true;
                        if (job_queue.get(i).second<this_job_end_time){
                            job_queue.set(i,new TwoTuple<>(child,this_job_end_time));
                        }
                        break;
                    }
                }
                if (flage == false){
                    job_queue.add(new TwoTuple<>(child,this_job_end_time));
                }
            }
        }

        float end_jobs_time=0;
        for (int i=0;i<dim;i++){
            if (work_time[i][1]>end_jobs_time){
                end_jobs_time=work_time[i][1];
            }
        }
        System.out.println("结束时间："+end_jobs_time+"约束时间："+jobs.getT_SLA());
        if (end_jobs_time>jobs.getT_SLA()){
            System.out.println("false");
            return false;
        }
        end_time_temp=end_jobs_time;
        System.out.println("true");
        return true;

    }

    public double getEnd_time_temp() {
        return end_time_temp;
    }

    public JudgeFeasibility(WorkflowInfo jobs_f, int[] pop_f, double time_limit_f){
        //读取任务流
        jobs=jobs_f;
        pop=pop_f;
        time_limit=time_limit_f;

        kind_list=jobs.getAllJobKindsList();
        dim=jobs.getAllJobIdSet().size();
        deal_time=jobs.getAllJobTime();
        deal_order=jobs.getAllDealOrder();

        initKindMatrix();
    }

    public JudgeFeasibility(WorkflowInfo jobs_f, int[] pop_f){
        //读取任务流
        jobs=jobs_f;
        pop=pop_f;
        kind_list=jobs.getAllJobKindsList();
        dim=jobs.getAllJobIdSet().size();
        deal_time=jobs.getAllJobTime();
        deal_order=jobs.getAllDealOrder();

        initKindMatrix();

        init_jobs=jobs.getInitJobsinOrder();
        job_child_map=jobs.getMapWithChildsList();
        job_parent_map=jobs.getMapWithParentList();

        initKindMatrix();
    }

    public float[][] getOptimized_work_time() {
        return optimized_work_time;
    }
}
