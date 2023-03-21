package tang.content.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import tang.content.po.CourseCategory;

import java.io.Serializable;
import java.util.List;

/**
 * 因为相比CourseCategory只是少了一个children属性,所以可以采用 继承
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true) //设置set 时返回返回对象本身
public class CourseCategoryTreeDto extends CourseCategory implements Serializable {
    List<CourseCategoryTreeDto> childrenTreeNodes;
}
