package tang.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import tang.content.po.CourseMarket;

/**
* @author 曾梦想仗剑走天涯
* @description 针对表【course_market(课程营销信息)】的数据库操作Mapper
* @createDate 2023-03-19 13:51:41
* @Entity .CourseMarket
*/
@Mapper
@Repository
public interface CourseMarketMapper extends BaseMapper<CourseMarket> {

}




