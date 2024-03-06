package Paper1;

import utils.TwoTuple;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @projectName: CodeForPaper
 * @package: Paper1
 * @className: Exp2DatasetKind
 * @author: chl
 * @description: TODO
 * @date: 2023/10/4 20:02
 * @version: 1.0
 */
public class Exp6TimeLoose {
    private static String in_file_path;
    private static String out_file_path;
    private static Map<String,Double> T_SLA_map=new HashMap<String, Double>();

    private static double T_SLA_perc;
    private static String resource_file_path;
    private static String time_statistic_file_path;

    private static void getTSLAFromFile_Fix(String work_flow_kind, Integer job_num) {
        File root_file = new File(in_file_path);
        double key_path_time=Double.NEGATIVE_INFINITY;
        for (File dax_file : root_file.listFiles()) {

            String file_name = dax_file.getName();
            if (file_name.split("\\.")[4].equals("dax") && file_name.split("\\.")[2].equals(job_num.toString())) {
                WorkflowInfo jobs=new WorkflowInfo(dax_file.getAbsolutePath());
                double file_max_time=jobs.getKeyPathTime();
                key_path_time=file_max_time>key_path_time?file_max_time:key_path_time;
            }

        }
        T_SLA_map.put(work_flow_kind,key_path_time*T_SLA_perc);
    }

    /**
     * @param args: 三个输入参数，分别是：求解器类型、读取工作流大小、求解工作流大小
     * @return void
     * @author chl
     * @description TODO
     * @date 2023/10/9 9:02
     */
    public static void main(String[] args){
        //mode, data file, repeat num,host id,
        //mode, data file, repeat num,host id,start loose para, aim succeed rate
        String mode=args[0];
        String time_data_file=args[1];
        try{
            String host_id;
            if (args[3].equals("1")){
                host_id="";
            }else{
                host_id=args[3];
            }
            String workflow_kind="SIPHT";
            String template_dax="/SIPHT.n.100.0.dax";
            String time_statistic_file="";
            if (time_data_file.equals("100")){
                time_statistic_file="time_data.SIPHT.100.11.23-16.57.08.txt";
            }else if (time_data_file.equals("0")){
                time_statistic_file="time_data.SIPHT.0.11.23-21.15.43.txt";
            }else{
                System.exit(-1);
            }
            resource_file_path="/resource_of_atomic_services.11.08-22.33.21.txt";
            if (System.getProperty("os.name").equals("Windows 10")){
                in_file_path="F:\\桌面\\博士课题研究\\data\\Workflow\\"+workflow_kind;
                time_statistic_file_path="F:\\桌面\\博士课题研究\\data\\Workflow\\TimeStatistic\\";
                out_file_path="F:\\桌面\\博士课题研究\\基于工作流的服务网络拓扑优化\\Expfordiffdataset\\Exp6\\";
            }else{
                in_file_path="/ai/hpc4chl"+host_id+"/dataset/Workflow/"+workflow_kind;
                time_statistic_file_path="/ai/hpc4chl"+host_id+"/dataset/Workflow/TimeStatistic/";
                out_file_path="/ai/hpc4chl"+host_id+"/paper1/Exp6/";
            }


//            List<Double> loose_para=new ArrayList<Double>(){{
//                add(0.1);add(0.2);add(0.3);add(0.4);add(0.5);add(0.6);add(0.7);add(0.8);add(0.9);
//                add(0.95);add(0.975);add(0.98);add(0.99);add(0.995);
//            }};

            Calendar calendar=Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("MM.dd-HH.mm.ss");

            //job 处理的参数
            Set<String> rest_job=new HashSet<>();
            int rest_job_num=0;
            double T_SLA;
            int jobs_seed=0;                   //大于0 限制随机数种子，等于0不限制

            //Heuristic参数
            int heur_repeat_time=100;
            int heur_cplex_time_limit=100;
            long heur_seed=0;                   //大于0 限制随机数种子，等于0不限制

            //共有参数
            boolean out_flage=false;
            T_SLA_perc=1.1;
            double cpu_weight=1.0/3;
            double mem_weight=1.0/3;
            double disk_weight=1.0/3;

            //本实验参数
            double loose_rate=0.5;
            double aim_success_rate=0.9;
            double control_limit=0.01;
            int repeat_time=Integer.parseInt(args[2]);
            String code_version="v4.4.0";

            String exp_flage;
            if (mode.equals("control")){
                exp_flage=code_version+"-"+mode+"-"+time_data_file+"-"+repeat_time+"-"+args[4]+"-"+args[5]+"-"+formatter.format(calendar.getTime());
            }else{
                exp_flage=code_version+"-"+mode+"-"+time_data_file+"-"+repeat_time+"-"+formatter.format(calendar.getTime());
            }
            int work_flow_size=100;

            getTSLAFromFile_Fix(workflow_kind,work_flow_size);
            T_SLA=T_SLA_map.get(workflow_kind);


            File file_out=new File(out_file_path+"Exp6."+exp_flage+".txt");
            if (!file_out.exists()){
                file_out.createNewFile();
            }
            FileWriter writer=new FileWriter(file_out,true);

            ServiceInfo service_info=new ServiceInfo(resource_file_path,time_statistic_file_path+time_statistic_file,0);
            WorkflowInfo jobs=new WorkflowInfo(template_dax,rest_job,rest_job_num,T_SLA,jobs_seed);

            if (mode.equals("control")){
                loose_rate=Double.parseDouble(args[4]);
                aim_success_rate=Double.parseDouble(args[5]);
                double real_success_rate;
                double worse_success_rate=0;
                double worse_loose_rate=0;
                double better_success_rate=1.0;
                double better_loose_rate=1.0;
                int iteration=1;
                long time1=System.currentTimeMillis();
                while(true){
                    System.out.print("iteration num: "+iteration);
                    System.out.print("\taim success ratio:"+aim_success_rate);
                    System.out.println("\tloose_rate:"+loose_rate);
                    writer.write("loose rate:"+loose_rate+"::");

                    jobs.resetDealTime_Stable(service_info,loose_rate);
                    Parameters4Solver heur_plus_paras=new Parameters4Solver(jobs,service_info,cpu_weight,mem_weight,disk_weight,heur_repeat_time,false,
                            heur_cplex_time_limit,heur_seed,out_flage,out_file_path+"Exp6.heur_plus"+exp_flage+".txt");
                    SolverHeuristic heur_plus_solver=new SolverHeuristic(heur_plus_paras);
                    //进行求解，并判断是否存在解
                    if(heur_plus_solver.run()==Double.POSITIVE_INFINITY){
                        System.out.println("当前工作流处理时间无法求得可行解！");
                        break;
                    }
                    int[] pop=heur_plus_solver.getPop();
                    int success_times=0;
                    for (int i=0;i<repeat_time;i++){
                        jobs.resetDealTime_Random(service_info);
                        JudgeFeasibility heur_judge=new JudgeFeasibility(jobs,pop,heur_cplex_time_limit);
                        boolean flage=heur_judge.cplexJudgeRun();
                        if (flage){
                            success_times+=1;
                        }
                    }
                    real_success_rate=(double)success_times/repeat_time;
                    System.out.println("real success ratio："+real_success_rate);
                    writer.write("real success ratio:"+real_success_rate+"\n");
                    writer.flush();
                    //判断是否结束
                    if(worse_success_rate<aim_success_rate && real_success_rate>=aim_success_rate
                            && Math.abs(loose_rate-worse_loose_rate)<=0.0100001){
                        break;
                    }else if(better_success_rate>=aim_success_rate && real_success_rate<aim_success_rate &&
                            Math.abs(loose_rate-better_loose_rate)<=0.0100001){
                        writer.write("loose rate:"+better_loose_rate+"::");
                        writer.write("real success ratio:"+better_success_rate+"\n");
                        break;
                    }
                    //记录历史值
                    if(real_success_rate<aim_success_rate && loose_rate>worse_loose_rate){
                        worse_success_rate=real_success_rate;
                        worse_loose_rate=loose_rate;
                    }
                    if (real_success_rate>aim_success_rate && loose_rate<better_loose_rate){
                        better_success_rate=real_success_rate;
                        better_loose_rate=loose_rate;
                    }

                    //下面就是控制器，需要好好思考
                    double step=(aim_success_rate-real_success_rate)*0.1;
                    if (Math.abs(step)<0.01&& step!=0){
                        step=step/Math.abs(step)*0.01;
                    }
                    loose_rate+=step;
                    if(loose_rate<=worse_loose_rate){
                        loose_rate+=0.01;
                    }else if(loose_rate>=better_loose_rate){
                        loose_rate-=0.01;
                    }
                    if(loose_rate>1 || loose_rate<0){
                        System.out.println("loose rate value wrong!");
                        System.exit(-1);
                    }
                    //////////////////////////////////////////////////////////////////
                    iteration++;
                }
                long time2=System.currentTimeMillis();
                writer.write("all time:"+(time2-time1)/1000);
            }else if (mode.equals("enum")){

                double real_success_ratio=0;
                for (double rate=0.01;rate<=1;rate+=0.01){
                    loose_rate=rate;
                    writer.write("loose_rate::"+loose_rate+"::");
                    System.out.println("\tloose_rate:"+loose_rate);

                    jobs.resetDealTime_Stable(service_info,loose_rate);
                    Parameters4Solver heur_plus_paras=new Parameters4Solver(jobs,service_info,cpu_weight,mem_weight,disk_weight,heur_repeat_time,false,
                            heur_cplex_time_limit,heur_seed,out_flage,out_file_path+"Exp6.heur_plus"+exp_flage+".txt");
                    SolverHeuristic heur_plus_solver=new SolverHeuristic(heur_plus_paras);
                    //进行求解，并判断是否存在解
                    if(heur_plus_solver.run()==Double.POSITIVE_INFINITY){
                        System.out.println("当前工作流处理时间无法求得可行解！");
                        break;
                    }
                    int[] pop=heur_plus_solver.getPop();
                    int success_times=0;
                    for (int i=0;i<repeat_time;i++){
                        jobs.resetDealTime_Random(service_info);
                        JudgeFeasibility heur_judge=new JudgeFeasibility(jobs,pop,heur_cplex_time_limit);
                        boolean flage=heur_judge.cplexJudgeRun();
                        if (flage){
                            success_times+=1;
                        }
                    }
                    real_success_ratio=(double)success_times/repeat_time;
                    System.out.println("real success rate："+real_success_ratio);
                    writer.write(real_success_ratio+"\n");
                    writer.flush();
                }

            }

            writer.close();
        }catch (Exception e){
            e.printStackTrace();
        }





    }



}
