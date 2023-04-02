package tang.content.feginclient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
@Component
@Slf4j
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaServiceClient> {
    @Override
    public MediaServiceClient create(Throwable throwable) {
        //当发生熔断,上游方法就会执行此降级逻辑
        //好处,拿到了熔断异常
        return new MediaServiceClient() {
            @Override
            public String upload(MultipartFile filedata, String objectName) {
                log.debug("远程调用发生熔断,异常原因:{}",throwable.toString());
                return null;
            }
        };
    }
}
