package Paper1;

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
public class Exp7Heurcheck {
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
/**
 * @param args: 输入参数4个，分别是：求解器类型，读取工作流规模，求解工作流最小规模，求解工作流最大规模
 * @return void
 * @author chl
 * @description TODO
 * @date 2023/10/9 9:10
 */
public static void main(String[] args) {
    List<String> h_kindlist = Arrays.asList( "time", "time_resource");
    for (String hk : h_kindlist) {
        try {
            //数据集
            String workflow_kind = args[0];
            int workflow_size = Integer.parseInt(args[1]);
            String resource_file_path = "/resource_of_atomic_services.mean-20.std-10.txt";
            if (System.getProperty("os.name").equals("Windows 10")) {
                in_file_path = "F:\\桌面\\博士课题研究\\data\\Workflow\\" + workflow_kind;
                out_file_path = "F:\\桌面\\博士课题研究\\基于工作流的服务网络拓扑优化\\Expfordiffdataset\\Exp7\\";
            } else {
                in_file_path = "/ai/hpc4chl/dataset/Workflow/" + workflow_kind;
                out_file_path = "/ai/hpc4chl/paper1/Exp7/";
            }

            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("MM.dd-HH.mm.ss");

            //job 处理的参数
            Set<String> rest_job = new HashSet<>();
            int rest_job_num = 0;
            Double T_SLA;
            int jobs_seed = 0;

            //Heuristic参数
            long heur_seed = 0;
            int heur_repeat_time = 1;
            int heur_cplex_time_limit = 0;
            String heur_out_path;
            String ha_kind = hk;

            //共有参数
            boolean out_flage = false;
            T_SLA_perc = 1.1;
            double cpu_weight = 1.0 / 3;
            double mem_weight = 1.0 / 3;
            double disk_weight = 1.0 / 3;

            //本实验参数
            String code_version = "v4.0.1";
            int repeat_time = Integer.parseInt(args[2]);
            String exp_flage = code_version + "-" +args[0] + "-" +ha_kind+ "-" + args[1] + "-" + args[2] + "-" + formatter.format(calendar.getTime());


            File file_out = new File(out_file_path + "Exp7.heur-" + exp_flage + ".txt");
            System.out.println("Exp7." + code_version + "\t" + args[0] + "\t" + args[1] + "\t" + args[2]);


            if (!file_out.exists()) {
                file_out.createNewFile();
            }
            FileWriter writer = new FileWriter(file_out, true);
            File root_file = new File(in_file_path);



            int i = workflow_size;
            getTSLAFromFile_Fix(workflow_kind, i);
            T_SLA = T_SLA_map.get(workflow_kind);

            for (File dax_file : root_file.listFiles()) {
                String file_name = dax_file.getName();
                if (file_name.split("\\.")[4].equals("dax") && file_name.split("\\.")[2].equals(Integer.toString(i))) {
                    if (Integer.parseInt(file_name.split("\\.")[3]) >= 30) {
                        continue;
                    }
                    writer.write(file_name + "::" + i + "::");
                    double optimal_value = 0;

                    WorkflowInfo jobs = new WorkflowInfo(dax_file.getAbsolutePath(), rest_job, rest_job_num, T_SLA, jobs_seed);
                    for (int j = 0; j < repeat_time; j++) {
                        System.out.println("Exp7::job num:" + i + "\tfile name:" + file_name + "\t repeat time:" + j + "   ...");
                        heur_out_path = out_file_path + "exp7-opt_traj.heur-" + file_name + "-" + exp_flage + "-" + j + ".txt";

                        ServiceInfo service_info = new ServiceInfo(0);
                        Parameters4Solver heur_paras = new Parameters4Solver(jobs, service_info, cpu_weight, mem_weight, disk_weight,
                                heur_repeat_time, false, heur_cplex_time_limit, heur_seed, out_flage, heur_out_path, ha_kind);

                        long time1 = System.currentTimeMillis();
                        SolverHeuristic heuristicsolver = new SolverHeuristic(heur_paras);
                        optimal_value = heuristicsolver.run();

                        long time2 = System.currentTimeMillis();
                        writer.write("(" + (time2 - time1) / 1000.0 + ":" + optimal_value + "),");
                        writer.flush();
                    }
                    System.gc();
                    writer.write("\n");
                }
            }



        writer.close();

    }catch(Exception e){
        e.printStackTrace();
    }
}


    }

}
