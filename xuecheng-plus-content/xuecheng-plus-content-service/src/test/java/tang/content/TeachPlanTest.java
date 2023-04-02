package tang.content;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import tang.ContentApplication;
import tang.content.dto.CoursePreviewDto;
import tang.content.dto.TeachplanDto;
import tang.content.mapper.TeachplanMapper;
import tang.content.service.CoursePublishPreService;
import tang.content.service.CoursePublishService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SpringBootTest(classes = ContentApplication.class)
/**
 * 课程计划测试
 */
public class TeachPlanTest {
    @Autowired
    TeachplanMapper teachplanMapper;
    @Autowired
    CoursePublishService coursePublishService;
    @Test
    public void getTreeNodes(){
        List<TeachplanDto> treeNodes = teachplanMapper.getTreeNodes(117L);
        for (TeachplanDto treeNode : treeNodes) {
            System.out.println(treeNode);
        }
    }
    @Test
    public void testFreeMark() throws IOException, TemplateException {
        //获取模板
        Configuration configuration = new Configuration(Configuration.getVersion());
        //得到路径
        String path = this.getClass().getResource("/").getPath();
        //拿到模板的目录
        configuration.setDirectoryForTemplateLoading(new File(path+"/templates/"));
        //指定编码
        configuration.setDefaultEncoding("utf-8");
        Template template = configuration.getTemplate("course_template.ftl");
        //准备数据
        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(120L);
        Map<String, Object>map=new HashMap<>();
        map.put("model",coursePreviewInfo);
        String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
        //将html写入文件
        //输入流
        InputStream inputStream = IOUtils.toInputStream(html, "utf-8");
        //输出流
        FileOutputStream fileOutputStream = new FileOutputStream(new File("E:\\WEB客户端设计\\120.html"));
        //使用流将html写入文件
        IOUtils.copy(inputStream, fileOutputStream);
    }
}
