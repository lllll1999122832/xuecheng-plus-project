package tang.content;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;
import tang.content.config.MultipartSupportConfig;
import tang.content.feginclient.MediaServiceClient;

import java.io.File;

@SpringBootTest
public class FeginUploadTest {
    @Autowired
    MediaServiceClient mediaServiceClient;
    @Test
    public void test(){
        //将File转换为MultipleFile类型
        File file = new File("E:\\WEB客户端设计\\120.html");
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        mediaServiceClient.upload(multipartFile,"course/120.html");
    }
}
