package Paper1;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import utils.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class WorkflowInfo {

    //key=ID  value=name, runtime
    private Map<String,TwoTuple<String,Float>> node_info;

    //#key=(parent,child)  value= edge_weight
    private Map<TwoTuple<String,String>,Long> edge_info;

    private List<String> node_id_list;

    private Map<String,Integer> id_to_seqnumber;

    private String file_path;

    private double T_SLA;
    private Random random;
    List<String> alljob_kindslist=null;




    public WorkflowInfo(){
        file_path="/LIGO.n.50.3.dax";
        T_SLA=2000;
        readXml(file_path);
    }

    public WorkflowInfo(String file_path_f,Set<String> rest_job, int rest_job_num, double T_SLA_f, long seed){
        file_path=file_path_f;
        readXml(file_path_f);

        if (seed>0){
            random=new Random(seed);
        }else{
            random=new Random();
        }

        //进行job筛选
        if (rest_job.size()!=0 && rest_job_num != 0){
            System.out.println("parameters for job select is wrong!");
        } else if (rest_job.size()!=0){
            selectSomeJobs(rest_job);
        }else if(rest_job_num!=0){
            if (rest_job_num>node_id_list.size()){
                System.out.println("rest_job_num is too big!");
                System.exit(0);
            }
            selectSomeJobs(rest_job_num);
        }
        T_SLA=T_SLA_f;
    }

    public WorkflowInfo(String file_path_f){
        file_path=file_path_f;
        readXml(file_path_f);
    }

    public String getJobKind(String job_id){
        if(!node_info.keySet().contains(job_id)){
            return null;
        }
        return node_info.get(job_id).first;
    }

    public String getJobKind(Integer job_order){
        if (alljob_kindslist==null){
            alljob_kindslist=getAllJobKindsList();
        }
        return alljob_kindslist.get(job_order);

    }

    public Float getJobTime(String job_id){
        if(!node_info.keySet().contains(job_id)){
            return null;
        }
        //in
        return node_info.get(job_id).second;
    }

    public Set<String> getAllJobIdSet(){
        Set<String> all_job_id_set=new HashSet<>();
        for (String job_id:node_info.keySet()){
            all_job_id_set.add(job_id);
        }
        return all_job_id_set;
    }

    public Long getFileSizeBetweenJobs(String job1,String job2){
        TwoTuple<String, String> key=new TwoTuple<>(job1,job2);
        Set<TwoTuple<String, String>> keyset=edge_info.keySet();
        if(edge_info.keySet().contains(key)){
            return edge_info.get(new TwoTuple<>(job1,job2));
        }else{
            System.out.println("Wrong!");
        }
        return null;
    }

    public Set<String> getInitJobsinName(){
        //由于不能直接删map.keyset()，因此需要先创建新的Set，对新的Set进行操作。
        Set<String> InitJobs=new HashSet<>();
        Iterator it=node_info.keySet().iterator();
        while(it.hasNext()){
            String name=(String)it.next();
            InitJobs.add(name);
        }
        //对出现在子节点的任务，执行删除，最终剩下初始节点任务。
        for(TwoTuple<String, String> edge: edge_info.keySet()){
            if(InitJobs.contains(edge.second))
            InitJobs.remove(edge.second);
        }
        return InitJobs;
    }


    public Set<Integer> getInitJobsinOrder(){

        Set<String> init_job_string=getInitJobsinName();
        Set<Integer> init_job_int=new HashSet<>();

        Iterator it=init_job_string.iterator();
        while(it.hasNext()){
            String name=(String)it.next();
            init_job_int.add(id_to_seqnumber.get(name));
        }
        return init_job_int;
    }
    //返回节点所有类型的列表
    public List<String> getAllJobKindsList(){
        List<String> all_job_kinds=new ArrayList<>();
        for(String job_id:node_id_list){
            all_job_kinds.add(node_info.get(job_id).first);
        }
        return all_job_kinds;
    }

    public Set<String> getAllJobKindsSet(){
        Set<String> all_job_kinds=new HashSet<>();
        for(String job_id:node_id_list){
            all_job_kinds.add(node_info.get(job_id).first);
        }
        return all_job_kinds;
    }

    public float[] getAllJobTime(){
        float[] all_job_time=new float[node_info.keySet().size()];
        int i=0;
        for(String job_id:node_id_list){
            all_job_time[i]=node_info.get(job_id).second;
            i++;
        }
        return all_job_time;
    }

    public int[][] getAllDealOrder(){

        int[][] deal_order=new int[edge_info.keySet().size()][2];
        int j=0;
        for (TwoTuple<String,String> edge: edge_info.keySet()){
            deal_order[j][0]=id_to_seqnumber.get(edge.first);
            deal_order[j][1]=id_to_seqnumber.get(edge.second);
            j++;
        }
        return deal_order;
    }

    public Map<String, Integer> getJobsKindsNumMap(){
        Map<String,Integer> each_job_num=new HashMap<>();
        for(String job_kind:getAllJobKindsSet()){
            each_job_num.put(job_kind,0);
        }
        for(String job_id:node_info.keySet()){
            //多一个任务，相应类型的任务数量增加1个
            each_job_num.put(node_info.get(job_id).first,each_job_num.get(node_info.get(job_id).first)+1);
        }
        return each_job_num;
    }

    public Map<String,Integer> getServiceKindsNumMap(Integer all_nums){
        Map<String, Integer>all_service_kinds_num=getJobsKindsNumMap();
        if (all_nums<all_service_kinds_num.keySet().size()){
            System.out.println("getServiceKindsNumMap：设定的服务数量太少！");
            System.exit(-1);
        }
        //总任务数量
        Integer job_num=all_service_kinds_num.values().stream().mapToInt(Integer::intValue).sum();
        //分配结果
        Map<String,Integer> each_service_num=new HashMap<>();
        //已经分配的数量
        Integer service_num_allocated=0;
        //记录剩余权重，用于分配剩余指标
        List<TwoTuple<String, Double>> service_weight_rest=new ArrayList<>();
        //通过循环初步吧整数部分分配，然后小数部分存储在service_weight_rest中
        for(String service_name:all_service_kinds_num.keySet()){
            Double weight=all_service_kinds_num.get(service_name)/job_num.doubleValue()*all_nums;
            //分配整数部分
            Integer temp = (int)Math.floor(weight);
            service_num_allocated+=temp;
            each_service_num.put(service_name,temp);
            service_weight_rest.add(new TwoTuple<>(service_name,weight-Math.floor(weight)));
        }
        service_weight_rest.sort(new Comparator<TwoTuple<String, Double>>() {
            @Override
            public int compare(TwoTuple<String, Double> o1, TwoTuple<String, Double> o2) {
                Double w1=o1.second;
                Double w2=o2.second;
                return w2.compareTo(w1);
            }
        });
        //将剩余权重较大的，按照不足的服务数量顺序依次增加1
        IntStream.range(0,all_nums-service_num_allocated).forEach( i->
            each_service_num.put(service_weight_rest.get(i).first,each_service_num.get(service_weight_rest.get(i).first)+1)
        );
        //检查是否有为0的，如果有，则强制加一
        Boolean flage=true;
        String max_name="";
        String zero_name="";
        Integer max_num=0;

        while(flage){
            flage =false;
            for(String service_name: each_service_num.keySet()){
                if(each_service_num.get(service_name) == 0){
                    zero_name=service_name;
                    flage=true;
                }else if(each_service_num.get(service_name)>max_num){
                    max_name=service_name;
                    max_num=each_service_num.get(service_name);
                }
            }
            if(flage){
                each_service_num.put(zero_name,1);
                each_service_num.put(max_name,each_service_num.get(max_name)-1);
            }
        }

        return each_service_num;
    }

    public Set<String> getNexJobs(String job_id){
        Set<String> next_jobs_id=new HashSet<>();
        for(TwoTuple<String, String> edge:edge_info.keySet()){
            if(edge.first.equals(job_id)){
                next_jobs_id.add(edge.second);
            }
        }
        return next_jobs_id;
    }

    public Set<String> getFrontJobs(String job_id){
        Set<String> front_jobs_id=new HashSet<>();
        for(TwoTuple<String, String> edge:edge_info.keySet()){
            if(edge.second.equals(job_id)){
                front_jobs_id.add(edge.first);
            }
        }
        return front_jobs_id;
    }



    public Map<Integer,List<Integer>> getMapWithChildsList(){
        Map<Integer,List<Integer>> job_child_map=new HashMap<>();
        for (String job_name:node_id_list){
            Set<String> child_set=getNexJobs(job_name);
            List<Integer> child_list= new ArrayList<>();
            for (String child:child_set){
                child_list.add(id_to_seqnumber.get(child));
            }
            job_child_map.put(id_to_seqnumber.get(job_name),child_list);
        }

        return job_child_map;
    }

    public Map<Integer,List<Integer>> getMapWithParentList(){
        Map<Integer,List<Integer>> job_parent_map=new HashMap<>();
        for (String job_name:node_id_list){

            Set<String> parent_set=getFrontJobs(job_name);
            List<Integer> parent_list= new ArrayList<>();
            for (String parent:parent_set){
                parent_list.add(id_to_seqnumber.get(parent));
            }
            job_parent_map.put(id_to_seqnumber.get(job_name),parent_list);
        }

        return job_parent_map;
    }


    private void readXml(String file_path_f){
        List<TwoTuple<String,Long>> file_in_list, file_out_list;
        Map<String,TwoTuple<List<TwoTuple<String,Long>>,List<TwoTuple<String,Long>>>> job_file_info;

        job_file_info=new HashMap<>();
        node_info=new HashMap<>();
        edge_info=new HashMap<>();
        try {

            // 创建SAXReader对象
            SAXReader reader = new SAXReader();
            Document dc;
            //file_path_f="/LIGO.n.50.3.dax";

            InputStream is=this.getClass().getResourceAsStream(file_path_f);
            // 加载xml文件
            if (is != null){
                dc= reader.read(is);
            }else{
                dc= reader.read(new File(file_path_f));
            }

//            Document dc= reader.read(is);
            // 获取根节点
            Element rootElement  = dc.getRootElement();
            // 获取迭代器
            Iterator it = rootElement.elementIterator();
            // 遍历迭代器，获取根节点信息
            while(it.hasNext()){
                //获取下一次迭代的元素
                Element ele_level1 = (Element)it.next();
                if(ele_level1.getName().equals("job")){
                    String id=ele_level1.attribute("id").getValue();
                    String name=ele_level1.attribute("name").getValue();
                    Float runtime=Float.parseFloat(ele_level1.attribute("runtime").getValue())/1000;
                    TwoTuple<String,Float> value_temp=new TwoTuple<String,Float>(name,runtime);
                    node_info.put(id,value_temp);
                    //////////////////////////上面读取顶点信息，下面读取顶点间传输文件的信息/////////////////////////////////////
                    Iterator nodefiles_it=ele_level1.elementIterator();
                    file_in_list=new LinkedList<>();
                    file_out_list= new LinkedList<>();

                    while(nodefiles_it.hasNext()){
                        Element ele_level2=(Element)nodefiles_it.next();
                        String file_name=ele_level2.attribute("file").getValue();
                        String file_kind=ele_level2.attribute("link").getValue();
                        long file_size=Long.parseLong(ele_level2.attribute("size").getValue());
                        if(file_kind.equals("input")){
                            file_in_list.add(new TwoTuple<>(file_name,file_size));
                        }else{
                            file_out_list.add(new TwoTuple<>(file_name,file_size));
                        }
                    }
                    job_file_info.put(id,new TwoTuple<>(file_in_list,file_out_list));
                }else if(ele_level1.getName().equals("child")){
                    String child_id=ele_level1.attribute("ref").getValue();
                    Iterator parentid_it=ele_level1.elementIterator();
                    while(parentid_it.hasNext()){
                        Element parent=(Element) parentid_it.next();
                        String parent_id=parent.attribute("ref").getValue();
                        //下面求取两个任务之间传输的文件大小——两个循环
                        Long file_size=0l;
                        for(TwoTuple<String, Long> p_file_temp:job_file_info.get(parent_id).second){
                            for(TwoTuple<String, Long> c_file_temp:job_file_info.get(child_id).first){
                                //存疑问题，到底要不要文件大小相等 && p_file_temp.second == c_file_temp.second
                                if(p_file_temp.first.equals(c_file_temp.first)){
                                    file_size+=p_file_temp.second;
                                }
                            }
                        }
                        edge_info.put(new TwoTuple<>(parent_id,child_id),file_size);
                    }
                }else{
                    System.out.println("XML文件超出预期");
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            System.out.println("CompositeJob file open wrong!");
            System.exit(-1);
        }
        node_id_list=new ArrayList<>();

        for (String node_id:node_info.keySet()){
            node_id_list.add(node_id);
        }
        node_id_list.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });

        id_to_seqnumber=new HashMap<>();
        int i=0;
        for(String id:node_id_list){
            id_to_seqnumber.put(id,i);
            i++;
        }

    }
    public void selectSomeJobs(Set<String> rest_job){
        //删除多于顶点
        for (Iterator<Map.Entry<String,TwoTuple<String,Float>>> it=node_info.entrySet().iterator();it.hasNext();){
            Map.Entry<String,TwoTuple<String,Float>> item =it.next();
            if (!rest_job.contains(item.getKey())){
                it.remove();
                node_id_list.remove(item.getKey());
            }
        }
        //删除多于连边
        for (Iterator<Map.Entry<TwoTuple<String, String>,Long>> it=edge_info.entrySet().iterator();it.hasNext();){
            Map.Entry<TwoTuple<String, String>,Long> item=it.next();
            if (!rest_job.contains(item.getKey().first) || !rest_job.contains(item.getKey().second)){
                it.remove();
            }
        }

        int i=0;
        id_to_seqnumber.clear();
        for(String id:node_id_list){
            id_to_seqnumber.put(id,i);
            i++;
        }

    }

    public void selectSomeJobs(int job_num){

        List<String> candidate_jobs=new ArrayList<>(getInitJobsinName());

        Set<String> seleted_jobs=new HashSet<>();
        String main_job=candidate_jobs.remove(random.nextInt(candidate_jobs.size()));
        seleted_jobs.add(main_job);
        while(getNexJobs(main_job).size()!=0 && seleted_jobs.size()<job_num){
            String next_main_job=null;
            for(String job:getNexJobs(main_job)){
                if(next_main_job==null){
                    next_main_job=job;
                    continue;
                }
                candidate_jobs.add(job);
            }

            main_job=next_main_job;
            seleted_jobs.add(main_job);
        }

        while(seleted_jobs.size()<job_num){
            main_job=candidate_jobs.remove(random.nextInt(candidate_jobs.size()));
            for(String job:getNexJobs(main_job)){
                if(!candidate_jobs.contains(job)){
                    candidate_jobs.add(job);
                }
            }
            seleted_jobs.add(main_job);
        }

        //删除多余顶点
        for (Iterator<Map.Entry<String,TwoTuple<String,Float>>> it=node_info.entrySet().iterator();it.hasNext();){
            Map.Entry<String,TwoTuple<String,Float>> item =it.next();
            if (!seleted_jobs.contains(item.getKey())){
                it.remove();
                node_id_list.remove(item.getKey());
            }
        }
        //删除多余连边
        for (Iterator<Map.Entry<TwoTuple<String, String>,Long>> it=edge_info.entrySet().iterator();it.hasNext();){
            Map.Entry<TwoTuple<String, String>,Long> item=it.next();
            if (!seleted_jobs.contains(item.getKey().first) || !seleted_jobs.contains(item.getKey().second)){
                it.remove();
            }
        }

        node_id_list.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });

        int i=0;
        id_to_seqnumber.clear();
        for(String id:node_id_list){
            id_to_seqnumber.put(id,i);
            i++;
        }
    }

    public String getFilePath(){
        return file_path;
    }

    public double getT_SLA(){
        return T_SLA;
    }

    public double getKeyPathTime(){
        Set<Integer> init_jobs=getInitJobsinOrder();
        int dim=node_info.keySet().size();
        Map<Integer, List<Integer>> job_child_map=getMapWithChildsList();
        Map<Integer, List<Integer>> job_parent_map=getMapWithParentList();
        float[] deal_time=getAllJobTime();
        List<TwoTuple<Integer,Float>> job_queue=new ArrayList<>();
        Set<Integer> job_done=new HashSet<>();

        for (Integer job_id:init_jobs){
            job_queue.add(new TwoTuple<>(job_id,0.0f));
        }

        float this_job_end_time=0;
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

            this_job_end_time=job_f.second+deal_time[job_f.first];

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
        return this_job_end_time;

    }

    public int getJobNum(){
        return node_info.keySet().size();
    }

    public Map<TwoTuple<Integer, Integer>,Long> getJob2JobFileSize(){
        Map<TwoTuple<Integer, Integer>,Long> job_job_filesize=new HashMap<>();
        edge_info.forEach((key,value)->{
            job_job_filesize.put(new TwoTuple<>(id_to_seqnumber.get(key.first),id_to_seqnumber.get(key.second)),value);

        });
        return job_job_filesize;
    }

    public void resetDealTime_Stable(ServiceInfo service_info,double ratio){
        for (int i=0; i<getJobNum();i++){
            String service_kind= getAllJobKindsList().get(i);
            String service_id=node_id_list.get(i);
            double new_deal_time=service_info.getServiceTime_stable(service_kind,ratio);
            node_info.put(service_id,new TwoTuple<>(node_info.get(service_id).first,(float)new_deal_time));
        }

    }
    public void resetDealTime_Random(ServiceInfo service_info){
        for (int i=0; i<getJobNum();i++){
            String service_kind= getAllJobKindsList().get(i);
            String service_id=node_id_list.get(i);
            double new_deal_time=service_info.getServiceNextTime_statistics(service_kind);
            node_info.put(service_id,new TwoTuple<>(node_info.get(service_id).first,(float)new_deal_time));

        }
    }
    public double getDealTimeSum(){
        double time_sum=0;
        float[] all_time=getAllJobTime();
        for(int i=0;i<getJobNum();i++){
            time_sum+=all_time[i];
        }
        return time_sum;
    }

/////////////////////////////////////////////静态方法/////////////////////////////////////////////////////////////
    private static Random random_s;
    private static Map<String,List<Float>> service_time_map;
    private static int x_num;
    private static List<Float> x_list;



/**
 * @param workflow_kind: 工作流类型，如果要统计所有类型，workflow_kind=""
 * @param workflow_size: 工作流大小，如果要统计所有规模，workflow_size=0
 * @return void
 * @author chl
 * @description 得到服务处理的统计数据，输出到文件。
 * @date 2023/11/12 11:20
 */
    public static void getStatisticDealTime(String workflow_kind,int workflow_size){
        random_s=new Random();
        service_time_map=new HashMap<>();
        x_num=10;
        x_list=new ArrayList<>();

        try{
            String in_file_path="F:\\桌面\\博士课题研究\\data\\Workflow\\";
            String out_dir_path="F:\\桌面\\博士课题研究\\基于工作流的服务网络拓扑优化\\Expfordiffdataset\\service info\\";

            initServiceTimeMap(in_file_path,workflow_kind,workflow_size);

            Calendar calendar=Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("MM.dd-HH.mm.ss");
            String exp_flage=formatter.format(calendar.getTime());

            File file_out=new File(out_dir_path+"time_data."+workflow_kind+"."+workflow_size+"."+exp_flage+".txt");
            if (!file_out.exists()){
                file_out.createNewFile();
            }
            FileWriter writer=new FileWriter(file_out,true);

            for (String service_kind:getService_time_map().keySet()){
                if (!workflow_kind.equals("") && !service_kind.split("::")[0].equals(workflow_kind)){
                    continue;
                }
                Map<Integer, Float> analysis_map=analysis_int(getService_time_map().get(service_kind));
                System.out.println(service_kind);

                grawValue(analysis_map);

                writer.write(service_kind+"::");
                writer.write(getService_time_map().get(service_kind)+"\n");
            }

//            List<Double> mydata=new ArrayList<>();
//            for (int i=0;i<1000;i++){
//                mydata.add(getGaussian(5.12,0.319));
//            }
//            writer.write("service_kind::mydata"+"::");
//            writer.write(mydata.toString());

            writer.flush();
            writer.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private static double getGaussian(double ave_f, double std_f){
        double value=std_f*random_s.nextGaussian()+ave_f;
        return (double)Math.round(value*1000)/1000;
    }
    private static void initServiceTimeMap(String in_file_path, String work_flow_kind, int workflow_size){
        File root_file=new File(in_file_path);
        for(File dir_diff_dataset:root_file.listFiles()){
            if (!work_flow_kind.equals("") && !dir_diff_dataset.getName().equals(work_flow_kind)){
                continue;
            }
            System.out.println(dir_diff_dataset.getName());
            for (File dax_file:dir_diff_dataset.listFiles()){
                String file_name=dax_file.getName();
                if (file_name.split("\\.")[4].equals("dax") && file_name.split("\\.")[2].length()<5 ){
                    if (workflow_size != 0 && Integer.parseInt(file_name.split("\\.")[2])!=workflow_size ){
                        continue;
                    }
                    WorkflowInfo jobs=new WorkflowInfo(dax_file.getAbsolutePath());
                    List<String> service_kind_list=jobs.getAllJobKindsList();
                    float[] service_time=jobs.getAllJobTime();
                    int dim=jobs.getJobNum();
                    for (int i=0;i<dim;i++){
                        String service_kind=dir_diff_dataset.getName()+"::"+service_kind_list.get(i);
                        if (!service_time_map.keySet().contains(service_kind)){
                            List<Float> time_list=new ArrayList<>();
                            time_list.add(service_time[i]);
                            service_time_map.put(service_kind,time_list);
                        }else{
                            service_time_map.get(service_kind).add(service_time[i]);
                        }
                    }

                }

            }
        }
    }
    /**
     * @param list:
     * @return Map<Float,Float>
     * @author chl
     * @description 和analysis_int的区别在于输出的map的key类型不同，本函数是均值
     * @date 2023/11/12 11:27
     */
    private static Map<Float,Float> analysis(List<Float> list){
        Map<Float,Float> map=new HashMap<>();
        Supplier<Stream<Float>> supp=()->list.stream();//Lambda表达式给供应商
        //为Stream提供一个比较器
        Comparator<Float> comp=(e1,e2)->e1>e2?1:-1;
        //获取最大最小值
        float max=supp.get().max(comp).get();
        float min=supp.get().min(comp).get();
        float range=(max-min)/x_num;//计算统计区间的单位范围
        //将每一个标记区的数据统计后放入map中。
        for(int i=1;i<=x_num;i++) {
            float start=min+(i-1)*range;
            float end=min+i*range;
            Stream<Float> stream=supp.get().filter((e)->e>=start).filter((e)->e<end);
            map.put((start+end)/2,(float)stream.count());///list.size()
        }
        return map;
    }
    private static Map<Integer,Float> analysis_int(List<Float> list){
        Map<Integer,Float> map=new HashMap<>();
        x_list=new ArrayList<>();
        Supplier<Stream<Float>> supp=()->list.stream();//Lambda表达式给供应商
        //为Stream提供一个比较器
        Comparator<Float> comp=(e1,e2)->e1>e2?1:-1;
        //获取最大最小值
        float max=supp.get().max(comp).get();
        float min=supp.get().min(comp).get();
        float range=(max-min)/x_num;//计算统计区间的单位范围
        //将每一个标记区的数据统计后放入map中。
        for(int i=1;i<=x_num;i++) {
            float start=min+(i-1)*range;
            float end=min+i*range;
            Stream<Float> stream=supp.get().filter((e)->e>=start).filter((e)->e<end);
            map.put(i,(float)stream.count()/list.size());//
            x_list.add((start+end)/2);
        }
        return map;
    }

    private static void grawValue(Map<Integer,Float> analysis_map) {
        int ScaleSize=14;//x轴刻度长度
        int xSize=10;
        double avgScale=1.0/xSize;
        int printSize=ScaleSize-String.valueOf(avgScale).length();
        //打印X轴、刻度以及刻度值
        for(int i=0;i<=xSize;i++) {
            printChar(' ',printSize);
            System.out.print(String.format("%.2f",i*avgScale));
        }
        System.out.println("");
        for(int i=0;i<=xSize+1;i++) {
            if(i==0) {
                printChar(' ',printSize);
            }else {
                printChar('-',ScaleSize);
            }
        }
        System.out.println();
        //绘制统计内容
        for(int i=1;i<=x_num;i++) {
            printChar(' ', printSize-1-String.valueOf(i).length());
            System.out.print(x_list.get(i-1)+":");
            float scaleValue=analysis_map.get(i);

            printChar('█', (int)(scaleValue*200));
            System.out.println(" "+scaleValue+"\n");
        }
    }
    private static void printChar(char c, int number){
        for(int i=0;i<number; i++){
            System.out.print(c);
        }
    }

    private static Map<String, List<Float>> getService_time_map() {
        return service_time_map;
    }


}



