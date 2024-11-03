import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @projectName: CodeForPaper
 * @package: Paper1
 * @className: ExpForDiffDatasets
 * @author: chl
 * @description: TODO
 * @date: 2023/10/2 10:36
 * @version: 1.0
 */
public class ExpForCplex {
    private static String in_file_path;
    private static String out_file_path;
    private static Map<String,Long> T_SLA_map=new HashMap<String, Long>();
    private static double T_SLA_perc;

    private static void getTSLAFromFile_Fix(String work_flow_kind, Integer job_num) {
        File root_file = new File(in_file_path);
        double key_path_time=Double.NEGATIVE_INFINITY;
        for (File dax_file : root_file.listFiles()) {

            String file_name = dax_file.getName();
            if (file_name.split("\\.")[4].equals("dax") && file_name.split("\\.")[2].equals(job_num.toString())) {
                WorkflowInfo jobs=new WorkflowInfo(dax_file.getAbsolutePath(),new HashSet<>(),0,0,0);
                double file_max_time=jobs.getKeyPathTime();
                key_path_time=file_max_time>key_path_time?file_max_time:key_path_time;
            }

        }
        T_SLA_map.put(work_flow_kind,Math.round(key_path_time*T_SLA_perc));
    }
    /**
     * @param args: 输入参数4个，分别是：求解器类型，读取工作流规模，求解工作流最小规模，求解工作流最大规模
     * @return void
     * @author chl
     * @description TODO
     * @date 2023/10/9 9:10
     */
    public static void main(String[] args){
        try{
            //求解器类型 工作流类型 SLA百分比
            String workflow_kind=args[1];
            float T_SLA_perc_in=Float.parseFloat(args[2]);

            if (System.getProperty("os.name").equals("Windows 10")){
                in_file_path="F:\\桌面\\博士课题研究\\data\\Workflow\\"+workflow_kind;
                out_file_path="F:\\桌面\\博士课题研究\\基于工作流的服务网络拓扑优化\\Expfordiffdataset\\ExpCplex\\";
            }else{
                in_file_path="/ai/hpc4chl/dataset/Workflow/"+workflow_kind;
                out_file_path="/ai/hpc4chl/paper1/ExpCplex/";
            }

            Calendar calendar=Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("MM.dd-HH.mm.ss");

            //job 处理的参数
            Set<String> rest_job=new HashSet<>();
            int rest_job_num=0;
            long T_SLA;
            int jobs_seed=0;

            //cplex参数
            double cplex_resolution=0.1;
            double time_limit=100.0;
            //GA参数
            int pop_num=100;
            int iteration_num_fi=100;
            int iteration_num_ufi=0;
            float cross_prob=0.1f;
            float mutation_prob=0.1f;
            long GA_seed=0;

            //Heuristic参数
            long Heur_seed=0;
            int heur_repeat_time=10;
            int heur_cplex_time_limit=100;
            //共有参数
            boolean out_flage=false;
            T_SLA_perc=T_SLA_perc_in;

            //本实验参数
            String code_version="v1.0.0";
            int repeat_time=1;

            String exp_flage=formatter.format(calendar.getTime());
            //int import_work_flow_size;
            int use_work_flow_min_size;
            int use_work_flow_max_size;
            String import_work_flow_size_string;



            File file_out=null;
            if(args.length==3){
                import_work_flow_size_string=args[1];
                //import_work_flow_size=Integer.parseInt(args[1]);


                System.out.println("Exp1."+code_version+"\t"+args[0]+"\t"+args[1]+"\t"+args[2]);
                exp_flage=code_version+"-"+args[1]+"."+args[2]+"-"+formatter.format(calendar.getTime());
                switch (args[0]){
                    case "cplex":
                        file_out=new File(out_file_path+"/Exp.cplex-"+exp_flage+".txt");
                        break;

                    case "ga":
                        file_out=new File(out_file_path+"/Exp.ga_fi-"+exp_flage+".txt");
                        break;

                    case "ga_ufi":
                        file_out=new File(out_file_path+"/Exp.ga_ufi-"+exp_flage+".txt");
                        break;

                    case "heur":
                        file_out=new File(out_file_path+"/Exp.heur-"+exp_flage+".txt");
                        break;

                    case "all":
                        file_out=new File(out_file_path+"/Exp.cplex_gafi_gaufi_heur-"+exp_flage+".txt");
                        break;

                    default:
                        System.out.println("parameter is not one of ['cplex','ga_fi','ga_ufi','heur','all']");
                        return;
                }
            }else {
                System.out.println("the number of parameter is not equal to 4!");
                return;
            }

            if (!file_out.exists()){
                file_out.createNewFile();
            }
            FileWriter writer=new FileWriter(file_out,true);


            File root_file=new File(in_file_path);
            List<Integer> workflow_sizelist=Arrays.asList(50,100,200,300,400,500,600,700,800,900,1000);

            for (int i :workflow_sizelist){
                getTSLAFromFile_Fix(workflow_kind,i);
                T_SLA=T_SLA_map.get(workflow_kind);

                for (File dax_file:root_file.listFiles()){
                    String file_name=dax_file.getName();
                    if (file_name.split("\\.")[4].equals("dax") && file_name.split("\\.")[2].equals(Integer.toString(i))){
                        writer.write(file_name+"::"+T_SLA_perc_in+"::"+T_SLA+"::");

                        int optimal_value=0;
                        int optimal_value_cplex=0;
                        int optimal_value_ga_fi=0;
                        int optimal_value_ga_ufi=0;
                        int optimal_value_heur=0;

                        for(int j=0;j<repeat_time;j++){
                            System.out.println("Exp::job num:"+i+"\tfile name:"+file_name+"\t repeat time:"+j+"   ...");
                            WorkflowInfo jobs=new WorkflowInfo(dax_file.getAbsolutePath(),rest_job,rest_job_num,T_SLA,jobs_seed);
                            long time3=0;
                            long time4=0;
                            long time5=0;
                            long time1=System.currentTimeMillis();

                            switch (args[0]) {
//                                case "cplex":
//                                    SolverCplex cplexsolver=new SolverCplex(jobs,time_limit,out_flage," ");
//                                    optimal_value=cplexsolver.run();
//                                    break;
//                                case "ga_fi":
//                                    SolverGAH ga1solver=new SolverGAH(jobs,pop_num,iteration_num_fi,cross_prob,mutation_prob,out_flage,GA_seed," ");
//                                    optimal_value=ga1solver.run();
//                                    break;
//
//                                case "ga_ufi":
//                                    SolverGAH ga2solver=new SolverGAH(jobs,pop_num,iteration_num_ufi,cross_prob,mutation_prob,out_flage,GA_seed," ");
//                                    optimal_value=ga2solver.run();
//                                    break;
//
//                                case "heur":
//                                    SolverHeuristic heuristicsolver=new SolverHeuristic(jobs,heur_repeat_time,true,heur_cplex_time_limit,out_flage,Heur_seed," ");
//                                    optimal_value=heuristicsolver.run();
//                                    break;
//                                case "all":
//                                    SolverCplexDis cplexsolver2=new SolverCplexDis(jobs,cplex_resolution,time_limit,out_flage," ");
//                                    optimal_value_cplex=cplexsolver2.run();
//                                    time3=System.currentTimeMillis();
//
//                                    SolverGAH ga1solver2=new SolverGAH(jobs,pop_num,iteration_num_fi,cross_prob,mutation_prob,out_flage,GA_seed," ");
//                                    optimal_value_ga_fi=ga1solver2.run();
//                                    time4=System.currentTimeMillis();
//
//                                    SolverGAH ga2solver2=new SolverGAH(jobs,pop_num,iteration_num_ufi,cross_prob,mutation_prob,out_flage,GA_seed," ");
//                                    optimal_value_ga_ufi=ga2solver2.run();
//                                    time5=System.currentTimeMillis();
//
//                                    SolverHeuristic heuristicsolver2=new SolverHeuristic(jobs,heur_repeat_time,true,heur_cplex_time_limit,out_flage,Heur_seed," ");
//                                    optimal_value_heur=heuristicsolver2.run();
//
//                                    break;

                            }

                            long time2=System.currentTimeMillis();

                            if (args[0].equals("all")){
                                writer.write("(["+(time3-time1)/1000.0+":"+optimal_value_cplex+"]-["+(time4-time3)/1000.0+":"+optimal_value_ga_fi+"]-["+(time5-time4)/1000.0+":"+optimal_value_ga_ufi+"]-["+(time2-time5)/1000.0+":"+optimal_value_heur+"]),");
                            }else{
                                writer.write("("+(time2-time1)/1000.0+":"+optimal_value+"),");
                            }

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
