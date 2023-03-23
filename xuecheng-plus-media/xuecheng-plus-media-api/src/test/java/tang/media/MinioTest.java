package tang.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.io.*;
import java.util.Objects;

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
    // 根据扩展名取出mimetype
    //根据扩展名取出mimeType
    ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".mp4");
    String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
    @Test
    public void testUpload(){

        try {
            if(!Objects.isNull(extensionMatch)){
                mimeType=extensionMatch.getMimeType();
            }
            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket("testbuckets") //确定桶
                    .object("test/lin.mp4")// 确定对象名 添加子目录
                    .filename("E:\\视频\\视频\\share_b3d1163eada6d0cde8017576357ced77.mp4") //自己文件
                    .contentType(mimeType)//默认根据扩展名确定文件内容类型，也可以指定
                    .build();
            //上传文件

            minioClient.uploadObject(testbucket);
            System.out.println("上传成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("上传失败");
        }
    }
    @Test
    public void testDelete(){
        try {
            RemoveObjectArgs testbucket = RemoveObjectArgs.builder()
                    .bucket("testbuckets") //确定桶
                    .object("/test/tang.mp4")// 确定对象名 添加子目录
//                    .contentType("video/mp4")//默认根据扩展名确定文件内容类型，也可以指定
                    .build();
            //上传文件
            minioClient.removeObject(testbucket);
            System.out.println("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("删除失败");
        }
    }
    @Test
    public void testSelect(){
        try {
            GetObjectArgs testbucket = GetObjectArgs.builder()
                    .bucket("testbuckets") //确定桶
                    .object("/test/lin.mp4")// 确定对象名 添加子目录
//                    .contentType("video/mp4")//默认根据扩展名确定文件内容类型，也可以指定
                    .build();
            //上传文件
            //查询远程的流
            FilterInputStream object = minioClient.getObject(testbucket);
            FileOutputStream filterOutputStream=new FileOutputStream(new File("C:\\Users\\31461\\Desktop\\lin.mp4"));
            System.out.println("查询成功");
            IOUtils.copy(object,filterOutputStream);
            //校验文件的完整性对文件的内容进行md5
            String in = DigestUtils.md5Hex(object);
            System.out.println(in);
            String out = DigestUtils.md5Hex(new FileInputStream("C:\\Users\\31461\\Desktop\\lin.mp4"));
            System.out.println(out);
            System.out.println(in.equals(out));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("查询失败");
        }
    }

}
