package tang.learning.api;


import tang.learning.model.dto.MyCourseTableParams;
import tang.learning.model.dto.XcChooseCourseDto;
import tang.learning.model.dto.XcCourseTablesDto;
import tang.learning.model.po.XcCourseTables;
import tang.learning.service.MyCourseTablesService;
import tang.learning.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tang.xuechengplusbase.base.exception.XueChengPlusException;
import tang.xuechengplusbase.base.model.PageResult;

import java.util.Objects;

/**
 * @author Mr.M
 * @version 1.0
 * @description 我的课程表接口
 * @date 2022/10/25 9:40
 */

@Api(value = "我的课程表接口", tags = "我的课程表接口")
@Slf4j
@RestController
public class MyCourseTablesController {
    @Autowired
    MyCourseTablesService myCourseTablesService;


    @ApiOperation("添加选课")
    @PostMapping("/choosecourse/{courseId}")
    public XcChooseCourseDto addChooseCourse(@PathVariable("courseId") Long courseId) {
        //拿到当前用户id
        Long userId = Long.valueOf(SecurityUtil.getUser().getId());
        if(Objects.isNull(userId)){
            throw new XueChengPlusException("请先登录!");
        }
        //todo 添加选课
        return myCourseTablesService.addChooseCourse(String.valueOf(userId),courseId);
    }

    @ApiOperation("查询学习资格")
    @PostMapping("/choosecourse/learnstatus/{courseId}")
    public XcCourseTablesDto getLearnstatus(@PathVariable("courseId") Long courseId) {
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        if(Objects.isNull(user)){
            throw new XueChengPlusException("请先登录!");
        }
        String id = user.getId();
        XcCourseTablesDto learningStatus = myCourseTablesService.getLearningStatus(id, courseId);
        return learningStatus;
    }

    @ApiOperation("我的课程表")
    @GetMapping("/mycoursetable")
    public PageResult<XcCourseTables> mycoursetable(MyCourseTableParams params) {
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        if(Objects.isNull(user)){
            throw new XueChengPlusException("请先登录!");
        }
        params.setUserId(user.getId());
        return myCourseTablesService.mycoursestabls(params);
    }

}
