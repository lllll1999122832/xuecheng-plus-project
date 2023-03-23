package tang.media;

import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 测试Minio的sdk
 */
@SpringBootTest
public class MinioTest {
    // Create a minioClient with the MinIO server playground, its access key and secret key.
    MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.101.128:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();
    @Test
    public void testUpload(){
        try {
            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket("testbuckets") //确定桶
                    .object("001/tang.mp4")// 确定对象名 添加子目录
                    .filename("E:\\视频\\视频\\share_b3d1163eada6d0cde8017576357ced77.mp4") //自己文件
                    .contentType("video/mp4")//默认根据扩展名确定文件内容类型，也可以指定
                    .build();
            //上传文件
            minioClient.uploadObject(testbucket);
            System.out.println("上传成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("上传失败");
        }

    }


}
