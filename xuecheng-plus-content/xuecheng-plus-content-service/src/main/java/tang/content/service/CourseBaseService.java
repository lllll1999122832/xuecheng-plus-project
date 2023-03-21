package tang.content.service;


import com.baomidou.mybatisplus.extension.service.IService;
import tang.content.dto.AddCourseDto;
import tang.content.dto.CourseBaseInfoDto;
import tang.content.dto.EditCourseDto;
import tang.content.dto.QueryCourseParamsDto;
import tang.content.po.CourseBase;
import tang.xuechengplusbase.base.model.PageParams;
import tang.xuechengplusbase.base.model.PageResult;

/**
* @author 曾梦想仗剑走天涯
* @description 针对表【course_base(课程基本信息)】的数据库操作Service
* @createDate 2023-03-19 13:51:41
*/
public interface CourseBaseService extends IService<CourseBase> {

    PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto);

    CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto);

    CourseBaseInfoDto getCourseById(Long courseId);

    CourseBaseInfoDto modifyCourse(Long companyId,EditCourseDto editCourseDto);
}
