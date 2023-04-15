package tang.learning.service.impl;

import com.alibaba.fastjson.JSON;
import tang.learning.config.PayNotifyConfig;
import tang.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tang.messagesdk.model.po.MqMessage;
import tang.xuechengplusbase.base.exception.XueChengPlusException;

/**
 * 接受消息通知的类
 */
@Service
@Slf4j
public class ReceivePayNotifyService {
    @Autowired
    MyCourseTablesService myCourseTablesService;


    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
    public void receive(Message message){
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //拿到消息
        byte[] body = message.getBody();
        String jsonString = new String(body);
        //转成对象
        MqMessage mqMessage = JSON.parseObject(jsonString, MqMessage.class);
        //根据消息的内容,更新选课记录表
        //向我的课程表插入记录
        //解析消息内容
        String chooseCourseId = mqMessage.getBusinessKey1(); //选课的id
        String orderType = mqMessage.getBusinessKey2(); //订单类型
        if(orderType.equals("60201")){
            //学习中心服务只要购买课程类的支付订单的结果
            boolean result = myCourseTablesService.saveChooseCourseSuccess(chooseCourseId);
            if(!result){
                throw new XueChengPlusException("保存选课记录失败!");
            }
        }
    }


}
