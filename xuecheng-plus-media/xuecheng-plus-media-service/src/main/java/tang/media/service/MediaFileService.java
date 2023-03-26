package tang.media.service;

import org.springframework.web.multipart.MultipartFile;
import tang.media.model.dto.UploadFileParamsDto;
import tang.media.model.dto.UploadFileResultDto;
import tang.xuechengplusbase.base.model.PageParams;
import tang.xuechengplusbase.base.model.PageResult;
import tang.media.model.dto.QueryMediaParamsDto;
import tang.media.model.po.MediaFiles;
import tang.xuechengplusbase.base.model.RestResponse;

import java.io.File;

/**
 * @description 媒资文件管理业务类
 * @author Mr.M
 * @date 2022/9/10 8:55
 * @version 1.0
 */
public interface MediaFileService {

 /**
  * @description 媒资文件查询方法
  * @param pageParams 分页参数
  * @param queryMediaParamsDto 查询条件
  * @return com.xuecheng.base.model.PageResult<com.xuecheng.media.model.po.MediaFiles>
  * @author Mr.M
  * @date 2022/9/10 8:57
 */
 public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);

    /**
     * 上传文件
     * @param companyId
     * @param uploadFileParamsDto 参数
     * @param localFilePath 文件本地路径
     * @return
     */

    UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath);

    //使他暴露出去,调用该对象为代理对象,这样可以使 @Transactional有效
    public MediaFiles addMediaFilesToDb(Long companyId,String fileMd5,UploadFileParamsDto uploadFileParamsDto,String bucket,String objectName);

    /**
     * 在数据库中查询是否该数据存在
     * @param fileMd5
     * @return
     */
    RestResponse<Boolean> checkfile(String fileMd5);

    /**
     * 检查分块
     * @param fileMd5
     * @param chunk
     * @return
     */
    RestResponse<Boolean> checkchunk(String fileMd5, int chunk);

   /**
    * 上传分块文件
    * @param fileMd5
    * @param chunk
    * @return
    */
    RestResponse uploadchunk(String localChunkFilePath, String fileMd5, int chunk);

    /**
     * @description 合并分块
     * @param companyId 机构id
     * @param fileMd5 文件md5
     * @param chunkTotal 分块总和
     * @param uploadFileParamsDto 文件信息
     * @return com.xuecheng.base.model.RestResponse
     * @author Mr.M
     * @date 2022/9/13 15:56
     */
    public RestResponse mergechunks(Long companyId,String fileMd5,int chunkTotal,UploadFileParamsDto uploadFileParamsDto);

    /**
     * 下载视频到本地
     * @param bucket
     * @param objectName
     * @return
     */
    public File downloadFileFromMinIO(String bucket, String objectName);

    /**
     * 文件上传至minio
     * @param localFilePath
     * @param mimeType
     * @param bucket
     * @param objectName
     * @return
     */
    public boolean addMediaFilesToMinIO(String localFilePath,String mimeType,String bucket, String objectName) ;

}
