package tang.orders.service;

import tang.orders.model.dto.AddOrderDto;
import tang.orders.model.dto.PayRecordDto;
import tang.orders.model.dto.PayStatusDto;
import tang.orders.model.po.XcPayRecord;
import tang.messagesdk.model.po.MqMessage;

/**
 * 订单相关的Service
 */
public interface OrderService {

    /**
     * @description 创建商品订单
     * @param addOrderDto 订单信息
     * @return PayRecordDto 支付交易记录(包括二维码)
     * @author Mr.M
     * @date 2022/10/4 11:02
     */
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto);
    /**
     * @description 查询支付交易记录
     * @param payNo  交易记录号
     * @return com.xuecheng.orders.model.po.XcPayRecord
     * @author Mr.M
     * @date 2022/10/20 23:38
     */
    public XcPayRecord getPayRecordByPayno(String payNo);

    /**
     * 查询支付结果
     * @param payNo
     * @return
     */

    public PayRecordDto queryPayResult(String payNo);

    /**
     * 保存支付状态
     * @param payStatusDto
     */
    public void saveAliPayStatus(PayStatusDto payStatusDto);

    /**
     * 发送通知结果
     */
    public void notifyPayResult(MqMessage mqMessage);
}
