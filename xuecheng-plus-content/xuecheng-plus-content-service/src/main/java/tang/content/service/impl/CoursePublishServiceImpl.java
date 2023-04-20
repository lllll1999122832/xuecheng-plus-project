package tang.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tang.content.config.MultipartSupportConfig;
import tang.content.dto.CourseBaseInfoDto;
import tang.content.dto.CoursePreviewDto;
import tang.content.dto.TeachplanDto;
import tang.content.feginclient.MediaServiceClient;
import tang.content.mapper.CourseBaseMapper;
import tang.content.mapper.CourseMarketMapper;
import tang.content.mapper.CoursePublishMapper;
import tang.content.mapper.CoursePublishPreMapper;
import tang.content.po.CourseBase;
import tang.content.po.CourseMarket;
import tang.content.po.CoursePublishPre;
import tang.content.service.CourseBaseService;
import tang.content.service.CourseMarketService;
import tang.content.service.CoursePublishService;
import tang.content.po.CoursePublish;
import tang.content.service.TeachplanService;
import tang.messagesdk.model.po.MqMessage;
import tang.messagesdk.service.MqMessageService;
import tang.xuechengplusbase.base.exception.CommonError;
import tang.xuechengplusbase.base.exception.XueChengPlusException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
* @author 曾梦想仗剑走天涯
 * 课程发布相关接口实现
* @description 针对表【course_publish(课程发布)】的数据库操作Service实现
* @createDate 2023-03-19 13:51:41
*/
@Service
@Slf4j
public class CoursePublishServiceImpl extends ServiceImpl<CoursePublishMapper, CoursePublish>
    implements CoursePublishService {
    @Autowired
    CourseBaseService courseBaseService;
    @Autowired
    TeachplanService teachplanService;
    @Autowired
    CourseMarketMapper courseMarketMapper;
    @Autowired
    CoursePublishPreMapper coursePublishPreMapper;
    @Autowired
    CourseBaseMapper courseBaseMapper;
    @Autowired
    CoursePublishMapper coursePublishMapper;
    @Autowired
    MqMessageService mqMessageService;
    @Autowired
    MediaServiceClient mediaServiceClient;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    RedissonClient redissonClient;
    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        //查询课程基本信息以及营销信息
        CourseBaseInfoDto courseById = courseBaseService.getCourseById(courseId);
        coursePreviewDto.setCourseBase(courseById);
        //课程计划信息
        List<TeachplanDto> treeNodes = teachplanService.getTreeNodes(courseId);
        coursePreviewDto.setTeachplans(treeNodes);
        return coursePreviewDto;
    }

    @Override
    @Transactional
    public void commitAudit(Long companyId, Long courseId) {
        //如果课程状态为已提交和不允许提交,则不需提交
        CourseBase courseById = courseBaseMapper.selectById(courseId);
        if(Objects.isNull(courseById)){
            throw new XueChengPlusException("课程找不到");
        }
        String auditStatus = courseById.getAuditStatus();
        //一旦对表进行修改,状态就会变化为未提交
        if(auditStatus.equals("202003")){
            throw new XueChengPlusException("课程已提交,请等待");
        }
        //todo 本机构只能提交本机构的课程
        //课程图片,计划信息没有填写则不需提交
        if(StringUtils.isEmpty(courseById.getPic())){
            throw new XueChengPlusException("请上传课程图片");
        }
        //查询课程计划
        List<TeachplanDto> treeNodes = teachplanService.getTreeNodes(courseId);
        if(Objects.isNull(treeNodes)){
            throw new XueChengPlusException("请编写课程计划");
        }
        //查询到课程的基本信息,营销信息,计划信息等,把他们插入课程预发布表
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        BeanUtils.copyProperties(courseById,coursePublishPre);
        //设置机构的Id
        coursePublishPre.setCompanyId(companyId);
        //课程基本信息加部分营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseService.getCourseById(courseId);
        BeanUtils.copyProperties(courseBaseInfo,coursePublishPre);
        //课程营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //转为json
        String courseMarketJson = JSON.toJSONString(courseMarket);
        //将课程营销信息json数据放入课程预发布表
        coursePublishPre.setMarket(courseMarketJson);

        //计划信息
        coursePublishPre.setTeachplan(JSON.toJSONString(treeNodes));
        //预发布表设置已提交
        coursePublishPre.setStatus("202003");
        //设置提交时间
//        coursePublishPre.setCreateDate(new Date());
        //查询预发布表如果有就更新,没有就插入
        CoursePublishPre coursePublishPreOld = coursePublishPreMapper.selectById(courseId);
        if(Objects.isNull(coursePublishPreOld)){
            coursePublishPreMapper.insert(coursePublishPre);
        }else {
            coursePublishPreMapper.updateById(coursePublishPre);
        }
        //更新课程基本信息表的状态为已提交
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setStatus("202003"); //审核状态为已提交
        courseBaseMapper.updateById(courseBase);
    }

    /**
     *
     * @param companyId
     * @param courseId
     */
    @Override
    @Transactional
    public void coursepublish(Long companyId, Long courseId) {
        //todo 本机构只能发布本机构的课程
        //查询预发布表查询数据
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        //拿到审核状态
        if(!coursePublishPre.getStatus().equals("202004")){
            throw new XueChengPlusException("课程审核未通过,不允许发布");
        }
        //向课程发布表写入数据
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre,coursePublish);
        //先课程发布表,假如没有的就插入,有就更新
        CoursePublish coursePublishOld = coursePublishMapper.selectById(courseId);
        if(Objects.isNull(coursePublishOld)){
            coursePublishMapper.insert(coursePublish);
        }else{
            coursePublishMapper.updateById(coursePublish);
        }
        //todo 向消息表写入数据
//        mqMessageService.addMessage()
        this.saveCoursePublishMessage(courseId);
        //把预发布表的数据删除
        coursePublishPreMapper.deleteById(courseId);
        //
    }

    @Override
    public File generateCourseHtml(Long courseId) {
        //定义返回的文件
        File file=null;
        //获取模板
        Configuration configuration = new Configuration(Configuration.getVersion());
        try{
//            //得到路径
//            String path = this.getClass().getResource("/").getPath();
//            //拿到模板的目录
//            configuration.setDirectoryForTemplateLoading(new File(path+"/templates/"));
            //上面那种是考磁盘路径,拿到文件,在linux中会报错
            //更改为如下方式
            //通过流的方式拿到模板文件
            configuration.setTemplateLoader(new ClassTemplateLoader(this.getClass().getClassLoader(),"/templates"));
            //指定编码
            configuration.setDefaultEncoding("utf-8");
            Template template = configuration.getTemplate("course_template.ftl");
            //准备数据
            CoursePreviewDto coursePreviewInfo = this.getCoursePreviewInfo(courseId);
            Map<String, Object> map=new HashMap<>();
            map.put("model",coursePreviewInfo);
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
            //将html写入文件
            //输入流
            InputStream inputStream = IOUtils.toInputStream(html, "utf-8");
            file=File.createTempFile("coursepublish",".html");
            //输出流
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            //使用流将html写入文件
            IOUtils.copy(inputStream, fileOutputStream);
        }catch (Exception e){
            e.printStackTrace();
            log.debug("转换为静态页面出错,courseId:{}",courseId.toString());
        }
        return file;
    }

    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        //将File转换为MultipleFile类型
//        File file = new File("E:\\WEB客户端设计\\120.html");
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        String upload = mediaServiceClient.upload(multipartFile, "course/" + courseId + ".html");
        if(StringUtils.isEmpty(upload)){
            log.debug("远程调用走的降级逻辑,课程id为:{}",courseId.toString());
            System.out.println("走的降级逻辑");
            XueChengPlusException.cast("上传静态页面过程存在异常");
        }
    }

    private void saveCoursePublishMessage(Long courseId){
        MqMessage coursePublish = mqMessageService.addMessage("course_publish", courseId.toString(), null, null);
        if (Objects.isNull(coursePublish)){
            throw new XueChengPlusException(CommonError.UNKOWN_ERROR.getErrMessage());
        }
    }
    public CoursePublish getCoursePublish(Long courseId){
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        return coursePublish ;
    }

//    @Override
//    public CoursePublish getCoursePublishCache(Long courseId) {
//        //查缓存
//        Object jsonObj = redisTemplate.opsForValue().get("course:" + courseId);
//        if(!Objects.isNull(jsonObj)){
//            if(jsonObj.equals("null")){
//                return null;
//            }
//            CoursePublish coursePublish = JSON.parseObject(jsonObj.toString(), CoursePublish.class);
//            return coursePublish;
//        }else {
//            synchronized (this){
//                jsonObj = redisTemplate.opsForValue().get("course:" + courseId);
//                //再次查一下缓存
//                if(Objects.isNull(jsonObj)) {
//                    if (jsonObj.equals("null")) {
//                        return null;
//                    }
//                    CoursePublish coursePublish = JSON.parseObject(jsonObj.toString(), CoursePublish.class);
//                    return coursePublish;
//                }
//                //设置缓存
//                CoursePublish coursePublish = getCoursePublish(courseId);
//                //存到redis中
////        if(!Objects.isNull(coursePublish)) {
//                redisTemplate.opsForValue().set("course:" + courseId, JSON.toJSONString(coursePublish),30+new Random().nextInt(90)+10, TimeUnit.MINUTES);
////        }
//                return coursePublish;
//            }
//        }
//    }

//    /**
//     * 使用redis的分布式锁来解决问题
//     * @param courseId
//     * @return
//     */
//    @Override
//    public CoursePublish getCoursePublishCache(Long courseId) {
//        //查缓存
//        Object jsonObj = redisTemplate.opsForValue().get("course:" + courseId);
//
//        if(!Objects.isNull(jsonObj)){
//            if(jsonObj.equals("null")){
//                return null;
//            }
//            CoursePublish coursePublish = JSON.parseObject(jsonObj.toString(), CoursePublish.class);
//            return coursePublish;
//        }else {
//            //掉用redis的setnx命令,谁执行成功,谁获取锁
//            Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("getCoursePublishCache", "lock", 30, TimeUnit.MINUTES);
//            if (ifAbsent){
//                //设置缓存
//                CoursePublish coursePublish = getCoursePublish(courseId);
//                //存到redis中
//    //        if(!Objects.isNull(coursePublish)) {
//                redisTemplate.opsForValue().set("course:" + courseId, JSON.toJSONString(coursePublish),30+new Random().nextInt(90)+10, TimeUnit.MINUTES);
//    //        }
//                return coursePublish;
//            }else{
//                //表示获取锁成功
//                jsonObj = redisTemplate.opsForValue().get("course:" + courseId);
//             while (Objects.isNull(jsonObj)){
//                 jsonObj = redisTemplate.opsForValue().get("course:" + courseId);
//             }
//                    if (jsonObj.equals("null")) {
//                        return null;
//                    }
//                    CoursePublish coursePublish = JSON.parseObject(jsonObj.toString(), CoursePublish.class);
//                    return coursePublish;
//                }
//            }
//        }
    /**
     * 使用redisson的分布式锁来解决问题
     * @param courseId
     * @return
     */
    @Override
    public CoursePublish getCoursePublishCache(Long courseId) {
        //查缓存
        Object jsonObj = redisTemplate.opsForValue().get("course:" + courseId);

        if(!Objects.isNull(jsonObj)){
            if(jsonObj.equals("null")){
                return null;
            }
            CoursePublish coursePublish = JSON.parseObject(jsonObj.toString(), CoursePublish.class);
            return coursePublish;
        }else {
            //
            RLock lock = redissonClient.getLock("coursequerylock:" + courseId);
            //获取分布式锁
            lock.lock();
           try {
//
                   //表示获取锁成功
                   jsonObj = redisTemplate.opsForValue().get("course:" + courseId);
                   if(!Objects.isNull(jsonObj)) {
                       if (jsonObj.equals("null")) {
                           return null;
                       } else {
                           CoursePublish coursePublish = JSON.parseObject(jsonObj.toString(), CoursePublish.class);
                           return coursePublish;
                       }
                   }
               //从数据库查询
               CoursePublish coursePublish = getCoursePublish(courseId);
               //查询完成再存储到redis
               redisTemplate.opsForValue().set("course:" + courseId, JSON.toJSONString(coursePublish), 300, TimeUnit.SECONDS);
               return coursePublish;
           }finally {
               //手动释放锁
               lock.unlock();
           }

        }
    }

}




