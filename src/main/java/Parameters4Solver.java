package Paper1;

/**
 * @projectName: CodeForPaper
 * @package: Paper1
 * @className: CommonPara
 * @author: chl
 * @description: TODO
 * @date: 2023/11/12 9:31
 * @version: 1.0
 */
public class Parameters4Solver {
    private WorkflowInfo jobs;
    private ServiceInfo serviceInfo;
    private double cpu_weight;
    private double mem_weight;
    private double disk_weight;

    private boolean out_flage;
    private String out_file_path;

    private long seed;
    private int pop_num;
    private int iteration_num;
    private float cross_prob;
    private float mutation_prob;

    private int repeat_time;
    private boolean plus_model;
    private int cplex_limit_time;
    private String ha_name;

    //cplex
    public Parameters4Solver(WorkflowInfo jobs_f, ServiceInfo serviceInfo_f,double cpu_weight_f, double mem_weight_f, double disk_weight_f,
                             int cplex_limit_time_f, boolean out_flage_f, String out_file_path_f){
        jobs=jobs_f;
        serviceInfo=serviceInfo_f;
        cpu_weight=cpu_weight_f;
        mem_weight=mem_weight_f;
        disk_weight=disk_weight_f;
        cplex_limit_time=cplex_limit_time_f;
        out_flage=out_flage_f;
        out_file_path=out_file_path_f;
    }
    //ga
    public Parameters4Solver(WorkflowInfo jobs_f, ServiceInfo serviceInfo_f,double cpu_weight_f, double mem_weight_f, double disk_weight_f,
                             int pop_num_f, int iteration_num_f, float cross_prob_f, float mutation_prob_f, long seed_f, boolean out_flage_f, String out_file_path_f){
        jobs=jobs_f;
        serviceInfo=serviceInfo_f;
        cpu_weight=cpu_weight_f;
        mem_weight=mem_weight_f;
        disk_weight=disk_weight_f;
        pop_num=pop_num_f;
        iteration_num=iteration_num_f;
        cross_prob=cross_prob_f;
        mutation_prob=mutation_prob_f;
        seed=seed_f;
        out_flage=out_flage_f;
        out_file_path=out_file_path_f;
    }
    //heur
    public Parameters4Solver(WorkflowInfo jobs_f, ServiceInfo serviceInfo_f,double cpu_weight_f, double mem_weight_f, double disk_weight_f,
                             int repeat_time_f, boolean plus_model_f, int cplex_limit_time_f, long seed_f, boolean out_flage_f, String out_file_path_f){
        jobs=jobs_f;
        serviceInfo=serviceInfo_f;
        cpu_weight=cpu_weight_f;
        mem_weight=mem_weight_f;
        disk_weight=disk_weight_f;
        repeat_time=repeat_time_f;
        plus_model=plus_model_f;
        cplex_limit_time=cplex_limit_time_f;
        seed=seed_f;
        out_flage=out_flage_f;
        out_file_path=out_file_path_f;
    }

    public Parameters4Solver(WorkflowInfo jobs_f, ServiceInfo serviceInfo_f,double cpu_weight_f, double mem_weight_f, double disk_weight_f,
                             int repeat_time_f, boolean plus_model_f, int cplex_limit_time_f, long seed_f, boolean out_flage_f, String out_file_path_f,String ha_name_f){
        jobs=jobs_f;
        serviceInfo=serviceInfo_f;
        cpu_weight=cpu_weight_f;
        mem_weight=mem_weight_f;
        disk_weight=disk_weight_f;
        repeat_time=repeat_time_f;
        plus_model=plus_model_f;
        cplex_limit_time=cplex_limit_time_f;
        seed=seed_f;
        out_flage=out_flage_f;
        out_file_path=out_file_path_f;
        ha_name=ha_name_f;
    }

    public WorkflowInfo getJobs() {
        return jobs;
    }

    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    public double getCpu_weight() {
        return cpu_weight;
    }

    public double getMem_weight() {
        return mem_weight;
    }

    public double getDisk_weight() {
        return disk_weight;
    }

    public long getSeed() {
        return seed;
    }
    public String getOut_file_path() {
        return out_file_path;
    }
    public boolean getOut_flage(){
        return out_flage;
    }

    public float getCross_prob() {
        return cross_prob;
    }

    public float getMutation_prob() {
        return mutation_prob;
    }

    public int getIteration_num() {
        return iteration_num;
    }

    public int getPop_num() {
        return pop_num;
    }

    public int getRepeat_time() {
        return repeat_time;
    }

    public int getCplex_limit_time() {
        return cplex_limit_time;
    }
    public boolean getPlus_model(){
        return plus_model;
    }

    public void setOut_file_path(String out_file_path) {
        this.out_file_path = out_file_path;
    }

    public void setPlus_model(boolean plus_model) {
        this.plus_model = plus_model;
    }

    public String getHa_name() {
        return ha_name;
    }
}
