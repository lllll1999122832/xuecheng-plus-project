package tang.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import tang.media.model.po.MediaProcessHistory;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author itcast
 */
@Mapper
@Repository
public interface MediaProcessHistoryMapper extends BaseMapper<MediaProcessHistory> {

}
