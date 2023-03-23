package tang;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 内容管理服务启动类
 */
@SpringBootApplication
@MapperScan("tang.content.mapper")
public class ContentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContentApplication.class,args);
    }
}
