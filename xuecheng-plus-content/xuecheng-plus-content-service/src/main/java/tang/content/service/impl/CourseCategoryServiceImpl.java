package tang.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tang.content.dto.CourseCategoryTreeDto;
import tang.content.mapper.CourseCategoryMapper;
import tang.content.service.CourseCategoryService;
import tang.content.po.CourseCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
* @author 曾梦想仗剑走天涯
* @description 针对表【course_category(课程分类)】的数据库操作Service实现
* @createDate 2023-03-19 13:51:41
*/
@Service
public class CourseCategoryServiceImpl extends ServiceImpl<CourseCategoryMapper, CourseCategory>
    implements CourseCategoryService {
    @Autowired
    CourseCategoryMapper courseCategoryMapper;
    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        //调用mapper递归查询出分类消息
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);
        //封装成List<CourseCategoryTreeDto>
        List<CourseCategoryTreeDto> courseCategoryTree = courseCategoryTreeDtos.stream()
                //过滤掉第一层
                .filter(courseCategory -> !courseCategory.getId().equals(id))
                //得到第二层
                .filter(courseCategory -> courseCategory.getParentid().equals(id))
                //先设置它的ChildrenTreeNodes为空
                .map(courseCategory->courseCategory.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>()))
                .collect(Collectors.toList());
        //设置好子类
        List<CourseCategoryTreeDto> ResultCourseCategoryTreeDto = courseCategoryTree.stream().
                map(courseCategory -> courseCategory.setChildrenTreeNodes(setChildrenCourseCategoryTreeDto(courseCategoryTreeDtos, courseCategory.getId())))
                .collect(Collectors.toList());
        return ResultCourseCategoryTreeDto;
    }
    public List<CourseCategoryTreeDto> setChildrenCourseCategoryTreeDto(List<CourseCategoryTreeDto> courseCategoryTreeDtos,String parentId){
       //他们的父类id为parentId
        return  courseCategoryTreeDtos.stream().filter(courseCategory->courseCategory.getParentid().equals(parentId))
                .collect(Collectors.toList());
    }
}




