package tang.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import tang.content.dto.CoursePreviewDto;
import tang.content.po.CoursePublish;

import java.io.File;

/**
* @author 曾梦想仗剑走天涯
 * 获取课程预览信息
* @description 针对表【course_publish(课程发布)】的数据库操作Service
* @createDate 2023-03-19 13:51:41
*/
public interface CoursePublishService extends IService<CoursePublish> {
    public CoursePreviewDto getCoursePreviewInfo(Long courseId);

    void commitAudit(Long companyId, Long courseId);

    /**
     * 课程发布发布
     * @param companyId
     * @param courseId
     */
    void coursepublish(Long companyId, Long courseId);

    /**
     * @description 课程静态化
     * @param courseId  课程id
     * @return File 静态化文件
     * @author Mr.M
     * @date 2022/9/23 16:59
     */
    public File generateCourseHtml(Long courseId);
    /**
     * @description 上传课程静态化页面
     * @param file  静态化文件
     * @return void
     * @author Mr.M
     * @date 2022/9/23 16:59
     */
    public void  uploadCourseHtml(Long courseId,File file);

    /**
     * 根据课程id查询发布信息
     * @param courseId
     * @return
     */
    CoursePublish getCoursePublish(Long courseId);

    /**
     * 白名单数据放入缓存 数据量又比较大的
     * @param courseId
     * @return
     */
    CoursePublish getCoursePublishCache(Long courseId);
}
