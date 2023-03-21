package tang.content.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
/**
 * 更新课程
 */
public class EditCourseDto extends AddCourseDto{
    @ApiModelProperty(value = "课程id",required = true)
    private Long id;
}
