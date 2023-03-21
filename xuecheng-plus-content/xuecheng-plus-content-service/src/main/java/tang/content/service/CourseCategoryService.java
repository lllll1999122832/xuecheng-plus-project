package tang.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import tang.content.dto.CourseCategoryTreeDto;
import tang.content.po.CourseCategory;

import java.util.List;

/**
* @author 曾梦想仗剑走天涯
* @description 针对表【course_category(课程分类)】的数据库操作Service
* @createDate 2023-03-19 13:51:41
*/
public interface CourseCategoryService extends IService<CourseCategory> {

    List<CourseCategoryTreeDto> queryTreeNodes(String id);
}
