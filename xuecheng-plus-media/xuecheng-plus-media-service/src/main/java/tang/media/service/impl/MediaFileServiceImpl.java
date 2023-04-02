package tang.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tang.media.mapper.MediaProcessMapper;
import tang.media.model.dto.UploadFileParamsDto;
import tang.media.model.dto.UploadFileResultDto;
import tang.media.model.po.MediaProcess;
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
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

 @Autowired
 MediaProcessMapper mediaProcessMapper;
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
 @Override
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
   System.out.println("上传成功:"+objectName);
   return true;
  } catch (Exception e) {
   e.printStackTrace();
   log.error("上传文件到minio出错,bucket:{},objectName:{},错误原因:{}",bucket,objectName,e.getMessage(),e);
   XueChengPlusException.cast("上传文件到文件系统失败");
  }
  return false;
 }

 @Override
 public MediaFiles getFileById(String mediaId) {
  MediaFiles mediaFiles = mediaFilesMapper.selectById(mediaId);
  return mediaFiles;
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

 /**
  * 如果纯在objectName就用ObjectName,不存在则自己设置
  * @param companyId
  * @param uploadFileParamsDto 参数
  * @param localFilePath 文件本地路径
  * @param objectName
  * @return
  */

 @Override
// @Transactional //事务控制  minio可能存在网络延迟
 public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath,String objectName) {
  //先得到文件名
  String filename = uploadFileParamsDto.getFilename();
  String extension = filename.substring(filename.lastIndexOf("."));
  //将文件上传minio
  String mimeType = getMimeType(extension);
  String defaultFolderPath = getDefaultFolderPath();
  //获取文件的md5值
  String fileMd5 = getFileMd5(new File(localFilePath));
  if(StringUtils.isEmpty(objectName)) {
   //使用默认的年月日
   objectName = defaultFolderPath + fileMd5 + extension;
  }
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
   //记录待处理任务
   //如果是aiv视频写入待处理任务 也可以通过mimeType处理 video/x-msvideo
   this.addWaitingTask(mediaFiles);
  }
  //向MediaProcess插入任务
  return mediaFiles;

 }
 /**
  * 添加待处理任务
  * @param mediaFiles 媒资文件信息
  */
 private void addWaitingTask(MediaFiles mediaFiles) {
  //获取mimetype
  //文件名称
  String filename = mediaFiles.getFilename();
  String extension = filename.substring(filename.lastIndexOf("."));
  String mimeType = getMimeType(extension);
  if(mimeType.equals("video/x-msvideo")){
   //如果是avi视频写入待处理任务
   MediaProcess mediaProcess = new MediaProcess();
   BeanUtils.copyProperties(mediaFiles,mediaProcess);
   //状态 应该是未处理
   mediaProcess.setStatus("1");
   mediaProcess.setCreateDate(LocalDateTime.now());
   mediaProcess.setFailCount(0); //默认值为0
   mediaProcessMapper.insert(mediaProcess);
  }
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

 @Override
 public RestResponse<Boolean> checkchunk(String fileMd5, int chunk) {
  //分块的存储路径是:md5的前两位的两个目录,chunk存储分块文件
  //根据md5值得到分块文件所在路径
  String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
   GetObjectArgs testbucket = GetObjectArgs.builder()
           .bucket(bucket_video) //确定桶
           .object(chunkFileFolderPath+chunk)// 确定对象名 添加子目录
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
  //文件不存在
  System.out.println("文件不存在");
  return RestResponse.success(false);
 }

 @Override
 public RestResponse uploadchunk(String localChunkFilePath, String fileMd5, int chunk) {
  //获取文件扩展名
  String mimeType = getMimeType("");
  //获取文件的路径
  String chunkFileFolderPath = getChunkFileFolderPath(fileMd5)+chunk;
  //将分块文件上传minio
  boolean result = addMediaFilesToMinIO(localChunkFilePath, mimeType, bucket_video, chunkFileFolderPath);
  if(result){
  return RestResponse.success(result);
  }
  return RestResponse.success(result,"上传分块失败");
 }

 @Override
 public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
  //找到分块文件调用minio的sdk进行文件的合并
  //=====获取分块文件路径=====
  String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
//组成将分块文件路径组成 List<ComposeSource>
  List<ComposeSource> sourceObjectList = Stream.iterate(0, i -> ++i)
          .limit(chunkTotal)
          .map(i -> ComposeSource.builder()
                  .bucket(bucket_video)
                  .object(chunkFileFolderPath.concat(Integer.toString(i)))
                  .build())
          .collect(Collectors.toList());
//=====合并=====

  //获取源文件的名
  String filename = uploadFileParamsDto.getFilename();
  //扩展名
  String extension = filename.substring(filename.lastIndexOf("."));
  //获取合并后的ObjectName
  String objectName = getFilePathByMd5(fileMd5, extension);
  ComposeObjectArgs testbuckets = ComposeObjectArgs.builder().bucket(bucket_video)
          .object(objectName)
          .sources(sourceObjectList)  //指定源文件
          .build();
  //合并文件
  try {
   minioClient.composeObject(testbuckets);
  } catch (Exception e) {
   e.printStackTrace();
   log.error("bucket:{}, 合并文件出错:{}",bucket_video,objectName);
   return RestResponse.validfail(false,"合并文件异常");
  }
  //校验合并后的文件和源文件是否一致,一致才成功
  //先下载再校验
  File file = downloadFileFromMinIO(bucket_video, objectName);
  try {
   FileInputStream fileInputStream=new FileInputStream(file);
   //合并的md5值
   String mergeFile_md5 = DigestUtils.md5Hex(fileInputStream);
   if(!mergeFile_md5.equals(fileMd5)){
    log.error("校验合并md5值不一致,原始文件:{},合并文件:{}",fileMd5,mergeFile_md5);
    System.err.println("校验失败");
    return RestResponse.validfail(false,"文件校验失败");
   }
   //文件的大小
   uploadFileParamsDto.setFileSize(file.length());
  } catch (Exception e) {
   return RestResponse.validfail(false,"文件校验失败");
  }
  //将文件上传入库,统一管理
  MediaFiles mediaFiles = this.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_video, objectName);
  if(Objects.isNull(mediaFiles)){
   return RestResponse.validfail(false,"文件入库失败");
  }
  //清理文块文件
  clearChunkFiles(chunkFileFolderPath,chunkTotal);
  System.err.println("文件"+filename+"上传成功!");
  return RestResponse.success(true);
 }

 //得到分块文件的目录
 private String getChunkFileFolderPath(String fileMd5) {
  return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
 }

 /**
  * 得到合并后的文件的地址
  * @param fileMd5 文件id即md5值
  * @param fileExt 文件扩展名
  * @return
  */
 private String getFilePathByMd5(String fileMd5,String fileExt){
  return fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" +fileMd5 +fileExt;
 }

 /**
  * 从minio下载文件
  * @param bucket 桶
  * @param objectName 对象名称
  * @return 下载后的文件
  */
 @Override
 public File downloadFileFromMinIO(String bucket,String objectName){
//临时文件
  File minioFile = null;
  FileOutputStream outputStream = null;
  try{
   InputStream stream = minioClient.getObject(GetObjectArgs.builder()
           .bucket(bucket)
           .object(objectName)
           .build());
//创建临时文件
   minioFile=File.createTempFile("minio", ".merge");
   outputStream = new FileOutputStream(minioFile);
   IOUtils.copy(stream,outputStream);
   return minioFile;
  } catch (Exception e) {
   e.printStackTrace();
  }finally {
   if(outputStream!=null){
    try {
     outputStream.close();
    } catch (IOException e) {
     e.printStackTrace();
    }
   }
  }
  return null;
 }
 /**
  * 清除分块文件
  * @param chunkFileFolderPath 分块文件路径
  * @param chunkTotal 分块文件总数
  */
 private void clearChunkFiles(String chunkFileFolderPath,int chunkTotal){

  try {
   List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
           .limit(chunkTotal)
           //DeleteObject是直接new出来的,不是build出来的
           .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
           .collect(Collectors.toList());

   RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket(bucket_video).objects(deleteObjects).build();
   Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
   //没有真正的删除,需要遍历的一遍
   results.forEach(r->{
    DeleteError deleteError = null;
    try {
     deleteError = r.get();
    } catch (Exception e) {
     e.printStackTrace();
     log.error("清楚分块文件失败,objectname:{}",deleteError.objectName(),e);
    }
   });
  } catch (Exception e) {
   e.printStackTrace();
   log.error("清楚分块文件失败,chunkFileFolderPath:{}",chunkFileFolderPath,e);
  }
 }
}

