package tang.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;
import tang.media.model.po.MediaProcess;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author itcast
 */
@Mapper
@Repository
public interface MediaProcessMapper extends BaseMapper<MediaProcess> {

    @Select("select * from media_process " +
            "t where t.id % #{shardTotal}=#{shardIndex} and (t.status=1 or  t.status=3 or t.status=4) " +
            "and t.fail_count<3 limit #{count}")
    List<MediaProcess> selectListByShardIndex(@Param("shardTotal") int shardTotal,
                                              @Param("shardIndex") int shardIndex,
                                              @Param("count") int count);

    /**
     * 开启一个任务
     * @param id 任务id
     * @return 更新记录数
     */
    @Update("update media_process m set m.status='4' where" +
            " (m.status='1' or m.status='3') " +
            "and m.fail_count<3 and m.id=#{id}")
    int startTask(@Param("id") long id);

}
