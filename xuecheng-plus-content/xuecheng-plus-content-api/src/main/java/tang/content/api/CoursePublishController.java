package tang.content.api;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import tang.content.dto.CoursePreviewDto;
import tang.content.po.CoursePublish;
import tang.content.service.CoursePublishService;
import tang.content.utils.SecurityUtil;

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
