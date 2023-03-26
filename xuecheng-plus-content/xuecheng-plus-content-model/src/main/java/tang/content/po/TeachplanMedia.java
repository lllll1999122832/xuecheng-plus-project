package tang.content.po;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @TableName teachplan_media
 */
@Data
@TableName("teachplan_media")
@AllArgsConstructor
@NoArgsConstructor
public class TeachplanMedia implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 媒资文件id
     */
    private String mediaId;

    /**
     * 课程计划标识
     */
    private Long teachplanId;

    /**
     * 课程标识
     */
    private Long courseId;

    /**
     * 媒资文件原始名称
     */
    private String mediaFilename;

    /**
     *
     */
//    @TableField(fill = FieldFill.INSERT)
    private Date createDate;

    /**
     * 创建人
     */
    private String createPeople;

    /**
     * 修改人
     */
    private String changePeople;

    private static final long serialVersionUID = 1L;
}
