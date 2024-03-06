package Paper1;


import utils.TwoTuple;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

/**
 * @projectName: CodeForPaper
 * @package: Paper1
 * @className: GAOorModel
 * @author: chl
 * @description: TODO
 * @date: 2023/9/28 8:19
 * @version: 1.0
 */
public class SolverGAH {
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

    //遗传算法参数
    private int pop_num;
    private int iteration_num;
    private float mutation_prob;
    private float cross_prob;
    private Double T_SLA;

    private int[][] pops;
    private int[][] pops_child;
    private double[] fitness_value;
    private double[] child_fitness_value;
    private double[] local_best_fit;
    private int[][] local_best_pop;
    private double[] global_best_fit;
    private int[][] global_best_pop;

    private float[][] best_work_time;

    private Random random;
    private long seed;
    private boolean out_flage;
    private String out_file_path;
    private Map<Integer,Double> resource_weight_value;

    public SolverGAH(Parameters4Solver all_parameters_f){
        serviceInfo=all_parameters_f.getServiceInfo();
        cpu_weight=all_parameters_f.getCpu_weight();
        mem_weight=all_parameters_f.getMem_weight();
        disk_weight=all_parameters_f.getDisk_weight();

        pop_num=all_parameters_f.getPop_num();
        iteration_num=all_parameters_f.getIteration_num();
        cross_prob=all_parameters_f.getCross_prob();
        mutation_prob=all_parameters_f.getMutation_prob();
        T_SLA=all_parameters_f.getJobs().getT_SLA();
        out_flage=all_parameters_f.getOut_flage();
        seed=all_parameters_f.getSeed();
        out_file_path= all_parameters_f.getOut_file_path();

        initJobInfo(all_parameters_f.getJobs());
        initResourceMap();

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
    private double calResourceConsump(int[] pop_f){
        double pop_resource_consump=0.0;
        for (int i=0;i<dim;i++){
            if (pop_f[i]==1){
                pop_resource_consump+=resource_weight_value.get(i);
            }
        }
        return pop_resource_consump;
    }

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
    private float[][] getWorkTime(int[] pop_f){
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
                return null;
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
        if (end_jobs_time>T_SLA){
            return null;
        }else{
            return work_time;
        }

    }
    private double fitness(int[] pop_f){
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
                return calResourceConsump(pop_f)+M;
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
        if (end_jobs_time>T_SLA){
            return calResourceConsump(pop_f)+M;
        }else{
            return calResourceConsump(pop_f);
        }

    }

    private void initPops(){

        double best_fit_f;
        int[] best_pop=new int[dim];

        //第一个个体包含dim个服务，肯定是可行解，如果不是，则本问题无解。
        for (int j=0;j<dim; j++){
            pops[0][j]=1;

        }
        best_fit_f=fitness(pops[0]);
        best_pop= Arrays.copyOf(pops[0],dim);
        //对第一个进行赋值存储适应度函数
        fitness_value[0]=best_fit_f;

        for (int i=1;i<pop_num;i++){
            for (int j=0;j<dim; j++){
                pops[i][j]=random.nextInt(2);
            }
            double fit_f=fitness(pops[i]);
            fitness_value[i]=fit_f;
            if (fit_f<best_fit_f){
                best_fit_f = fit_f;
                best_pop = Arrays.copyOf(pops[i],dim);
            }

        }
        local_best_fit[0]=best_fit_f;
        local_best_pop[0]=Arrays.copyOf(best_pop,dim);
        global_best_fit[0]=best_fit_f;
        global_best_pop[0]=Arrays.copyOf(best_pop,dim);
    }

    private void initGA(long seed_f){

        pops=new int[pop_num][dim];
        pops_child=new int[pop_num][dim];
        fitness_value=new double[pop_num];
        child_fitness_value=new double[pop_num];

        local_best_fit=new double[iteration_num+1];
        local_best_pop=new int[iteration_num+1][dim];
        global_best_fit=new double[iteration_num+1];
        global_best_pop=new int[iteration_num+1][dim];

        if (seed_f>0){
            random=new Random(seed_f);
        }else{
            random=new Random();
        }

        initPops();
    }
    private void selection(){
        double fitness_all= Arrays.stream(fitness_value).sum();
        double fitness_max=Arrays.stream(fitness_value).max().getAsDouble();
        double[] fitness_select_value=new double[pop_num];
        for (int i=0;i<pop_num;i++){
            fitness_select_value[i]=fitness_max-fitness_value[i];
        }
        fitness_all= Arrays.stream(fitness_select_value).sum();

        double[] fitness_level=new double[pop_num];
        double temp_fitness=0;
        for (int i=0;i<pop_num;i++){
            temp_fitness+=fitness_select_value[i];
            fitness_level[i]=temp_fitness/fitness_all;
        }

        double r;
        for (int i=0;i<pop_num;i++){
            r=random.nextDouble();
            int j=0;
            while(r>fitness_level[j]){
                j++;
            }
            pops_child[i]=Arrays.copyOf(pops[j],dim);
        }
    }
    private void cross(){
        //int cross_location;
        int gene_temp;
        for(int i=0;i<pop_num;i=i+2){
            if (random.nextDouble()<=cross_prob){
                for (int j=0;j<dim;j++){
                    if (random.nextDouble()<0.5){
                        gene_temp=pops_child[i][j];
                        pops_child[i][j]=pops_child[i+1][j];
                        pops_child[i+1][j]=gene_temp;
                    }
                }
            }
        }
    }

/**
 * @param :
 * @return void
 * @author chl
 * @description TODO
 * @date 2023/9/30 14:53
 */
    private void mutation(){
        for (int i=0;i<pop_num;i++){
            if (random.nextDouble()<mutation_prob){
                int mutation_location=random.nextInt(dim);
                if (pops_child[i][mutation_location]==0){
                    pops_child[i][mutation_location]=1;
                }else{
                    pops_child[i][mutation_location]=0;
                }
            }
        }


    }
/**
 * @param generation_num: 迭代数，用于对比父代数据
 * @return void
 * @author chl
 * @description 计算本代中每个个体的适应度，并对最优的个体和适应度进行保存。
 * @date 2023/9/30 14:51
 */
    private void getFitness(int generation_num){
        double best_fitness=Double.POSITIVE_INFINITY;
        int[] best_pop=new int[dim];
        double fitness_temp;
        for (int i=0;i<pop_num;i++){
            fitness_temp=fitness(pops_child[i]);
            child_fitness_value[i]=fitness_temp;
            if (fitness_temp<best_fitness){
                best_fitness=fitness_temp;
                best_pop=Arrays.copyOf(pops_child[i],dim);
            }
        }

        local_best_fit[generation_num]=best_fitness;
        local_best_pop[generation_num]=Arrays.copyOf(best_pop,dim);
        if (best_fitness<global_best_fit[generation_num-1]){
            global_best_fit[generation_num]=best_fitness;
            global_best_pop[generation_num]=Arrays.copyOf(best_pop,dim);
        }else{
            global_best_fit[generation_num]=global_best_fit[generation_num-1];
            global_best_pop[generation_num]=Arrays.copyOf(global_best_pop[generation_num-1],dim);
        }

    }

    public double run(){
        try{
            System.out.println("Starting GASolver ...");
            long start_time=System.currentTimeMillis();
            initGA(seed);
            int it=1;
            int[][] pops_point=pops;
            int[][] pops_child_point=pops_child;

            double[] fitness_value_point=fitness_value;
            double[] child_fitness_value_point=child_fitness_value;

            while(it <=iteration_num){
//                if (out_flage){
//                    System.out.print("Iteration "+it+" ...\t");
//                }
                //选择子代
                selection();
                //交叉
                cross();
                //变异
                mutation();
                //计算适应度
                getFitness(it);
                //将子代变为父代
                pops=pops_child_point;
                pops_child=pops_point;
                pops_point=pops;
                pops_child_point=pops_child;
                //修改适应度
                fitness_value=child_fitness_value_point;
                child_fitness_value=fitness_value_point;
                fitness_value_point=fitness_value;
                child_fitness_value_point=child_fitness_value;
                //输出本次迭代最优
//                if (out_flage){
//                    System.out.println("local best fit: "+local_best_fit[it]);
//                }
                it++;
            }

            iteration_num=--it;
            if (out_flage){
                System.out.println("fitness value:"+global_best_fit[iteration_num]);
            }

            long stop_time=System.currentTimeMillis();
            System.out.println("GASolver stop!");

            if (out_flage){

                List<String> service_name=jobs.getAllJobKindsList();
                Map<String,List<Integer>> solution_service_map=new HashMap<>();
                for (int i=0;i<dim;i++){
                    if (global_best_pop[iteration_num][i]==1){
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
                writer.write("GA time{s}: "+(stop_time-start_time)/1000.0+"\n");
                writer.write("best object value: "+ global_best_fit[iteration_num]+"\n");
                for (String service_name_temp:solution_service_map.keySet()){
                    writer.write(service_name_temp+":"+solution_service_map.get(service_name_temp).toString()+"\t");
                }
                writer.write("\n");
                writer.write("T_SLA: "+T_SLA+"\n");

                writer.write("pop_num: "+pop_num+"\titeration_num:"+iteration_num+"\n");
                writer.write("cross_prob: "+cross_prob+"\tmutation_prob:"+mutation_prob+"\n");

                writer.write("local_best_fit: "+Arrays.toString(local_best_fit)+"\n");
                writer.write("global_best_fit: "+Arrays.toString(global_best_fit)+"\n");

                writer.write("work_time的值为："+"\n");
                best_work_time=getWorkTime(global_best_pop[iteration_num]);
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

        return global_best_fit[iteration_num];
    }
}
