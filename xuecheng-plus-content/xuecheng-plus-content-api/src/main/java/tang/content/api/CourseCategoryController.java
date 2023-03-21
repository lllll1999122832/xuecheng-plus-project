package tang.content.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tang.content.dto.CourseCategoryTreeDto;
import tang.content.service.CourseCategoryService;

import java.util.List;


@RestController

public class CourseCategoryController {
    @Autowired
    CourseCategoryService categoryService;
    @GetMapping("/course-category/tree-nodes")
    public List<CourseCategoryTreeDto> getCourseCategoryTree(){
        return  categoryService.queryTreeNodes("1");
    }

}
