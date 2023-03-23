package tang.content.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tang.content.dto.SaveTeachplanDto;
import tang.content.dto.TeachplanDto;
import tang.content.mapper.TeachplanMapper;
import tang.content.service.TeachplanService;
import tang.content.po.Teachplan;
import tang.xuechengplusbase.base.utils.BeanCopyUtils;

import java.util.List;
import java.util.Objects;

/**
* @author 曾梦想仗剑走天涯
* @description 针对表【teachplan(课程计划)】的数据库操作Service实现
* @createDate 2023-03-19 13:51:41
*/
@Service
public class TeachplanServiceImpl extends ServiceImpl<TeachplanMapper, Teachplan>
    implements TeachplanService {

    @Autowired
    TeachplanMapper teachplanMapper;
    @Override
    public List<TeachplanDto> getTreeNodes(Long courseId) {
        return teachplanMapper.getTreeNodes(courseId);
    }

    @Override
    public void saveTeachPlan(SaveTeachplanDto saveTeachplanDto) {
        //通过判断课程计划的id来判断是新增还是修改
        Long teachplanId=saveTeachplanDto.getId();
        if(Objects.isNull(teachplanId)){
            //新增
            Teachplan teachplan = BeanCopyUtils.copyBean(saveTeachplanDto, Teachplan.class);
            //确定排序顺序 找到同级节点的个数,排序字段就是个数加1
            //select * from teachplan where parentid=#{parentid} and course_id=#{course_id}
            Long courseId = teachplan.getCourseId();
            Long parentid = teachplan.getParentid();
            LambdaQueryWrapper<Teachplan>lambdaQueryWrapper=new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Teachplan::getCourseId,courseId);
            lambdaQueryWrapper.eq(Teachplan::getParentid,parentid);
            Integer count = this.count(lambdaQueryWrapper);
            //排序加1
            teachplan.setOrderby(count+1);
            this.save(teachplan);
        }else{
            //更新
            Teachplan teachplan = this.getById(teachplanId);
            //拷贝参数
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            this.updateById(teachplan);
        }
    }
}




