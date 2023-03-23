package tang.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import tang.content.dto.SaveTeachplanDto;
import tang.content.dto.TeachplanDto;
import tang.content.po.Teachplan;

import java.util.List;

/**
* @author 曾梦想仗剑走天涯
* @description 针对表【teachplan(课程计划)】的数据库操作Service
* @createDate 2023-03-19 13:51:41
*/
public interface TeachplanService extends IService<Teachplan> {

    List<TeachplanDto> getTreeNodes(Long courseId);

    void saveTeachPlan(SaveTeachplanDto saveTeachplanDto);
}
