package tang.content.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tang.content.dto.*;
import tang.xuechengplusbase.base.exception.ValidationGroups;
import tang.xuechengplusbase.base.model.PageParams;
import tang.xuechengplusbase.base.model.PageResult;
import tang.content.po.CourseBase;
import tang.content.service.CourseBaseService;


@RestController
@Api(tags = "课程信息管理接口")
public class CourseBaseInfoController {
    @Autowired
    CourseBaseService courseBaseService;
    @PostMapping("/course/list")
    @ApiOperation("课程分页查询接口")
    public PageResult<CourseBase> list(PageParams pageParams, @RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto){
        return courseBaseService.queryCourseBaseList(pageParams,queryCourseParamsDto);
    }
    @ApiOperation("新增课程")
    @PostMapping("/course")
    public CourseBaseInfoDto createCourseBase(@RequestBody
                                                  @Validated(ValidationGroups.Insert.class) AddCourseDto addCourseDto){
      //  @Validate 激活校验可能
        //获取用户的所属的机构id
        // TODO: 2023/3/20 现在还没有认证,暂时设置一个随机
        Long companyId=1314520L;
        return courseBaseService.createCourseBase(companyId,addCourseDto);
    }
    @ApiOperation("根据id查询课程接口")
    @GetMapping("/course/{courseId}")
    public CourseBaseInfoDto getCourseById(@PathVariable("courseId")Long courseId){
        return courseBaseService.getCourseById(courseId);

    }
    @ApiOperation("修改课程信息接口")
    @PutMapping("/course")
    public CourseBaseInfoDto modifyCourse(@RequestBody
                                              @Validated({ValidationGroups.Update.class}) EditCourseDto editCourseDto){
        Long companyId=1314520L;
        return courseBaseService.modifyCourse(companyId,editCourseDto);
    }
}
