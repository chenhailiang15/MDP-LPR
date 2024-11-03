import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class Exp5AlgorithmParem {
    private static String in_file_path;
    private static String out_file_path;
    private static Map<String,Double> T_SLA_map=new HashMap<String, Double>();
    private static double T_SLA_perc;

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

    public static void main(String[] args){
        try{

            if (System.getProperty("os.name").equals("Windows 10")){
                in_file_path="F:\\桌面\\博士课题研究\\data\\Workflow\\SIPHT";
                out_file_path="F:\\桌面\\博士课题研究\\基于工作流的服务网络拓扑优化\\Expfordiffdataset\\Exp4\\";
            }else{
                in_file_path="/ai/hpc4chl/dataset/Workflow/SIPHT";
                out_file_path="/ai/hpc4chl/paper1/Exp4/";
            }

            Calendar calendar=Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("MM.dd-HH.mm.ss");

            List<String> job_num_list=new ArrayList<String>(){{
                add("100");add("200");add("300");add("400");add("500");
                add("600");add("700");add("800");add("900");add("1000");
            }};
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
            int heur_repeat_time=0;
            int heur_cplex_time_limit=100;
            //共有参数
            boolean out_flage=false;
            List<Float> T_SLA_perc_list=new ArrayList<Float>(){{
               add(1.0f);add(1.1f);add(1.2f); add(1.3f);add(1.4f);
               add(1.5f);add(1.6f);add(1.7f);add(1.8f);add(1.9f);
               add(2.0f);
            }};

            //本实验参数
            String code_version="v0.2.0";
            String work_flow_name="SIPHT";
            String import_work_flow_size_string;
            int use_work_flow_size;

            int repeat_time=20;
            String exp_flage=formatter.format(calendar.getTime());


            File file_out=null;
            if(args.length==3){
                import_work_flow_size_string=args[1];
                use_work_flow_size=Integer.parseInt(args[2]);
                rest_job_num=use_work_flow_size;
                getTSLAFromFile_Fix(work_flow_name,Integer.parseInt(args[1]));
                System.out.println("Exp3."+code_version+"\t"+args[0]+"\t"+args[1]+"\t"+args[2]);
                exp_flage=code_version+"-"+args[1]+"."+args[2]+"-"+formatter.format(calendar.getTime());
                switch (args[0]){

                    case "cplex":
                        file_out=new File(out_file_path+"Exp4.cplex-"+exp_flage+".txt");
                        break;

                    case "ga_fi":
                        file_out=new File(out_file_path+"Exp4.ga_fi-"+exp_flage+".txt");
                        break;

                    case "ga_ufi":
                        file_out=new File(out_file_path+"Exp4.ga_ufi-"+exp_flage+".txt");
                        break;

                    case "heur":
                        file_out=new File(out_file_path+"Exp4.heur-"+exp_flage+".txt");
                        break;

                    default:
                        System.out.println("parameter is not one of ['cplex','ga_fi','ga_ufi','heur']");
                        return;
                }
            }else {
                System.out.println("the number of parameter is not equal to 3!");
                return;
            }

            if (!file_out.exists()){
                file_out.createNewFile();
            }
            FileWriter writer=new FileWriter(file_out,true);


            File root_file=new File(in_file_path);
            for (float perc:T_SLA_perc_list){
                T_SLA=Math.round(perc*T_SLA_map.get(work_flow_name));

                for (File dax_file:root_file.listFiles()){
                    String file_name=dax_file.getName();
                    if (file_name.split("\\.")[4].equals("dax") && file_name.split("\\.")[2].equals(import_work_flow_size_string)){
                        writer.write(file_name+"::"+perc+"::");

                        int optimal_value=0;

                        for(int j=0;j<repeat_time;j++){
                            System.out.println("Exp4::T_SLA:"+T_SLA+"\tfile name:"+file_name+"\t repeat time:"+j+"   ...");
                            WorkflowInfo jobs=new WorkflowInfo(dax_file.getAbsolutePath(),rest_job,rest_job_num,T_SLA,jobs_seed);
                            long time1=System.currentTimeMillis();
                            switch (args[0]) {
//                                case "cplex":
//                                    SolverCplexDis cplexsolver=new SolverCplexDis(jobs,cplex_resolution,time_limit,out_flage," ");
//                                    optimal_value=cplexsolver.run();
//                                    break;
//
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

                            }

                            long time2=System.currentTimeMillis();

                            writer.write("("+(time2-time1)/1000.0+":"+optimal_value+"),");
                            writer.flush();

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
