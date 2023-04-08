package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tang.xuechengplusbase.base.exception.XueChengPlusException;
import tang.xuechengplusbase.base.utils.IdWorkerUtils;
import tang.xuechengplusbase.base.utils.QRCodeUtil;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    XcOrdersMapper xcOrdersMapper;
    @Autowired
    XcOrdersGoodsMapper xcOrdersGoodsMapper;
    @Autowired
    XcPayRecordMapper xcPayRecordMapper;
    @Value("${pay.qrcodeurl}")
    String qrcodeurl;
    //    @Value("${pay.alipay.APP_ID}")
    String APP_ID;
    //    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;

    //    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;
    @Override
    @Transactional
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {
        //插入订单,插入订单明细表
        //进行幂等性的判断,不管多少次操作,结果都是一样的,同一个选课记录只有一个订单
        XcOrders xcOrders = saveXcOrders(userId, addOrderDto);
        //插入支付记录
        XcPayRecord payRecord = createPayRecord(xcOrders);
        //生成二维码
        Long payNo = payRecord.getPayNo();
        QRCodeUtil qrCodeUtil = new QRCodeUtil();
        //支付二维码的地址
        String url = String.format(qrcodeurl, payNo);
        String QRcode;
        try {
            QRcode=qrCodeUtil.createQRCode(url,200,200);
        } catch (IOException e) {
            throw new XueChengPlusException("生成二维码失败!");
        }
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord,payRecordDto);
        payRecordDto.setQrcode(QRcode);
        return payRecordDto;
    }

    @Override
    public XcPayRecord getPayRecordByPayno(String payNo) {
        XcPayRecord xcPayRecord = xcPayRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
        return xcPayRecord;
    }

    @Override
    public PayRecordDto queryPayResult(String payNo) {
        //调用支付宝的接口查询支付结果
        PayStatusDto payStatusDto = queryPayResultFromAlipay(payNo);
        //拿到支付结果,更新支付状态
        saveAliPayStatus(payStatusDto);
        //返回最新的支付记录信息
        XcPayRecord payRecordByPayno = getPayRecordByPayno(payNo);
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecordByPayno,payRecordDto);
        return payRecordDto;
    }
    /**
     * 请求支付宝查询支付结果
     * @param payNo 支付交易号
     * @return 支付结果
     */
    public PayStatusDto queryPayResultFromAlipay(String payNo){
        //========请求支付宝查询支付结果=============
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, "json", AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE); //获得初始化的AlipayClient
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo);
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                XueChengPlusException.cast("请求支付查询查询失败");//交易不存在
            }
        } catch (AlipayApiException e) {
            log.error("请求支付宝查询支付结果异常:{}", e.toString(), e);
            XueChengPlusException.cast("请求支付查询查询失败");
        }

        //获取支付结果
        String resultJson = response.getBody();
        //转map
        Map resultMap = JSON.parseObject(resultJson, Map.class);
        Map alipay_trade_query_response = (Map) resultMap.get("alipay_trade_query_response");
        //支付结果
        String trade_status = (String) alipay_trade_query_response.get("trade_status");
        String total_amount = (String) alipay_trade_query_response.get("total_amount");
        String trade_no = (String) alipay_trade_query_response.get("trade_no");
        //保存支付结果
        PayStatusDto payStatusDto = new PayStatusDto();
        payStatusDto.setOut_trade_no(payNo);
        payStatusDto.setTrade_status(trade_status);
        payStatusDto.setApp_id(APP_ID);
        payStatusDto.setTrade_no(trade_no);
        payStatusDto.setTotal_amount(total_amount);
        return payStatusDto;
    }

    /**
     * @description 保存支付宝支付结果
     * @param payStatusDto  支付结果信息
     * @return void
     * @author Mr.M
     * @date 2022/10/4 16:52
     */
    @Transactional
    public void saveAliPayStatus(PayStatusDto payStatusDto) {
        String payNo= payStatusDto.getOut_trade_no();
        //如果支付成功
        XcPayRecord payRecordByPayno = getPayRecordByPayno(payNo);
        if(Objects.isNull(payRecordByPayno)){
            throw new XueChengPlusException("找不到相关的记录!");
        }
        //订单的id
        Long orderId = payRecordByPayno.getOrderId();
        XcOrders xcOrders = xcOrdersMapper.selectById(orderId);
        if(Objects.isNull(orderId)){
            throw new XueChengPlusException("找不到相关的订单!");
        }
        //支付状态
        String statusFromDB = payRecordByPayno.getStatus();
        //不在处理了
        if(statusFromDB.equals("601002")){
            //已经保存过了
            return;
        }
        String tradeStatus = payStatusDto.getTrade_status();//支付宝查询到的支付结果
        if(!tradeStatus.equals("TRADE_SUCCESS")){
            throw new XueChengPlusException("支付为成功!");
        }
        //更新支付记录表的状态为支付成功
        payRecordByPayno.setStatus("601002");
        payRecordByPayno.setOutPayNo(payStatusDto.getTrade_no());//支付宝的订单号
        //第三方渠道编号
        payRecordByPayno.setOutPayChannel("Alipay");
        payRecordByPayno.setPaySuccessTime(LocalDateTime.now());
        xcPayRecordMapper.updateById(payRecordByPayno);
        //更新订单表的状态为支付成功
        xcOrders.setStatus("600002");//订单交易状态为交易成功
        xcOrdersMapper.updateById(xcOrders);
    }


    /**
     * 保存订单信息
     * @param userId
     * @param addOrderDto
     * @return
     */

    @Transactional
    public XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto) {
        //插入订单,插入订单明细表
        //进行幂等性的判断,不管多少次操作,结果都是一样的,同一个选课记录只有一个订单
        XcOrders xcOrders = getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if(!Objects.isNull(xcOrders)){
            return xcOrders;
        }
        //插入订单表,
        xcOrders=new XcOrders();
        xcOrders.setId(IdWorkerUtils.getInstance().nextId()); //雪花算法
        xcOrders.setTotalPrice(addOrderDto.getTotalPrice());
        xcOrders.setCreateDate(LocalDateTime.now());
        xcOrders.setStatus("600001");//未支付
        xcOrders.setUserId(userId);
        xcOrders.setOrderType("60201"); //订单类型,购买课程
        xcOrders.setOrderName(addOrderDto.getOrderName());
        xcOrders.setOrderDescrip(addOrderDto.getOrderDescrip());
        xcOrders.setOrderDetail(addOrderDto.getOrderDetail());
        xcOrders.setOutBusinessId(addOrderDto.getOutBusinessId()); //课程id,如果是购买课程就是记录选课的id
        int result = xcOrdersMapper.insert(xcOrders);
        Long xcOrdersId = xcOrders.getId();
        if(result<=0){
            throw new XueChengPlusException("添加订单失败!");
        }
        //插入订单明细表
        //将前端传入的明细的json串转成list
        String orderDetail = addOrderDto.getOrderDetail();
        List<XcOrdersGoods> xcOrdersGoods = JSON.parseArray(orderDetail, XcOrdersGoods.class);
        //遍历插入订单明细
        xcOrdersGoods.forEach(ordersGood-> {
            ordersGood.setOrderId(xcOrdersId);
            int insert = xcOrdersGoodsMapper.insert(ordersGood);
        }); //设置好他们的订单id
        return xcOrders;
    }
    //根据业务id查询订单
    //业务id就是选课表中的主键
    public XcOrders getOrderByBusinessId(String businessId) {
        XcOrders orders = xcOrdersMapper.selectOne(new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId, businessId));
        return orders;
    }

    /**
     * 保存支付记录
     * @param orders
     * @return
     */
    public XcPayRecord createPayRecord(XcOrders orders) {
        Long ordersId = orders.getId();
        //假如订单不存在,就不添加支付记录
        XcOrders xcOrders = xcOrdersMapper.selectById(ordersId);
        if(Objects.isNull(xcOrders)){
            throw new XueChengPlusException("订单不存在!");
        }
        //如果此订单支付结果为成功,就不再添加支付记录,避免重复支付
        String status = orders.getStatus();
        if(status.equals("601002")){
            //表示支付成功
            throw new XueChengPlusException("此订单已经支付!");
        }
        //添加支付记录
        XcPayRecord xcPayRecord = new XcPayRecord();
        xcPayRecord.setPayNo(IdWorkerUtils.getInstance().nextId()); //将来要传给支付宝
        xcPayRecord.setOrderId(ordersId);
        xcPayRecord.setOrderName(orders.getOrderName());
        xcPayRecord.setTotalPrice(orders.getTotalPrice());
        xcPayRecord.setCurrency("CNY");
        xcPayRecord.setCreateDate(LocalDateTime.now());
        xcPayRecord.setStatus("601001");//未支付
        xcPayRecord.setUserId(orders.getUserId());
        int insert = xcPayRecordMapper.insert(xcPayRecord);
        if(insert<=0){
            throw new XueChengPlusException("插入支付记录失败!");
        }
        return xcPayRecord;
    }

}
