package tang.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import tang.content.po.CourseBase;

/**
* @author 曾梦想仗剑走天涯
* @description 针对表【course_base(课程基本信息)】的数据库操作Mapper
* @createDate 2023-03-19 13:51:41
* @Entity .CourseBase
*/

@Repository
@Mapper
public interface CourseBaseMapper extends BaseMapper<CourseBase> {

}




