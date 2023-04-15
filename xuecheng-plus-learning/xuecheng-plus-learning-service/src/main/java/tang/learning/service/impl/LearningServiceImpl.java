package tang.learning.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tang.learning.feignclient.ContentServiceClient;
import tang.learning.feignclient.CoursePublish;
import tang.learning.feignclient.MediaServiceClient;
import tang.learning.model.dto.XcCourseTablesDto;
import tang.learning.service.LearningService;
import tang.learning.service.MyCourseTablesService;
import tang.xuechengplusbase.base.exception.XueChengPlusException;
import tang.xuechengplusbase.base.model.RestResponse;
import tang.xuechengplusbase.base.utils.StringUtil;

import java.util.Objects;

@Service
@Slf4j
public class LearningServiceImpl implements LearningService {
    @Autowired
    MyCourseTablesService myCourseTablesService;
    @Autowired
    ContentServiceClient contentServiceClient;
    @Autowired
    MediaServiceClient mediaServiceClient;

    @Override
    public RestResponse<String> getvideo(String userId, Long courseId, Long teachplanId, String mediaId) {
        //获取学习资格
        //用户登录
        //查询课程信息
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        //如果课程不存在就抛出异常
        if(Objects.isNull(coursepublish)){
            return  RestResponse.validfail("课程不存在!");
        }
        //根据课程id去远程调用课程接口去查询课程计划信息,如果is_preview的值是1表示支持试学
        //todo 也可以从coursepublish对象中解析出课程计划判断只否支持试学
        if(StringUtil.isNotEmpty(userId)){
            XcCourseTablesDto xcCourseTablesDto = myCourseTablesService.getLearningStatus(userId, courseId);
            //[{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
            String learnStatus = xcCourseTablesDto.getLearnStatus();
            if(learnStatus.equals("702002")){
                return RestResponse.validfail("无法学习,没有选课或选课后没有支付");
            }else if(learnStatus.equals("702003")){
                return RestResponse.validfail("无法学习,已过期需要申请续期或重新支付");
            }else{
                //有资格学习,返回视频的播放地址
                //todo 调用媒资服务获取视频服务的地址
                RestResponse<String> playUrlByMediaId = mediaServiceClient.getPlayUrlByMediaId(mediaId);
                return playUrlByMediaId;
            }
        }
        //没有登录的时候
        //查询课程收费规则
        String charge = coursepublish.getCharge();
        if(charge.equals("201000")){
            //免费学习,有资格学习
            //todo 返回视频播放地址
            RestResponse<String> playUrlByMediaId = mediaServiceClient.getPlayUrlByMediaId(mediaId);
            return playUrlByMediaId;
        }
        return RestResponse.validfail("该课程没有选课!");
    }
}
