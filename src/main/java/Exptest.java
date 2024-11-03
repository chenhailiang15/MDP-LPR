import java.io.File;
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
public class Exptest {
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
                WorkflowInfo jobs=new WorkflowInfo(dax_file.getAbsolutePath(),new HashSet<>(),0,0,0);
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
public static void main(String[] args){
        try{
            String workflow_kind="CYBERSHAKE";
            int workflow_size=100;
            String resource_file_path="/resource_of_atomic_services.11.08-22.33.21.txt";
            in_file_path="F:\\桌面\\博士课题研究\\data\\Workflow\\"+workflow_kind;
            out_file_path="F:\\桌面\\博士课题研究\\基于工作流的服务网络拓扑优化\\Expfordiffdataset\\Exptest\\";


            Calendar calendar=Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("MM.dd-HH.mm.ss");

            //job 处理的参数
            Set<String> rest_job=new HashSet<>();
            int rest_job_num=0;
            double T_SLA;
            int jobs_seed=0;

            //cplex参数
            double cplex_resolution=0.1;
            double time_limit=0;
            //GA参数
            int pop_num=100;
            int iteration_num_fi=100;
            int iteration_num_ufi=0;
            float cross_prob=0.1f;
            float mutation_prob=0.1f;
            long GA_seed=0;

            //Heuristic参数
            long heur_seed=0;
            int heur_repeat_time=10;
            int heur_cplex_time_limit=100;
            //共有参数
            boolean out_flage=true;
            T_SLA_perc=1;
            double cpu_weight=1.0/3;
            double mem_weight=1.0/3;
            double disk_weight=1.0/3;

            //本实验参数

            String code_version="Tv0.2.0";
            int repeat_time=1;
            getTSLAFromFile_Fix(workflow_kind,workflow_size);
            String exp_flage=formatter.format(calendar.getTime());
            //int import_work_flow_size;
            ServiceInfo.ServiceResourceGenerate();
//            ServiceInfo service_info=new ServiceInfo(resource_file_path);
//            WorkflowInfo.getStatisticDealTime("SIPHT",0);



        }catch (Exception e){
            e.printStackTrace();
        }


    }

}
