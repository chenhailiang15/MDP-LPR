package Paper1;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @projectName: CodeForPaper
 * @package: Paper1
 * @className: Exp2DatasetKind
 * @author: chl
 * @description: TODO
 * @date: 2023/10/4 20:02
 * @version: 1.0
 */
public class Exp3DatasetKind {
    private static String in_file_path;
    private static String out_file_path;
    private static Map<String,Double> T_SLA_map=new HashMap<String, Double>();

    private static double T_SLA_perc;
    private static String resource_file_path;

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
        try{

            String workflow_kind=args[0];
            String cplex_flage=args[1];
            String host_id;
            if (args.length==3){
                host_id=args[2];
            }else{
                host_id="";
            }


            resource_file_path="/resource_of_atomic_services.11.08-22.33.21.txt";
            if (System.getProperty("os.name").equals("Windows 10")){
                in_file_path="F:\\桌面\\博士课题研究\\data\\Workflow\\"+workflow_kind;
                out_file_path="F:\\桌面\\博士课题研究\\基于工作流的服务网络拓扑优化\\Expfordiffdataset\\Exp3\\";
            }else{
                in_file_path="/ai/hpc4chl"+host_id+"/dataset/Workflow/"+workflow_kind;
                out_file_path="/ai/hpc4chl"+host_id+"/paper1/Exp3/";
            }


            Calendar calendar=Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("MM.dd-HH.mm.ss");
            DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");


            //job 处理的参数
            Set<String> rest_job=new HashSet<>();
            int rest_job_num;
            double T_SLA;
            int jobs_seed=0;                   //大于0 限制随机数种子，等于0不限制

            //cplex参数
            int cplex_limit_time=3600*24;               //大于0 对cplex求解时间进行限制，等于0不限制

            //GA参数
            int pop_num=100;
            int iteration_num=3000;
            float cross_prob=0.1f;
            float mutation_prob=0.1f;
            long GA_seed=0;                     //大于0 限制随机数种子，等于0不限制

            //Heuristic参数
            int heur_repeat_time=100;
            int heur_cplex_time_limit=100;
            long heur_seed=0;                   //大于0 限制随机数种子，等于0不限制


            //共有参数
            boolean out_flage=true;
            T_SLA_perc=1.1;
            double cpu_weight=1.0/3;
            double mem_weight=1.0/3;
            double disk_weight=1.0/3;

            //本实验参数
            String code_version="v4.3.0";
            int repeat_time=1;
            String exp_flage=code_version+"-"+workflow_kind+"-"+cplex_flage+"-"+formatter.format(calendar.getTime());
            String import_work_flow_size_string="100";
            int work_flow_size=100;

            ServiceInfo service_info=new ServiceInfo(resource_file_path);

            File file_out=new File(out_file_path+"Exp3.main-"+exp_flage+".txt");
            getTSLAFromFile_Fix(workflow_kind,work_flow_size);


            if (!file_out.exists()){
                file_out.createNewFile();
            }
            FileWriter writer=new FileWriter(file_out,true);


            //F:\桌面\博士课题研究\基于工作流的服务网络拓扑优化\Expfordiffdataset\
            File root_file=new File(in_file_path);
            T_SLA=T_SLA_map.get(workflow_kind);
            for (int wf_size=10;wf_size<=work_flow_size;wf_size=wf_size+5){
                rest_job_num=wf_size;

                for(File dax_file:root_file.listFiles()){
                    String file_name=dax_file.getName();
                    if (file_name.split("\\.")[4].equals("dax") && file_name.split("\\.")[2].equals(import_work_flow_size_string)){
                        if (wf_size==work_flow_size){
                            rest_job_num=0;
                        }
                        WorkflowInfo jobs=new WorkflowInfo(dax_file.getAbsolutePath(),rest_job,rest_job_num,T_SLA,jobs_seed);

                        double optimal_value_cplex=0;
                        double optimal_value_ga=0;
                        double optimal_value_heur=0;
                        double optimal_value_heur_plus=0;
                        writer.write(file_name+"::"+wf_size+"::");

                        for (int i=0;i<repeat_time;i++){
                            LocalDateTime dateTime = LocalDateTime.now();
                            System.out.println("Exp3:: "+cplex_flage+"\tworkflow_size:"+wf_size+"\tfile name:"+file_name+"\trepeat time:"+i+"\t now time:"+dateTime.format(formatter2));
                            //初始化参数
                            Parameters4Solver cplex_paras=new Parameters4Solver(jobs,service_info,cpu_weight,mem_weight,disk_weight,
                                    cplex_limit_time,out_flage,out_file_path+"Exp3-cplex-"+exp_flage+".txt");

                            Parameters4Solver ga_paras=new Parameters4Solver(jobs,service_info,cpu_weight,mem_weight,disk_weight,
                                    pop_num,iteration_num,cross_prob,mutation_prob,GA_seed,out_flage,out_file_path+"Exp3-ga-"+exp_flage+".txt");

                            Parameters4Solver heur_paras=new Parameters4Solver(jobs,service_info,cpu_weight,mem_weight,disk_weight,
                                    heur_repeat_time,false,heur_cplex_time_limit,heur_seed, out_flage,out_file_path+"Exp3-heur-"+exp_flage+".txt");

                            Parameters4Solver heur_plus_paras=new Parameters4Solver(jobs,service_info,cpu_weight,mem_weight,disk_weight,
                                    heur_repeat_time,true,heur_cplex_time_limit,heur_seed, out_flage,out_file_path+"Exp3-heur_plus-"+exp_flage+".txt");

                            long time1=System.currentTimeMillis();
                            if (cplex_flage.equals("cplex_up")){
                                SolverCplex solverCplex=new SolverCplex(cplex_paras);
                                optimal_value_cplex= solverCplex.run();
                            }else if (cplex_flage.equals("cplex_down")){
                                optimal_value_cplex=0.0;
                            }else{
                                System.out.println("parameters wrong!");
                                System.exit(-1);
                            }

                            long time2=System.currentTimeMillis();
                            SolverGAH solverGAH=new SolverGAH(ga_paras);
                            optimal_value_ga= solverGAH.run();

                            long time3=System.currentTimeMillis();
                            SolverHeuristic solverHeuristic=new SolverHeuristic(heur_paras);
                            optimal_value_heur=solverHeuristic.run();

                            long time4=System.currentTimeMillis();
                            SolverHeuristic solverHeuristic_plus=new SolverHeuristic(heur_plus_paras);
                            optimal_value_heur_plus=solverHeuristic_plus.run();

                            long time5=System.currentTimeMillis();

                            writer.write("(["+(time2-time1)/1000.0+":"+optimal_value_cplex+"]-["+(time3-time2)/1000.0+":"+optimal_value_ga+"]-["+(time4-time3)/1000.0+":"+optimal_value_heur+"]-["+(time5-time4)/1000.0+":"+optimal_value_heur_plus+"]),");
                            writer.flush();
                            System.gc();

                        }
                        writer.write("\n");

                    }

                }

            }
            writer.close();

        }catch (Exception e){
            e.printStackTrace();
        }





    }



}
