package tang.content;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tang.ContentApplication;
import tang.content.dto.TeachplanDto;
import tang.content.mapper.TeachplanMapper;

import java.util.List;

@SpringBootTest(classes = ContentApplication.class)
/**
 * 课程计划测试
 */
public class TeachPlanTest {
    @Autowired
    TeachplanMapper teachplanMapper;
    @Test
    public void getTreeNodes(){
        List<TeachplanDto> treeNodes = teachplanMapper.getTreeNodes(117L);
        for (TeachplanDto treeNode : treeNodes) {
            System.out.println(treeNode);
        }
    }
}
