package tang.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import tang.content.po.TeachplanMedia;

/**
* @author 曾梦想仗剑走天涯
* @description 针对表【teachplan_media】的数据库操作Mapper
* @createDate 2023-03-19 13:51:41
* @Entity .TeachplanMedia
*/
@Mapper
@Repository
public interface TeachplanMediaMapper extends BaseMapper<TeachplanMedia> {

}




