package tang.auth.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import tang.ucenter.model.po.XcUser;
import tang.ucenter.service.WxAuthService;

import java.io.IOException;

@Controller
@Slf4j
public class WxLoginController {
    @Autowired
    WxAuthService wxAuthService;
    @RequestMapping("/wxLogin")
    public String wxLogin(String code, String state) throws IOException {
        log.debug("微信扫码回调,code:{},state:{}",code,state);
        //todo 请求微信申请令牌，拿到令牌查询用户信息，将用户信息写入本项目数据库
        wxAuthService.WxAuth(code);
        XcUser xcUser = new XcUser();
        //暂时硬编写，目的是调试环境
        xcUser.setUsername("t1");
        if(xcUser==null){
            return "redirect:http://www.51xuecheng.cn/error.html";
        }
        String username = xcUser.getUsername();
        return "redirect:http://www.51xuecheng.cn/sign.html?username="+username+"&authType=wx";
    }

}
