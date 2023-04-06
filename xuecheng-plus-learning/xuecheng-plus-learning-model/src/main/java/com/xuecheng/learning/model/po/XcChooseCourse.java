package com.xuecheng.learning.model.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author itcast
 */
@Data
@TableName("xc_choose_course")
public class XcChooseCourse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 课程id
     */
    private Long courseId;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 机构id
     */
    private Long companyId;

    /**
     * 选课类型
     */
    private String orderType;

    /**
     * 添加时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createDate;

    /**
     * 课程有效期(天)
     */
    private Integer validDays;

    private Float coursePrice;

    /**
     * 选课状态
     */
    private String status;

    /**
     * 开始服务时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validtimeStart;

    /**
     * 结束服务时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validtimeEnd;

    /**
     * 备注
     */
    private String remarks;


}
