package tang.xuechengplusbase.base.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页查询参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageParams {
    //当前页码
    @ApiModelProperty(value = "页码")
    private Long pageNo=1L;
    //页面大小
    @ApiModelProperty(value = "每页记录数")
    private Long pageSize=30L;
}
