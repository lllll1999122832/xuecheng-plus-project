package tang.content.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import tang.content.dto.SaveTeachplanDto;
import tang.content.dto.TeachplanDto;
import tang.content.service.TeachplanService;

import java.util.List;

/**
 * 课程计划相关的接口
 */
@Api(value = "课程计划编辑接口",tags = "课程计划编辑接口")
@RestController
public class TeachplanController {
    @Autowired
    TeachplanService teachplanService;

    //查询课程计划接口
    @ApiOperation("查询课程计划树形结构")
    @ApiImplicitParam(value = "courseId",name = "课程Id",required = true,dataType = "Long",paramType = "path")
    @GetMapping("/teachplan/{id}/tree-nodes")
    public List<TeachplanDto> getTreeNodes(@PathVariable("id")Long courseId){
        return teachplanService.getTreeNodes(courseId);
    }

    @ApiOperation("课程计划新建或者修改")
    @PostMapping("/teachplan")
    public void saveTeachPlan(@RequestBody SaveTeachplanDto saveTeachplanDto){
        //难点在Service
        teachplanService.saveTeachPlan(saveTeachplanDto);
    }
}
