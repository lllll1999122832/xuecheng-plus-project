package tang.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.core.util.SystemMillisClock;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tang.content.dto.AddCourseDto;
import tang.content.dto.CourseBaseInfoDto;
import tang.content.dto.EditCourseDto;
import tang.content.dto.QueryCourseParamsDto;
import tang.content.mapper.CourseBaseMapper;
import tang.content.mapper.CourseCategoryMapper;
import tang.content.mapper.CourseMarketMapper;
import tang.content.po.CourseCategory;
import tang.content.po.CourseMarket;
import tang.content.service.CourseBaseService;
import tang.content.po.CourseBase;
import tang.xuechengplusbase.base.exception.XueChengPlusException;
import tang.xuechengplusbase.base.model.PageParams;
import tang.xuechengplusbase.base.model.PageResult;
import tang.xuechengplusbase.base.utils.BeanCopyUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
* @author 曾梦想仗剑走天涯
* @description 针对表【course_base(课程基本信息)】的数据库操作Service实现
* @createDate 2023-03-19 13:51:41
*/
@Service
public class CourseBaseServiceImpl extends ServiceImpl<CourseBaseMapper, CourseBase>
    implements CourseBaseService {

    /**
     * 课程分页查询
     * @param pageParams
     * @param queryCourseParamsDto
     * @return
     */
    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CourseCategoryMapper courseCategoryMapper;
    @Autowired
    CourseBaseMapper courseBaseMapper;
    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {
        //封装查询条件
        LambdaQueryWrapper<CourseBase>lambdaQueryWrapper=new LambdaQueryWrapper<>();
        //模糊查询输入课程名
        lambdaQueryWrapper.like(!StringUtils.isBlank(queryCourseParamsDto.getCourseName()),CourseBase::getName,queryCourseParamsDto.getCourseName());
        //根据审核状态查询
        lambdaQueryWrapper.eq(!StringUtils.isBlank(queryCourseParamsDto.getAuditStatus()),CourseBase::getAuditStatus,queryCourseParamsDto.getAuditStatus());
        //根据课程发布状态查询
        lambdaQueryWrapper.eq(!StringUtils.isBlank(queryCourseParamsDto.getPublishStatus()),CourseBase::getStatus,queryCourseParamsDto.getPublishStatus());
        //进行封页查询
        Page<CourseBase>page=new Page<>(pageParams.getPageNo(),pageParams.getPageSize());
         this.page(page,lambdaQueryWrapper);
         //获取数据列表
        List<CourseBase> records = page.getRecords();
        //总记录数
        long total = page.getTotal();
        PageResult<CourseBase> courseBasePageResult = new PageResult<>(records, total, pageParams.getPageNo(), pageParams.getPageSize());
        return courseBasePageResult;
    }

    /**
     * 新增课程,还要新增营销信息
     * companyId 机构id
     * @param dto 课程信息
     * @return
     */
    @Override
    @Transactional
    public CourseBaseInfoDto createCourseBase(Long companyId,AddCourseDto dto) {
        //参数合法校验
//        if (StringUtils.isBlank(dto.getName())) {
//            throw new XueChengPlusException("课程名称为空");
//        }
//
//        if (StringUtils.isBlank(dto.getMt())) {
//            throw new XueChengPlusException("课程分类为空");
//        }
//
//        if (StringUtils.isBlank(dto.getSt())) {
//            throw new XueChengPlusException("课程分类为空");
//        }
//
//        if (StringUtils.isBlank(dto.getGrade())) {
//            throw new XueChengPlusException("课程等级为空");
//        }
//
//        if (StringUtils.isBlank(dto.getTeachmode())) {
//            throw new XueChengPlusException("教育模式为空");
//        }
//
//        if (StringUtils.isBlank(dto.getUsers())) {
//            throw new XueChengPlusException("适应人群为空");
//        }
//
//        if (StringUtils.isBlank(dto.getCharge())) {
//            throw new XueChengPlusException("收费规则为空");
//        }
        //向课程基本信息表写入信息 Course_base
        //使用BeanUtils拷贝
        CourseBase courseBase = BeanCopyUtils.copyBean(dto, CourseBase.class); //只要属性一直可以拷贝
        //设置关键信息
        courseBase.setCompanyId(companyId);
        courseBase.setCreateDate(new Date(System.currentTimeMillis()));
        //审核状态默认未提交
        courseBase.setAuditStatus("202002");
        //发布状态默认为未发布
        courseBase.setStatus("203001");
        //把传入参数放入CourseBase
        boolean save = this.save(courseBase);
        if (!save){
            throw new RuntimeException("添加课程信息失败!");
        }
        //获取课程的id
        Long id = courseBase.getId();
        //向营销表写入信息 Course_mark
        //将页面输入数据拷贝到营销表中
        CourseMarket courseMarket = BeanCopyUtils.copyBean(dto, CourseMarket.class);
        courseMarket.setId(id);
        //单独写一个营销表,存在即更新,不存在即插入
        saveCourseMarket(courseMarket);
        //从数据库中查询详细的信息,包括两部分
        return getCourseBaseInfo(id);
    }

    /**
     * 根据课程Id查询信息
     * @param courseId
     * @return
     */
    @Override
    public CourseBaseInfoDto getCourseById(Long courseId) {
        return getCourseBaseInfo(courseId);
    }

    @Override
    public CourseBaseInfoDto modifyCourse(Long companyId, EditCourseDto editCourseDto) {
        //拿到课程id
        CourseBaseInfoDto course = this.getCourseById(editCourseDto.getId());
        if(Objects.isNull(course)){
            throw  new XueChengPlusException("课程不存在");
        }
        //数据合法性校验
        //业务逻辑校验
//        //  todo 本机构只能修改本机构的课程
//        if(!course.getCompanyId().equals(companyId)){
//            throw  new XueChengPlusException("本机构只能修改本机构的课程");
//        }
        //封装数据
        CourseBase courseBase = BeanCopyUtils.copyBean(editCourseDto, CourseBase.class);
        //更新数据库
         if (!this.updateById(courseBase)) {
            throw  new XueChengPlusException("更新失败");
        }
         //更新营销表
        CourseMarket courseMarket = BeanCopyUtils.copyBean(editCourseDto, CourseMarket.class);
        saveCourseMarket(courseMarket);
        return getCourseBaseInfo(courseBase.getId());
    }

    /**
     *    从数据库中查询详细的信息,包括两部分
     */
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId){
        //课程表查询
        CourseBase courseBase =courseBaseMapper.selectById(courseId);
        if(Objects.isNull(courseBase)){
            return null;
        }
        //营销表查询
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //组装在一起
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        if(!Objects.isNull(courseMarket)){
            BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);
        }

        //通过courseCategoryMapper查询分类信息 信息放入courseBaseInfoDto中
        //todo
        //大分类
        CourseCategory mt = courseCategoryMapper.selectById(courseBase.getMt());
        courseBaseInfoDto.setMtName(mt.getName());
        CourseCategory st = courseCategoryMapper.selectById(courseBase.getSt());
        courseBaseInfoDto.setStName(st.getName());
        return courseBaseInfoDto;
    }

    //单独写一个营销表,存在即更新,不存在即插入
    private int saveCourseMarket(CourseMarket courseMarket){
        //合法校验
        String charge = courseMarket.getCharge();
        if(StringUtils.isBlank(charge)){
            throw new XueChengPlusException("收费规则没有选择");
        }
         //收费规则为收费
        if(charge.equals("201001")){
            if(courseMarket.getPrice() == null || courseMarket.getPrice().floatValue()<=0){
                throw new XueChengPlusException("课程为收费价格不能为空且必须大于0");
            }
        }
        //根据id从课程营销表查询
        Long id = courseMarket.getId();
        CourseMarket courseMarketOld = courseMarketMapper.selectById(id);
        if(Objects.isNull(courseMarketOld)){
            //插入数据库
            return courseMarketMapper.insert(courseMarket);
        }else{
            //更新数据
            return courseMarketMapper.updateById(courseMarket);
        }
    }
}




