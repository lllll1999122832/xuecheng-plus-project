package tang.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tang.media.model.dto.UploadFileParamsDto;
import tang.media.model.dto.UploadFileResultDto;
import tang.xuechengplusbase.base.exception.XueChengPlusException;
import tang.xuechengplusbase.base.model.PageParams;
import tang.xuechengplusbase.base.model.PageResult;
import tang.media.mapper.MediaFilesMapper;
import tang.media.model.dto.QueryMediaParamsDto;
import tang.media.model.po.MediaFiles;
import tang.media.service.MediaFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/9/10 8:58
 * @version 1.0
 */
 @Service
 @Slf4j
public class MediaFileServiceImpl implements MediaFileService {

  @Autowired
 MediaFilesMapper mediaFilesMapper;
  @Autowired
 MinioClient minioClient;
  //文件
 @Value("${minio.bucket.files}")
 private String bucket_Files;
 //图片
 @Value("${minio.bucket.videofiles}")
 private String bucket_video;
 @Override
 public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

  //构建查询条件对象
  LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

  //分页对象
  Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
  // 查询数据内容获得结果
  Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
  // 获取数据列表
  List<MediaFiles> list = pageResult.getRecords();
  // 获取数据总数
  long total = pageResult.getTotal();
  // 构建结果集
  PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
  return mediaListResult;

 }

 /**
  * 写一个功能类,根据扩展名获取MimeType
  * @param extension
  * @return
  */

 private String getMimeType(String extension){
  if(!StringUtils.hasText(extension)){
   extension=" ";
  }
  //根据扩展名取出mimeType
  ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
  String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
  if(!Objects.isNull(extensionMatch)){
   mimeType=extensionMatch.getMimeType();
  }
  return mimeType;
 }

 //将文件上传到minio
 /**
  * @description 将文件写入minIO
  * @param localFilePath 文件地址
  * @param bucket 桶
  * @param objectName 对象名称
  * @return void
  * @author Mr.M
  * @date 2022/10/12 21:22
  */
 public boolean addMediaFilesToMinIO(String localFilePath,String mimeType,String bucket, String objectName) {
  try {
   UploadObjectArgs testbucket = UploadObjectArgs.builder()
           .bucket(bucket)
           .object(objectName)
           .filename(localFilePath)
           .contentType(mimeType)
           .build();
   minioClient.uploadObject(testbucket);
   log.debug("上传文件到minio成功,bucket:{},objectName:{}",bucket,objectName);
   System.out.println("上传成功");
   return true;
  } catch (Exception e) {
   e.printStackTrace();
   log.error("上传文件到minio出错,bucket:{},objectName:{},错误原因:{}",bucket,objectName,e.getMessage(),e);
   XueChengPlusException.cast("上传文件到文件系统失败");
  }
  return false;
 }
 //获取文件默认存储目录路径 年/月/日
 private String getDefaultFolderPath() {
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  String folder = sdf.format(new Date()).replace("-", "/")+"/";
  return folder;
 }
 //获取文件的md5
 private String getFileMd5(File file) {
  try (FileInputStream fileInputStream = new FileInputStream(file)) {
   String fileMd5 = DigestUtils.md5Hex(fileInputStream);
   return fileMd5;
  } catch (Exception e) {
   e.printStackTrace();
   return null;
  }
 }

 @Override
 public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {
  //先得到文件名
  String filename = uploadFileParamsDto.getFilename();
  String extension = filename.substring(filename.lastIndexOf("."));
  //将文件上传minio
  String mimeType = getMimeType(extension);
  String defaultFolderPath = getDefaultFolderPath();
  //获取文件的md5值
  String fileMd5 = getFileMd5(new File(localFilePath));
  String objectName=defaultFolderPath+fileMd5+extension;
  addMediaFilesToMinIO(localFilePath,mimeType,bucket_Files,objectName);
  //将文件信息保存数据库


  return null;
 }


}
