package tang.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import tang.content.dto.TeachplanDto;
import tang.content.po.Teachplan;

import java.util.List;

/**
* @author 曾梦想仗剑走天涯
* @description 针对表【teachplan(课程计划)】的数据库操作Mapper
* @createDate 2023-03-19 13:51:41
* @Entity .Teachplan
*/
@Repository
@Mapper
public interface TeachplanMapper extends BaseMapper<Teachplan> {
    public List<TeachplanDto> getTreeNodes(Long courseId);
}




