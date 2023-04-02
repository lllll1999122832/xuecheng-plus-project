package tang.content.service.jobhandler;


import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tang.content.feginclient.CourseIndex;
import tang.content.feginclient.SearchServiceClient;
import tang.content.mapper.CoursePublishMapper;
import tang.content.po.CoursePublish;
import tang.content.service.CoursePublishService;
import tang.messagesdk.model.po.MqMessage;
import tang.messagesdk.service.MessageProcessAbstract;
import tang.messagesdk.service.MqMessageService;
import tang.xuechengplusbase.base.exception.XueChengPlusException;

import java.io.File;
import java.util.Objects;

@Component
@Slf4j
public class CoursePublishTask extends MessageProcessAbstract {
    @Autowired
    CoursePublishService coursePublishService;
    @Autowired
    SearchServiceClient searchServiceClient;
    @Autowired
    CoursePublishMapper coursePublishMapper;
    /**
     * 任务调度入口
     */
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler(){
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        //调用抽象类执行方法
        process(shardIndex,shardTotal,"course_publish",30,60);
    }

    /**
     * 执行课程任务发布逻辑
     *
     * @param mqMessage 执行任务内容
     * @return
     */
    @Override
    public boolean execute(MqMessage mqMessage) {
        //向mqMessage拿到课程id
        Long courseId = Long.parseLong(mqMessage.getBusinessKey1());
        //向minio写静态页面
        this.generateCourseHtml(mqMessage,courseId);
        //向elasticsearch写索引
        this.saveCourseIndex(mqMessage,courseId);
        //向redis写缓存
        this.saveCourseCache(mqMessage,courseId);

        return true;
    }

    //生成课程静态化页面并上传至文件系统
    public void generateCourseHtml(MqMessage mqMessage, long courseId) {
        MqMessageService mqMessageService = this.getMqMessageService();
        //任务幂等性的处理
        int stageOne = mqMessageService.getStageOne(mqMessage.getId());
        if(stageOne>0){
            log.debug("课程静态化处理完成...无序处理");
            return;
        }
        //生成静态化页面
        File file =coursePublishService.generateCourseHtml(courseId);
        //上传minio
        if(Objects.isNull(file)){
            throw new XueChengPlusException("生成的静态页面为空!");
        }
        coursePublishService.uploadCourseHtml(courseId,file);
        //任务完成改变任务状态
        mqMessageService.completedStageOne(mqMessage.getId());
    }
    //向es中写任务的索引
    public void saveCourseIndex(MqMessage mqMessage, long courseId) {
        MqMessageService mqMessageService = this.getMqMessageService();
        //任务幂等性的处理
        int stageTwo = mqMessageService.getStageTwo(mqMessage.getId());
        if(stageTwo>0){
            log.debug("课程任务的索引编写完成...无序处理");
            return;
        }
        //查询课程信息,添加索引服务
        //查询课程信息,调用搜索服务添加索引接口
        //从课程发布表查询信息
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        CourseIndex courseIndex=new CourseIndex();
        BeanUtils.copyProperties(coursePublish,courseIndex);
        Boolean add = searchServiceClient.add(courseIndex);
        if(!add){
            XueChengPlusException.cast("远程调用搜索服务,索引添加失败!");
        }
        //任务完成改变任务状态
        mqMessageService.completedStageTwo(mqMessage.getId());
    }
    //向redis中写缓存
    public void saveCourseCache(MqMessage mqMessage, long courseId) {
        MqMessageService mqMessageService = this.getMqMessageService();
        //任务幂等性的处理
        int stageThree = mqMessageService.getStageThree(mqMessage.getId());
        if(stageThree>0){
            log.debug("课程任务的索引编写完成...无序处理");
            return;
        }
        //取该任务的状态
        //任务完成改变任务状态
        mqMessageService.completedStageThree(mqMessage.getId());
    }
}
