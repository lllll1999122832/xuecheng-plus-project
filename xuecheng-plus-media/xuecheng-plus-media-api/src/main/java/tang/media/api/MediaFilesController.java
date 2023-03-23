package tang.media.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import tang.media.model.dto.UploadFileResultDto;
import tang.xuechengplusbase.base.model.PageParams;
import tang.xuechengplusbase.base.model.PageResult;
import tang.media.model.dto.QueryMediaParamsDto;
import tang.media.model.po.MediaFiles;
import tang.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description 媒资文件管理接口
 * @author Mr.M
 * @date 2022/9/6 11:29
 * @version 1.0
 */
 @Api(value = "媒资文件管理接口",tags = "媒资文件管理接口")
 @RestController
public class MediaFilesController {


  @Autowired
  MediaFileService mediaFileService;


 @ApiOperation("媒资列表查询接口")
 @PostMapping("/files")
 public PageResult<MediaFiles> list(PageParams pageParams, @RequestBody QueryMediaParamsDto queryMediaParamsDto){
  Long companyId = 1232141425L;
  return mediaFileService.queryMediaFiels(companyId,pageParams,queryMediaParamsDto);

 }
    @ApiOperation("上传图片")
    @PostMapping(value = "/upload/coursefile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    //说明上传类型为复杂类型
    public UploadFileResultDto upload(@RequestPart("filedata")MultipartFile filedata){
     //已经接受到文件了
     //@RequestPart 上传的文件参数名
        //调用mediaFileService方法
        Long companyId = 1232141425L;
//        return mediaFileService.uploadFile(companyId,filedata,null);
        return null;
    }

}
