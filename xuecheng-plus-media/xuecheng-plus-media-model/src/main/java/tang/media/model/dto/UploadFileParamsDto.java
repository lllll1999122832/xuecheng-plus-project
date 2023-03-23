package tang.media.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用来保存文件信息的
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadFileParamsDto {
    /**
     * 文件名称
     */
    private String filename;


    /**
     * 文件类型（文档，音频，视频）
     */
    private String fileType;
    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 标签
     */
    private String tags;

    /**
     * 上传人
     */
    private String username;

    /**
     * 备注
     */
    private String remark;

}
