package tang.content.dto;

import lombok.Data;
import tang.content.po.Teachplan;
import tang.content.po.TeachplanMedia;

import java.util.List;

@Data
public class TeachplanDto extends Teachplan {
    //小章节list
    private List<TeachplanDto> teachPlanTreeNodes;
    //与媒资信息关联的信息
    private TeachplanMedia teachplanMedia;
}
