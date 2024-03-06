package Paper1;

import java.util.HashSet;

/**
 * @projectName: CodeForPaper
 * @package: Paper1
 * @className: ExpForGA_BigJobs
 * @author: chl
 * @description: TODO
 * @date: 2023/10/4 10:44
 * @version: 1.0
 */
public class ExpForGA_BigJobs {

    public static void main(String[] args){
        int pop_num=100;
        int iteration_num=1000;
        float cross_prob=0.1f;
        float mutation_prob=0.1f;
        double T_SLA=2000;
        boolean out_flage=true;
        long jobs_seed=10;
        long GA_seed=10;

        String in_fold_path="/";
        String in_file_path="LIGO.n.1000.1.dax";

        String out_fold_path="F:\\桌面\\博士课题研究\\基于工作流的服务网络拓扑优化\\Expfordiffdataset\\";
        String out_file_path="SIPHT.n.6000.15.dax.txt";


//        CompositeJob jobs=new CompositeJob(in_fold_path+in_file_path,"", new HashSet<>(),0,T_SLA,jobs_seed);
//        SolverGAH gasolver=new SolverGAH(jobs,pop_num,iteration_num,cross_prob,mutation_prob,out_flage,GA_seed,out_fold_path+"GA."+out_file_path);
//        int optimal_solution_ga=gasolver.run();
    }






}
