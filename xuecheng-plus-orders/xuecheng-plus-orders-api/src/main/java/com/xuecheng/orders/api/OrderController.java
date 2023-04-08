package com.xuecheng.orders.api;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import com.xuecheng.orders.util.SecurityUtil;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import tang.xuechengplusbase.base.exception.XueChengPlusException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * 订单相关的接口
 */
@Slf4j
@Controller
public class OrderController {
    @Autowired
    OrderService orderService;
    //    @Value("${pay.alipay.APP_ID}")
    String APP_ID;
    //    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;

    //    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;

    @ApiOperation("生成支付二维码")
    @PostMapping("/generatepaycode")
    @ResponseBody
    public PayRecordDto generatePayCode(@RequestBody AddOrderDto addOrderDto) {
        //调用Service完成插入订单信息,插入支付记录,生成支付二维码
        String userId = SecurityUtil.getUser().getId();
        return orderService.createOrder(userId, addOrderDto);
    }

    @ApiOperation("扫码下单接口")
    @GetMapping("/requestpay")
    public void requestpay(String payNo, HttpServletResponse httpResponse) throws IOException, AlipayApiException {
        //请求支付宝去下单
        //传入支付记录号,判断支付记录号是否存在
        XcPayRecord payRecordByPayno = orderService.getPayRecordByPayno(payNo);
        if(Objects.isNull(payRecordByPayno)){
            throw new XueChengPlusException("请重新点击支付获取二维码");
        }
        //判断支付结果
        String status = payRecordByPayno.getStatus();
        if(status.equals("601002")){
            throw new XueChengPlusException("已经支付,无需重复支付!");
        }
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL,
                APP_ID, APP_PRIVATE_KEY, AlipayConfig.FORMAT,
                AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY,
                AlipayConfig.SIGNTYPE);;//获得初始化的AlipayClient
        AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();//创建API对应的request
//        alipayRequest.setReturnUrl("http://domain.com/CallBack/return_url.jsp");
//        alipayRequest.setNotifyUrl("http://domain.com/CallBack/notify_url.jsp");//在公共参数中设置回跳和通知地址
        alipayRequest.setNotifyUrl("http://localhost:63030/orders/paynotify");
        //这个是支付宝通知的域名
        //必须使用内网穿透
        alipayRequest.setBizContent("{" +
                "    \"out_trade_no\":\""+payNo+"\"," +
                "    \"total_amount\":"+payRecordByPayno.getTotalPrice()+"," +
                "    \"subject\":\""+payRecordByPayno.getOrderName()+"\"," +
                "    \"product_code\":\"QUICK_WAP_WAY\"" +
                "  }");//填充业务参数
        String form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单,响应
        httpResponse.setContentType("text/html;charset=" + AlipayConfig.CHARSET);
        httpResponse.getWriter().write(form);//直接将完整的表单html输出到页面
        httpResponse.getWriter().flush();
    }
    @ApiOperation("查询支付结果")
    @GetMapping("/payresult")
    @ResponseBody
    public PayRecordDto payresult(String payNo) throws IOException {

        //查询支付结果


        //当支付成功之后,更新支付记录表的支付状态及订单表的状态为支付成功
        return orderService.queryPayResult(payNo);

    }
}
