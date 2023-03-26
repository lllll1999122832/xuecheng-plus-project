package tang.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tang.media.mapper.MediaFilesMapper;
import tang.media.mapper.MediaProcessHistoryMapper;
import tang.media.mapper.MediaProcessMapper;
import tang.media.model.po.MediaFiles;
import tang.media.model.po.MediaProcess;
import tang.media.model.po.MediaProcessHistory;
import tang.media.service.MediaFileProcessService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class MediaFileProcessServiceImpl implements MediaFileProcessService {
    @Autowired
    MediaProcessMapper mediaProcessMapper;
    @Autowired
    MediaFilesMapper mediaFilesMapper;
    @Autowired
    MediaProcessHistoryMapper mediaProcessHistoryMapper;
    @Override
    public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count) {
        return mediaProcessMapper.selectListByShardIndex(shardTotal,shardIndex,count);
    }

    //开始任务
    public boolean startTask(long id) {
        int result = mediaProcessMapper.startTask(id);
        return result<=0?false:true;
    }

    @Override
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {
       //需要更新的任务
        MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
        if(Objects.isNull(mediaProcess)){
            return;
        }
        //如果任务更新失败
        if(status.equals("3")){
            //更新mediaProcess状态
            mediaProcess.setStatus("3");
            mediaProcess.setFailCount(mediaProcess.getFailCount()+1);
            mediaProcess.setErrormsg(errorMsg);
            mediaProcessMapper.updateById(mediaProcess);
            //更高效的更新方式
            //todo 将上面的更新方式换为更高效的更新方式
//            mediaProcessMapper.update()
            return;
        }
        //如果任务更新成功
        //把url更新到文件表中
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        mediaFiles.setUrl(url);
        mediaFilesMapper.updateById(mediaFiles);
        //更新MediaProcess表的状态
        mediaProcess.setStatus("2");
        mediaProcess.setFinishDate(LocalDateTime.now());
        mediaProcess.setUrl(url);
        mediaProcessMapper.updateById(mediaProcess);
        //插入历史表中
        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();
        BeanUtils.copyProperties(mediaProcess,mediaProcessHistory);
        mediaProcessHistoryMapper.insert(mediaProcessHistory);
        //删除当前任务
        LambdaQueryWrapper<MediaProcess>lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(MediaProcess::getFileId,fileId);
        mediaProcessMapper.delete(lambdaQueryWrapper);
    }

}
