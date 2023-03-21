package tang.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import tang.content.dto.CourseCategoryTreeDto;
import tang.content.po.CourseCategory;

import java.util.List;

/**
* @author 曾梦想仗剑走天涯
* @description 针对表【course_category(课程分类)】的数据库操作Mapper
* @createDate 2023-03-19 13:51:41
* @Entity .CourseCategory
*/
@Mapper
@Repository
public interface CourseCategoryMapper extends BaseMapper<CourseCategory> {
    /**
     * 使用递归查询我们的分类
     */
     public List<CourseCategoryTreeDto> selectTreeNodes( String id);
}




