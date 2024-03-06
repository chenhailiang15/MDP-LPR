package Paper1;

import java.util.HashSet;
import java.util.Set;

/**
 * @projectName: CodeForPaper
 * @package: Paper1
 * @className: ExpForJobSize
 * @author: chl
 * @description: TODO
 * @date: 2023/10/2 20:14
 * @version: 1.0
 */
public class ExpForJobSize {


    public static void main(String[] args){
        int pop_num=100;
        int iteration_num=100;
        float cross_prob=0.1f;
        float mutation_prob=0.1f;
        double T_SLA=2000;
        boolean out_flage=false;

        String in_fold_path="F:\\桌面\\博士课题研究\\data\\Workflow\\";
        String in_file_path="LIGO\\LIGO.n.50.3.dax";

        String out_fold_path="F:\\桌面\\博士课题研究\\基于工作流的服务网络拓扑优化\\Expfordiffdataset\\";
        String out_file_path="LIGO.n.50.3.dax.txt";


        Set<String> rest_job10=new HashSet<String>(){{
            add("ID00000");add("ID00012");add("ID00024");add("ID00025");add("ID00037");
            add("ID00049");add("ID00005");add("ID00017");add("ID00029");add("ID00041");
        }};
        int rest_job_num=0;
        long jobs_seed=8;
        long GA_seed=10;
        WorkflowInfo jobs=new WorkflowInfo(in_fold_path+in_file_path,rest_job10,rest_job_num,T_SLA,jobs_seed);
        long time1=System.currentTimeMillis();
//        SolverGAH gasolver=new SolverGAH(jobs,pop_num,iteration_num,cross_prob,mutation_prob,out_flage,GA_seed,out_fold_path+"GA."+out_file_path);
//        int optimal_solution_ga=gasolver.run();
//
//        long time2=System.currentTimeMillis();
//        double cplex_resolution=0.1;
//        SolverCplexDis cplexsolver=new SolverCplexDis(jobs,cplex_resolution,0,out_flage,out_fold_path+"Cplex."+out_file_path);
//        cplexsolver.run();
//        long time3=System.currentTimeMillis();

    }


}
