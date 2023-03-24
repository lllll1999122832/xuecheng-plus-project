package tang.media;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.util.Arrays;

@SpringBootTest

public class BigFileTest {
    //分块
    @Test
    public void testChunk() throws IOException {
        //找到源文件
        File file = new File("E:\\1.mp4");

        //分块文件存储路径
        String chunkFilePath="C:\\Users\\31461\\Desktop\\JVM\\";
        //分块文件大小
        long chunkSize=1024*1024*5; //1M
        //分块文件大小
        long chunkNum = (long) Math.ceil(file.length() * 1.0 / chunkSize); //向上抛一位
        //从源文件中读数据,向分块中写数据
        RandomAccessFile r = new RandomAccessFile(file, "r");
        //缓存区
        byte[]bytes=new byte[1024];
        for (int i = 0; i < chunkNum; i++) {
            File tempFile = new File(chunkFilePath + i);
            RandomAccessFile rw = new RandomAccessFile(tempFile, "rw");
            int len=-1;
            while((len=r.read(bytes))!=-1){
                rw.write(bytes,0,len);
                if(tempFile.length()>=chunkSize){
                    break;
                }
            }
            rw.close();
        }
        r.close();
    }
    //合并
    @Test
    public void testMerge() throws IOException {
        //块文件目录
        File chunkFolder = new File("C:\\Users\\31461\\Desktop\\JVM");
        //源文件
        File source = new File("E:\\1.mp4");
        //合并后的文件
        File mergeFile = new File("C:\\Users\\31461\\Desktop\\paizhao.mp4");
        //取出文件所有的分块文件
        File[] files = chunkFolder.listFiles();
        //对快文件排序
        Arrays.sort(files,(a,b)->Integer.parseInt(a.getName())-Integer.parseInt(a.getName()));
       //向合并文件写的流
        RandomAccessFile rw = new RandomAccessFile(mergeFile, "rw");
        //遍历分块文件
        //缓冲区
        byte[]bytes=new byte[1024*10];
        for (File file : files) {
            //读分块文件
            RandomAccessFile r = new RandomAccessFile(file, "r");
            int len=-1;
            System.out.println(file.getName());
            while ((len=r.read(bytes))!=-1){
                rw.write(bytes,0,len);
            }
            r.close();
        }
        rw.close();
        FileInputStream fileInputStream_mergeFile = new FileInputStream(mergeFile);
        System.out.println(DigestUtils.md5Hex(fileInputStream_mergeFile).equals(DigestUtils.md5Hex(new FileInputStream(source))));
    }
}
