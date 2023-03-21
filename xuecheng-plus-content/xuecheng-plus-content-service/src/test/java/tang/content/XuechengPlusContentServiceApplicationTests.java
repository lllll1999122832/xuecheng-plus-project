package tang.content;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tang.content.po.CourseBase;
import tang.content.service.CourseBaseService;

import java.util.List;

@SpringBootTest
class XuechengPlusContentServiceApplicationTests {

    @Autowired
    CourseBaseService courseBaseService;
    @Test
    void contextLoads() {
        List<CourseBase> list =courseBaseService.list();
        list.forEach(System.out::println);
    }
    @Test
    void testBaseMapper(){

    }

}
