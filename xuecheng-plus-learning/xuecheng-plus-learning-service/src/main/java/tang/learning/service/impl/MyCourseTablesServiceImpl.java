package tang.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import tang.learning.feignclient.ContentServiceClient;
import tang.learning.feignclient.CoursePublish;
import tang.learning.mapper.XcChooseCourseMapper;
import tang.learning.mapper.XcCourseTablesMapper;
import tang.learning.model.dto.MyCourseTableParams;
import tang.learning.model.dto.XcChooseCourseDto;
import tang.learning.model.dto.XcCourseTablesDto;
import tang.learning.model.po.XcChooseCourse;
import tang.learning.model.po.XcCourseTables;
import tang.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tang.xuechengplusbase.base.exception.XueChengPlusException;
import tang.xuechengplusbase.base.model.PageResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class MyCourseTablesServiceImpl implements MyCourseTablesService {
    @Autowired
    XcChooseCourseMapper xcChooseCourseMapper;
    @Autowired
    XcCourseTablesMapper xcCourseTablesMapper;
    @Autowired
    ContentServiceClient contentServiceClient;

    @Override
    @Transactional
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        XcChooseCourseDto chooseCourseDto=new XcChooseCourseDto();
        //远程调用内容管理服务 查询课程的收费规则
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (Objects.isNull(coursepublish)) {
            throw new XueChengPlusException("课程不存在!");
        }
        XcChooseCourse xcChooseCourse;
        String charge = coursepublish.getCharge(); //收费规则
        if (charge.equals("201000")) {
            //如果是免费课程,会向选课记录表和我的课程表写数据
            xcChooseCourse= addFreeCoruse(userId, coursepublish);
            //向我的课表写
            XcCourseTables xcCourseTables = addCourseTabls(xcChooseCourse);
        } else {
            //如果是收费课程,会向选课记录表写数据
            xcChooseCourse = addChargeCoruse(userId, coursepublish);
        }
        //查询学生的学习资格
        XcCourseTablesDto learningStatus = getLearningStatus(userId, courseId);
        BeanUtils.copyProperties(xcChooseCourse,chooseCourseDto);
        //设置学习资格状态
        chooseCourseDto.setLearnStatus(learningStatus.getLearnStatus());
        return chooseCourseDto;
    }

    //[{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        //查我的课程表,如果查不到说明没有选课
        XcCourseTables xcCourseTables = getXcCourseTables(userId, courseId);
        XcCourseTablesDto xcCourseTablesDto=new XcCourseTablesDto();
        if(Objects.isNull(xcCourseTables)){
            //"code":"702002","desc":"没有选课或选课后没有支付"
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }
        //如果查到了,检查是否过期
        boolean before = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if(before){
            xcCourseTablesDto.setLearnStatus("702003"); //过期
            return xcCourseTablesDto;
        }
        xcCourseTablesDto.setLearnStatus("702001");
        return xcCourseTablesDto;
    }

    @Override
    @Transactional
    public boolean saveChooseCourseSuccess(String chooseCourseId) {
        //查询选课表,选课id是否存在
        XcChooseCourse xcChooseCourse = xcChooseCourseMapper.selectById(chooseCourseId);
        if(Objects.isNull(xcChooseCourse)){
            log.debug("接受到购买课程的消息,根据选课的id,找不到选课记录:{}",chooseCourseId);
            return false;
        }
        //选课状态
        String status = xcChooseCourse.getStatus();
        //只有未支付时,才更新为已支付
        if (status.equals("701002")){
            //更新选课记录为已经支付
            xcChooseCourse.setStatus("701002");
            int result = xcChooseCourseMapper.updateById(xcChooseCourse);
            if(result<=0){
                log.debug("添加选课记录失败:{}",xcChooseCourse);
                throw new XueChengPlusException("添加选课记录失败!");
            }
            //向我的课程表插入记录
            XcCourseTables xcCourseTables = addCourseTabls(xcChooseCourse);
        }
        return true;
    }

    @Override
    public PageResult<XcCourseTables> mycoursestabls(MyCourseTableParams params) {
        LambdaQueryWrapper<XcCourseTables>lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(!Objects.isNull(params.getUserId()),XcCourseTables::getUserId,params.getUserId());
//                .eq(!Objects.isNull(params.getCourseType()),XcCourseTables::getCourseType,params.getCourseType());
        //页码
        int pageNum= params.getPage();
        int pageSize=params.getSize();
        Page<XcCourseTables> page = new Page<>(pageNum, pageSize);
        Page<XcCourseTables> result = xcCourseTablesMapper.selectPage(page, lambdaQueryWrapper);
        //数据列表
        List<XcCourseTables> records = result.getRecords();
        return new PageResult<>(records,result.getTotal(),pageNum,pageSize);
    }

    //添加免费课程,免费课程加入选课记录表、我的课程表
    public XcChooseCourse addFreeCoruse(String userId, CoursePublish coursepublish) {
        Long coursepublishId = coursepublish.getId();
        //假如存在免费的该选课记录且状态为成功 ,那直接返回
        LambdaQueryWrapper<XcChooseCourse>lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(XcChooseCourse::getUserId,userId)
        .eq(XcChooseCourse::getCourseId,coursepublishId)
                .eq(XcChooseCourse::getOrderType,"700001") //免费课程
                .eq(XcChooseCourse::getStatus,"701001"); //选课成功
        List<XcChooseCourse> xcChooseCourseList = xcChooseCourseMapper.selectList(lambdaQueryWrapper);
        if(xcChooseCourseList.size()>0){
            return xcChooseCourseList.get(0);
        }
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublishId);
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700001");
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setValidDays(365);
        xcChooseCourse.setStatus("701001"); //选课成功
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));
        int result = xcChooseCourseMapper.insert(xcChooseCourse);
        if(result<=0){
            throw new XueChengPlusException("课程添加记录失败");
        }
        return xcChooseCourse;
    }

    //添加收费课程
    public XcChooseCourse addChargeCoruse(String userId, CoursePublish coursepublish) {
        Long coursepublishId = coursepublish.getId();
        //假如存在收费的该选课记录且状态为待支付,那直接返回
        LambdaQueryWrapper<XcChooseCourse>lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(XcChooseCourse::getUserId,userId)
                .eq(XcChooseCourse::getCourseId,coursepublishId)
                .eq(XcChooseCourse::getOrderType,"700002") //收费课程
                .eq(XcChooseCourse::getStatus,"701002"); //待支付
        List<XcChooseCourse> xcChooseCourseList = xcChooseCourseMapper.selectList(lambdaQueryWrapper);
        if(xcChooseCourseList.size()>0){
            return xcChooseCourseList.get(0);
        }
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublishId);
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700002");//收费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setValidDays(365);
        xcChooseCourse.setStatus("701002"); //待支付
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));
        int result = xcChooseCourseMapper.insert(xcChooseCourse);
        if(result<=0){
            throw new XueChengPlusException("课程添加记录失败");
        }
        return xcChooseCourse;
    }

    //添加到我的课程表
    public XcCourseTables addCourseTabls(XcChooseCourse xcChooseCourse) {
        //只有选课成功了才向我的课程记录添加
        String status = xcChooseCourse.getStatus();
        if(!status.equals("701001")){
            throw new XueChengPlusException("选课不成功,无法添加到课程表");
        }
        XcCourseTables xcCourseTables = getXcCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if(!Objects.isNull(xcCourseTables)){
            return xcCourseTables;
        }
        xcCourseTables=new XcCourseTables();
        BeanUtils.copyProperties(xcChooseCourse,xcCourseTables);
        xcCourseTables.setChooseCourseId(xcChooseCourse.getId()); //记录选课表的关联
        xcCourseTables.setCourseType(xcChooseCourse.getOrderType());
        xcCourseTables.setUpdateDate(LocalDateTime.now());
        int result = xcCourseTablesMapper.insert(xcCourseTables);
        if(result<=0){
            throw new XueChengPlusException("添加我的课程表失败");
        }
        return xcCourseTables;

    }
    /**
     * @description 根据课程和用户查询我的课程表中某一门课程
     * @param userId
     * @param courseId
     * @return com.xuecheng.learning.model.po.XcCourseTables
     * @author Mr.M
     * @date 2022/10/2 17:07
     */
    public XcCourseTables getXcCourseTables(String userId,Long courseId){
        XcCourseTables xcCourseTables = xcCourseTablesMapper.selectOne(new LambdaQueryWrapper<XcCourseTables>().eq(XcCourseTables::getUserId, userId).eq(XcCourseTables::getCourseId, courseId));
        return xcCourseTables;

    }

}
