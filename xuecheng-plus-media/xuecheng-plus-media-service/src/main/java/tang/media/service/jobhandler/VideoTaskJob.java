package tang.media.service.jobhandler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tang.media.model.po.MediaProcess;
import tang.media.service.MediaFileProcessService;
import tang.media.service.MediaFileService;
import tang.xuechengplusbase.base.utils.Mp4VideoUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * 视频处理任务
 */
@Component
@Slf4j
public class VideoTaskJob {
    @Autowired
    MediaFileProcessService mediaFileProcessService;
    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegpath; //ffmpeg路径
    @Autowired
    MediaFileService mediaFileService;
    /**
     * 1、视频处理任务
     */
    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        //查询任务
        List<MediaProcess> mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, 5);

        //开启任务
        //确定CPU核心数
        int cpu = Runtime.getRuntime().availableProcessors();
        //算了,直接使用任务数
        int size = mediaProcessList.size();
        if(size<=0){
            log.debug("取不到任务!");
            return;
        }
        //使用计数器
        CountDownLatch countDownLatch=new CountDownLatch(size);
        //创建一个线程池
        ExecutorService executorService=new ThreadPoolExecutor(
                size,//核心类,刚开始工作线程数量
                cpu,//总共数量,全部加班之后的数量
                10,//除了核心线程,其他线程工作多久之后停
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(size),//待客区,最多有多少线程等待
                new ThreadPoolExecutor.DiscardOldestPolicy() // AbortPolicy拒绝之后抛出异常 | CallerRunsPolicy哪来的回哪个线程去
                //DiscardPolicy不抛出异常 || DiscardOldestPolicy队列满了尝试和最早的竞争,不会抛出异常
                );

        mediaProcessList.forEach(mediaProcess->{
            executorService.execute(()->{
                try {
                    //执行任务逻辑
                    //任务id
                    Long taskId = mediaProcess.getId();
                    //开启任务,执行视频转码
                    boolean start = mediaFileProcessService.startTask(taskId);
                    if (!start) {
                        log.debug("强占任务失败,任务Id:{}", taskId);
                        return; //为啥是return,不是continue;
                    }
                    //下载minio视频到本地
                    String bucket = mediaProcess.getBucket();
                    String filePath = mediaProcess.getFilePath();
                    File originalFile = mediaFileService.downloadFileFromMinIO(bucket, filePath);
                    //文件id就是md5值
                    String fileId = mediaProcess.getFileId();
                    if (Objects.isNull(originalFile)) {
                        log.debug("下载视频出错了,视频filePath:{}", filePath);
                        //保存失败处理的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "下载视频到本地失败");
                    }
                    //视频转码
                    //源avi视频的路径
                    String video_path = originalFile.getAbsolutePath();
                    //转换后mp4文件的名称
                    String mp4_name = fileId + ".mp4";
                    //转换后mp4文件的路径
                    //先创建一个临时文件,作为转换后的文件
                    File fileMp4 = null;
                    try {
                        fileMp4 = File.createTempFile("mp4", ".mp4");
                    } catch (IOException e) {
                        log.debug("创建临时文件异常,视频filePath:{}", filePath);
                        //保存失败处理的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "创建临时文件异常");
                        return;
                    }
                    String mp4_path = fileMp4.getAbsolutePath();
                    //创建工具类对象
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegpath, video_path, fileMp4.getName(), mp4_path);
                    //开始视频转换，成功将返回success
                    String result = videoUtil.generateMp4();
                    if (!result.equals("success")) {
                        log.debug("文件转码失败,失败原因:{},视频:{}", result, filePath);
                        //保存失败处理的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "文件转码失败");
                        return;
                    }
                    String objectName = getFilePath(fileId, ".mp4");
                    ////mp4文件的Url
                    String url = "/" + bucket + "/" + objectName;

                    //上传minio
                    boolean minioResult = mediaFileService.addMediaFilesToMinIO(fileMp4.getAbsolutePath(), "video/mp4", bucket, objectName);
                    if (!minioResult) {
                        log.debug("文件上传到minio失败,视频:{}", objectName);
                        //保存失败处理的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "文件上传到minio失败");
                        return;
                    }

                    //保存任务处理结果
                    mediaFileProcessService.saveProcessFinishStatus(taskId, "2", fileId, url, "成功执行");
                    log.debug("执行成功,objectName:{}",objectName);
                    //计算器减一
                }finally {
                    countDownLatch.countDown();
                }
            });
        });
        //阻塞,指定最大的阻塞时间
        countDownLatch.await(30,TimeUnit.MINUTES);
    }

    private String getFilePath(String fileMd5,String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }
        /**
         * 2、分片广播任务
         */
//    @XxlJob("shardingJobHandler")
//    public void shardingJobHandler() throws Exception {
//
//        // 分片参数
//        int shardIndex = XxlJobHelper.getShardIndex();
//        int shardTotal = XxlJobHelper.getShardTotal();
//
//        System.out.println("当前执行器号码"+shardIndex);
//        System.out.println("当前执行器总数"+shardTotal);
//    }




}
