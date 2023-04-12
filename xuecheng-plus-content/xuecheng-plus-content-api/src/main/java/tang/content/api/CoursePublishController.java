package tang.content.api;

import com.alibaba.fastjson.JSON;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import tang.content.dto.CourseBaseInfoDto;
import tang.content.dto.CoursePreviewDto;
import tang.content.dto.TeachplanDto;
import tang.content.po.CoursePublish;
import tang.content.po.Teachplan;
import tang.content.service.CoursePublishService;
import tang.content.utils.SecurityUtil;

import java.util.List;
import java.util.Objects;

/**
 * 课程预览，发布
 */
@Controller
public class CoursePublishController {
    @Autowired
    CoursePublishService coursePublishService;
    @GetMapping("/coursepreview/{courseId}")
    public ModelAndView preview(@PathVariable("courseId") Long courseId){
        //指定模型
        ModelAndView modelAndView = new ModelAndView();
        //查询预览课程信息
        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(courseId);
        modelAndView.addObject("model",coursePreviewInfo);
        //指定模型
        modelAndView.setViewName("course_template"); //最终根据视图名称+.ftl找到模板
        return modelAndView;
    }
    @ApiOperation("获取课程发布信息")
    @ResponseBody
    @GetMapping("/course/whole/{courseId}")
    public CoursePreviewDto getCoursePublish(@PathVariable("courseId") Long courseId) {
        //查询课程发布表
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        CoursePublish coursePublish = coursePublishService.getCoursePublish(courseId);
        if(Objects.isNull(coursePublish)) {
            return null;
        }
        //向coursePreviewDto中填充数据
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(coursePublish,courseBaseInfoDto);
        String teachplanJson = coursePublish.getTeachplan();
        //转成List<teachplanDto>
        List<TeachplanDto> teachplanDtos = JSON.parseArray(teachplanJson, TeachplanDto.class);
        coursePreviewDto.setCourseBase(courseBaseInfoDto);
        coursePreviewDto.setTeachplans(teachplanDtos);
        return coursePreviewDto;
    }

        /**
         * 课程审核
         * @param courseId
         */
    @ResponseBody
    @PostMapping("/courseaudit/commit/{courseId}")
    public void commitAudit(@PathVariable("courseId")Long courseId){
        Long companyId= Long.valueOf(SecurityUtil.getUser().getCompanyId());
        coursePublishService.commitAudit(companyId,courseId);
    }
    @ApiOperation("课程发布")
    @ResponseBody
    @PostMapping ("/coursepublish/{courseId}")
    public void coursepublish(@PathVariable("courseId") Long courseId){
        Long companyId= Long.valueOf(SecurityUtil.getUser().getCompanyId());
        coursePublishService.coursepublish(companyId,courseId);
    }
    @ApiOperation("查询课程发布信息")
    @ResponseBody
    @GetMapping("/r/coursepublish/{courseId}")
    // /r将来在白名单中配置
    public CoursePublish getCoursepublish(@PathVariable("courseId") Long courseId) {
        CoursePublish coursePublish = coursePublishService.getCoursePublish(courseId);
        return coursePublish;
    }







}
