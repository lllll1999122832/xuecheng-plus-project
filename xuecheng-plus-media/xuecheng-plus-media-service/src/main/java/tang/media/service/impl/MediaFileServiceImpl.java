package tang.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
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
import tang.xuechengplusbase.base.model.RestResponse;
import tang.xuechengplusbase.base.utils.BeanCopyUtils;

import javax.management.ObjectName;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
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
// @Transactional //事务控制  minio可能存在网络延迟
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
  //保存进入minio
  boolean result = addMediaFilesToMinIO(localFilePath, mimeType, bucket_Files, objectName);
  //将文件信息保存数据库
  if (!result){
   throw new XueChengPlusException("保存失败!");
  }
  //加this 保证他是代理对象 这样就能够进行事务处理
  //执行事务必须是代理对象
  MediaFiles mediaFiles = this.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_Files, objectName);
  //准备返回的对象
  if(Objects.isNull(mediaFiles)){
   throw new XueChengPlusException("文件上传后,保存信息失败!");
  }
  UploadFileResultDto uploadFileResultDto=new UploadFileResultDto();
  BeanUtils.copyProperties(mediaFiles,uploadFileResultDto);
  return uploadFileResultDto;
 }

 /**
  * @description 将文件信息添加到文件表
  * @param companyId 机构id
  * @param fileMd5 文件md5值
  * @param uploadFileParamsDto 上传文件的信息
  * @param bucket 桶
  * @param objectName 对象名称
  * @return com.xuecheng.media.model.po.MediaFiles
  * @author Mr.M
  * @date 2022/10/12 21:22
  */
 @Transactional
 public MediaFiles addMediaFilesToDb(Long companyId,String fileMd5,UploadFileParamsDto uploadFileParamsDto,String bucket,String objectName){
  //先判断文件是否存在,通过主键来查询,主键是Md5值
  MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
  if(Objects.isNull(mediaFiles)){
   mediaFiles = BeanCopyUtils.copyBean(uploadFileParamsDto, MediaFiles.class);
   //设置id
   mediaFiles.setId(fileMd5);
   //文件id
   mediaFiles.setFileId(fileMd5);
   //机构的id
   mediaFiles.setCompanyId(companyId);
   //桶
   mediaFiles.setBucket(bucket);
   //file_path
   mediaFiles.setFilePath(objectName);
   //file_id
   mediaFiles.setFileId(fileMd5);
   //url
   mediaFiles.setUrl("/"+bucket+"/"+objectName);
   //上传时间
   mediaFiles.setCreateDate(LocalDateTime.now());
   //状态
   mediaFiles.setStatus("1"); //1正常 2不展示
   //审核状态
   mediaFiles.setAuditStatus("002003");
   //插入数据库
   int insert = mediaFilesMapper.insert(mediaFiles);
   if(insert<=0){
    log.error("向数据库保存文件失败,bucket:{},objectName:{}",bucket, objectName);
    return null;
   }
  }
  return mediaFiles;

 }

 @Override
 public RestResponse<Boolean> checkfile(String fileMd5) {
  MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
  if(!Objects.isNull(mediaFiles)){
   GetObjectArgs testbucket = GetObjectArgs.builder()
           .bucket(mediaFiles.getBucket()) //确定桶
           .object(mediaFiles.getFilePath())// 确定对象名 添加子目录
           .build();
   //上传文件
   //查询远程的流
   try {
    FilterInputStream object = minioClient.getObject(testbucket);
    if(!Objects.isNull(object)){
     return RestResponse.success(true);
    }
   } catch (Exception e) {
    e.printStackTrace();
   }
  }
  //文件不存在
  return RestResponse.success(false);
 }


}
