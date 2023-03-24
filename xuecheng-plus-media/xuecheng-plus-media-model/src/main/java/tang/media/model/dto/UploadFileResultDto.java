package tang.media.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tang.media.model.po.MediaFiles;

/**
 * 文件上传成功之后传递的实体类
 */
@Data
//@AllArgsConstructor
//@NoArgsConstructor
public class UploadFileResultDto extends MediaFiles {
}
