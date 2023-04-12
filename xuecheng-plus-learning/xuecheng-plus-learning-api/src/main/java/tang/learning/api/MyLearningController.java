package tang.learning.api;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import tang.learning.service.LearningService;
import tang.learning.util.SecurityUtil;
import tang.xuechengplusbase.base.exception.XueChengPlusException;
import tang.xuechengplusbase.base.model.RestResponse;

import java.util.Objects;

/**
 * @author Mr.M
 * @version 1.0
 * @description 我的学习接口
 * date  2022/10/27 8:59
 */
@Api(value = "学习过程管理接口", tags = "学习过程管理接口")
@Slf4j
@RestController
public class MyLearningController {
    @Autowired
    LearningService learningService;
    @ApiOperation("获取视频")
    //媒资接口只管媒资
    @GetMapping("/open/learn/getvideo/{courseId}/{teachplanId}/{mediaId}")
    public RestResponse<String> getvideo(@PathVariable("courseId") Long courseId, @PathVariable("teachplanId") Long teachplanId, @PathVariable("mediaId") String mediaId) {
        //判断学习资格
//        String userId = SecurityUtil.getUser().getId();
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        if(Objects.isNull(user)){
              XueChengPlusException.cast("请先登录!");
        }
        String userId = user.getId();
        //有学习资格了之后,远程调用媒资服务来查询视频的播放地址
        return learningService.getvideo(userId,courseId,teachplanId,mediaId);
    }

}
