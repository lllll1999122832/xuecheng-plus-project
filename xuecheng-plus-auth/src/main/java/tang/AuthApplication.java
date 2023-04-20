package tang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableFeignClients(basePackages = "tang.ucenter.feignClient")
public class AuthApplication {
    @Bean
    //第三方的微信调用,本地微服务使用的fegin
    RestTemplate restTemplate(){
        RestTemplate restTemplate = new RestTemplate(new OkHttp3ClientHttpRequestFactory());
        return  restTemplate;
    }


    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);

    }

}
