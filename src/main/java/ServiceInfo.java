import utils.TwoTuple;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @projectName: CodeForPaper
 * @package: Paper1
 * @className: ServiceInfo
 * @author: chl
 * @description: TODO
 * @date: 2023/11/12 10:47
 * @version: 1.0
 */
public class ServiceInfo {
    private Map<String, Double> cpu_resource_map;
    private Map<String, Double> mem_resource_map;
    private Map<String, Double> disk_resource_map;
    private static Random random;
    Map<String, TwoTuple<Double,Double>> service_time_distribute;
    Map<String, List<Float>> service_time_data;
    Map<String, Integer> service_time_number;
    Map<Double,Double> fractiles;
    private static double cpu_ave;
    private static double cpu_std;
    private static double mem_ave;
    private static double mem_std;
    private static double disk_ave;
    private static double disk_std;
    private static boolean init_flage=false;

    /**
     * @param file_path_f:
     * @return null
     * @author chl
     * @description 通过读取文件得到资源消耗量，比较固定
     * @date 2023/12/8 9:20
     */
    public ServiceInfo(String file_path_f){
        initResource(file_path_f);
    }
    /**
     * @param :
     * @return null
     * @author chl
     * @description 每次初始化，随机生成资源消耗量，主要用于启发式对比
     * @date 2023/12/8 9:19
     */
    public ServiceInfo(long seed_f){
        if (seed_f==0){
            random=new Random();
        }else{
            random=new Random(seed_f);
        }
        initResource_random();
    }
    /**
     * @param file_path_f:
     * @param time_data_file_path_f:
     * @param seed_f:
     * @return null
     * @author chl
     * @description 用于根据统计数据生成随机的工作流
     * @date 2023/12/8 9:18
     */
    public ServiceInfo(String file_path_f, String time_data_file_path_f,long seed_f){
        if (seed_f==0){
            random=new Random();
        }else{
            random=new Random(seed_f);
        }
        initResource(file_path_f);
        loadStatisticTime(time_data_file_path_f);
    }

    public double getServiceTime_stable(String service_kind,double ratio) {
        int data_number = service_time_number.get(service_kind);
        int next_index =(int)Math.round(data_number*ratio) ;
        return service_time_data.get(service_kind).get(next_index);
    }

    public double getServiceNextTime_statistics(String service_kind) {
        int data_number = service_time_number.get(service_kind);
        int next_index = random.nextInt(data_number);
        return service_time_data.get(service_kind).get(next_index);
    }
    public double getServiceNextTime_generate(String service_kind){
        if (service_kind.equals("Inspiral")){
            double min=service_time_distribute.get(service_kind).first;
            double max=service_time_distribute.get(service_kind).second;
            return random.nextDouble()*(max-min)+min;
        }else{
            double mean=service_time_distribute.get(service_kind).first;
            double std=service_time_distribute.get(service_kind).second;
            return getGaussian(mean,std);
        }
    }
    public double getGama(Map<String, Integer> kinds_num,double ratio){
        double numerator=0;
        double denominator=0;
        for(String service_kind:kinds_num.keySet()){
            if (service_kind.equals("Inspiral")){
                double min=service_time_distribute.get(service_kind).first;
                double max=service_time_distribute.get(service_kind).second;
                numerator+=(max-min)*(max-min)*kinds_num.get(service_kind)/12;
                denominator+=(max+min)*kinds_num.get(service_kind)/2;
            }else{
                double mean=service_time_distribute.get(service_kind).first;
                double std=service_time_distribute.get(service_kind).second;
                numerator+=std*std*kinds_num.get(service_kind);
                denominator+=mean*kinds_num.get(service_kind);
            }
        }
        double gama=1+numerator/denominator*fractiles.get(ratio);
        System.out.println(gama);
        return 1+numerator/denominator*fractiles.get(ratio);


    }
    public double getMeanTime(String service_kind){
        if (service_kind.equals("Inspiral")){
            double min=service_time_distribute.get(service_kind).first;
            double max=service_time_distribute.get(service_kind).second;
            return (max+min)/2;
        }else{
            double mean=service_time_distribute.get(service_kind).first;
            return mean;
        }
    }
    private void loadStatisticTime(String file_path_f){
//        if (!workflow_kind_f.equals("LIGO")){
//            System.out.println("ServiceInfo: workflow kind wrong!");
//            System.exit(-1);
//        }
        try{
            service_time_data=new HashMap<>();
            service_time_number=new HashMap<>();
            FileInputStream fin= new FileInputStream(file_path_f);
            InputStreamReader reader=new InputStreamReader(fin);
            BufferedReader bufferedReader=new BufferedReader(reader);
            String line;
            while((line=bufferedReader.readLine())!=null){
                String[] line_split=line.split("::");
                String service_kind=line_split[1];
                List<Float> time_datas=new ArrayList<>();
                String time_data=line_split[2].substring(1,line_split[2].length()-1);
                for( String time_t:time_data.split(",")){
                    time_datas.add(Float.parseFloat(time_t));
                }
                time_datas.sort(new Comparator<Float>() {
                    @Override
                    public int compare(Float o1, Float o2) {
                        return o1.compareTo(o2);
                    }
                });
                service_time_data.put(service_kind,time_datas);
                service_time_number.put(service_kind,time_datas.size());
            }

        }catch(Exception e){
            e.printStackTrace();
        }

    }
    private void loadStatisticTime_LIGO(String workflow_kind_f){
        if (!workflow_kind_f.equals("LIGO")){
            System.out.println("ServiceInfo: workflow kind wrong!");
            System.exit(-1);
        }

        service_time_distribute=new HashMap<String,TwoTuple<Double,Double>>(){{
            put("Inspiral",new TwoTuple<Double,Double>(230.920,689.980));
            put("TmpltBank",new TwoTuple<Double,Double>(18.127,0.435));
            put("Thinca",new TwoTuple<Double,Double>(5.372,0.223));
            put("TrigBank",new TwoTuple<Double,Double>(5.121,0.319));
        }};

        fractiles=new HashMap<Double,Double>(){{
            put(0.1,-1.282);put(0.2,-0.842);put(0.3,-0.524);put(0.4,-0.253);put(0.5,0.0);
            put(0.6,0.253);put(0.7,0.524);put(0.8,0.842);put(0.9,1.282);put(0.95,1.645);
            put(0.975,1.960);put(0.98,2.054);put(0.99,2.326);put(0.995,2.576);
        }};

    }
    private boolean initResource_random(){
        try{

            cpu_resource_map=new HashMap<>();
            mem_resource_map=new HashMap<>();
            disk_resource_map=new HashMap<>();
            String[] file_list=new String[]{
                    "/SIPHT.n.50.0.dax","/LIGO.n.50.0.dax","/GENOME.n.50.0.dax","/MONTAGE.n.50.0.dax",
                    "/CYBERSHAKE.n.50.0.dax"
            };
            for (String file_name : file_list){
                WorkflowInfo jobs=new WorkflowInfo(file_name);
                for(String job_name:jobs.getAllJobKindsSet()){
                    cpu_resource_map.put(job_name,Math.abs(getGaussian(cpu_ave,cpu_std)));
                    mem_resource_map.put(job_name,Math.abs(getGaussian(mem_ave,mem_std)));
                    disk_resource_map.put(job_name,Math.abs(getGaussian(disk_ave,disk_std)));
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }

    private boolean initResource(String file_path_f){
        try{

            cpu_resource_map=new HashMap<>();
            mem_resource_map=new HashMap<>();
            disk_resource_map=new HashMap<>();
            InputStream is=this.getClass().getResourceAsStream(file_path_f);
            InputStreamReader reader = new InputStreamReader(is); // 建立一个输入流对象reader
            BufferedReader br = new BufferedReader(reader); // 建立一个对象，它把文件内容转成计算机能读懂的语言
            String line = br.readLine();
            while(line != null){
                String service_kind=line.split("::")[1];
                double cpu_resource=Double.parseDouble(line.split("::")[2]);
                double mem_resource=Double.parseDouble(line.split("::")[3]);
                double disk_resource=Double.parseDouble(line.split("::")[4]);
                cpu_resource_map.put(service_kind,cpu_resource);
                mem_resource_map.put(service_kind,mem_resource);
                disk_resource_map.put(service_kind,disk_resource);
                line=br.readLine();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }


    public Map<String, Double> getCpu_resource_map() {
        return cpu_resource_map;
    }

    public Map<String, Double> getMem_resource_map() {
        return mem_resource_map;
    }

    public Map<String, Double> getDisk_resource_map() {
        return disk_resource_map;
    }
/////////////////////////////////////////////////////静态方法///////////////////////////////////////////////////////


    private static void initParas(){
        cpu_ave=10;
        cpu_std=1;
        mem_ave=10;
        mem_std=1;
        disk_ave=10;
        disk_std=1;
        init_flage=true;
    }
    private static double getGaussian(double ave_f, double std_f){
        return std_f*random.nextGaussian()+ave_f;
    }

    public static void ServiceResourceGenerate(){
        try{
            if (!init_flage){
                initParas();
            }
            random=new Random();

            String in_file_path="F:\\桌面\\博士课题研究\\data\\Workflow\\";
            String out_dir_path="F:\\桌面\\博士课题研究\\基于工作流的服务网络拓扑优化\\Expfordiffdataset\\service info\\";
            Calendar calendar=Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("MM.dd-HH.mm.ss");
            String exp_flage=formatter.format(calendar.getTime());

            File file_out=new File(out_dir_path+"resource_of_atomic_services."+exp_flage+".txt");
            if (!file_out.exists()){
                file_out.createNewFile();
            }
            FileWriter writer=new FileWriter(file_out,true);

            File root_file=new File(in_file_path);
            for(File dir_diff_dataset:root_file.listFiles()){
                System.out.println(dir_diff_dataset.getName());
                for (File dax_file:dir_diff_dataset.listFiles()){
                    String file_name=dax_file.getName();
                    if (file_name.split("\\.")[4].equals("dax") && file_name.split("\\.")[2].equals("1000")){
                        WorkflowInfo jobs=new WorkflowInfo(dax_file.getAbsolutePath());
                        Set<String> service_kind=jobs.getAllJobKindsSet();
                        for (String kind: service_kind){
                            writer.write(dir_diff_dataset.getName()+"::"+kind+"::"+getGaussian(cpu_ave,cpu_std)+
                                    "::"+getGaussian(mem_ave,mem_std)+"::"+getGaussian(disk_ave,disk_std)+"\n");
                        }
                        break;
                    }

                }
            }
            writer.flush();
            writer.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public static double getCpu_ave() {
        if(!init_flage){
            initParas();
        }
        return cpu_ave;
    }

    public static double getCpu_std() {
        if(!init_flage){
            initParas();
        }
        return cpu_std;
    }

    public static double getMem_ave() {
        if(!init_flage){
            initParas();
        }
        return mem_ave;
    }

    public static double getMem_std() {
        if(!init_flage){
            initParas();
        }
        return mem_std;
    }

    public static double getDisk_ave() {
        if(!init_flage){
            initParas();
        }
        return disk_ave;
    }

    public static double getDisk_std() {
        if(!init_flage){
            initParas();
        }
        return disk_std;
    }


}
