package tang.ucenter.service.impl;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import tang.ucenter.mapper.XcUserMapper;
import tang.ucenter.mapper.XcUserRoleMapper;
import tang.ucenter.model.dto.AuthParamsDto;
import tang.ucenter.model.dto.XcUserExt;
import tang.ucenter.model.po.XcUser;
import tang.ucenter.model.po.XcUserRole;
import tang.ucenter.service.AuthService;
import tang.ucenter.service.WxAuthService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 微信验证
 */
@Service("wx_authservice")
public class WxAuthServiceImpl implements AuthService, WxAuthService {
    @Value("${weixin.appid}")
    String appid;
    @Value("${weixin.secret}")
    String secret;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    XcUserMapper xcUserMapper;
    @Autowired
    XcUserRoleMapper xcUserRoleMapper;

    @Autowired
    WxAuthServiceImpl currentPorxy;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        //得到账号
        String username = authParamsDto.getUsername();
        //查询数据库
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if(Objects.isNull(xcUser)){
            throw new RuntimeException("用户不存在!");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser,xcUserExt);
        return xcUserExt;
    }

    @Override
    public XcUser WxAuth(String code) {
        //申请令牌
        Map<String, String> accessToken = getAccess_token(code);
        //携带令牌查询用户信息
        String access_token = accessToken.get("access_token");
        String openid = accessToken.get("openid");
        Map<String, String> userinfo = getUserinfo(access_token, openid);
        //保存信息到数据库
        XcUser xcUser = currentPorxy.addWxUser(userinfo);
        return xcUser;
    }

    /**
     * 携带授权码,申请令牌
     *
     * @param code
     * @return {
     * "access_token":"ACCESS_TOKEN",
     * "expires_in":7200,
     * "refresh_token":"REFRESH_TOKEN",
     * "openid":"OPENID",
     * "scope":"SCOPE",
     * "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"
     * }
     * https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code
     */
    private Map<String, String> getAccess_token(String code) {
        String url_template = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
        //最终请求路径
        String url = String.format(url_template, appid, secret, code);
        //远程调用此url
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, null, String.class); //第三个为请求参数
        //获取相应结果
        String result = exchange.getBody();
        //将result转换为map
        Map<String, String> map = JSON.parseObject(result, Map.class);
        return map;
    }

    /**
     * 携带令牌查信息
     * "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
     * @param access_token
     * @param openid
     * @return
     * 获取用户信息，示例如下：
     *  {
     *  "openid":"OPENID",
     *  "nickname":"NICKNAME",
     *  "sex":1,
     *  "province":"PROVINCE",
     *  "city":"CITY",
     *  "country":"COUNTRY",
     *  "headimgurl": "https://thirdwx.qlogo.cn/mmopen/g3MonUZtNHkdmzicIlibx6iaFqAc56vxLSUfpb6n5WKSYVY0ChQKkiaJSgQ1dZuTOgvLLrhJbERQQ4eMsv84eavHiaiceqxibJxCfHe/0",
     *  "privilege":[
     *  "PRIVILEGE1",
     *  "PRIVILEGE2"
     *  ],
     *  "unionid": " o6_bmasdasdsad6_2sgVt7hMZOPfL"
     *  }
     */
    private Map<String,String> getUserinfo(String access_token,String openid) {
        String url_template="https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
        String url = String.format(url_template, access_token, openid);
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
        String body = exchange.getBody();
        //获取相应结果
//        String result = exchange.getBody();//z这样会乱码
        String result=new String(exchange.getBody().getBytes(StandardCharsets.ISO_8859_1),StandardCharsets.UTF_8);
        //将result转换为map
        Map<String, String> map = JSON.parseObject(result, Map.class);
        return map;
    }

    /**
     * 保存用户信息进入数据库
     * @param userInfo_map
     * @return
     */
    @Transactional
    public XcUser addWxUser(Map userInfo_map){
        //根据unionid查询用户信息
        String unionid =(String) userInfo_map.get("unionid");
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getWxUnionid, unionid));
        if(!Objects.isNull(xcUser)){
            return xcUser;
        }
        xcUser=new XcUser();
        String id=UUID.randomUUID().toString();
        xcUser.setId(id);//主键
        xcUser.setUsername(unionid);
        xcUser.setPassword(unionid);
        xcUser.setWxUnionid(unionid);
        xcUser.setNickname((String) userInfo_map.get("nickname"));
        xcUser.setName((String) userInfo_map.get("nickname"));
        xcUser.setUtype("101001");//学生类型
        xcUser.setStatus("1");//用户状态
        xcUser.setCreateTime(LocalDateTime.now());
        xcUserMapper.insert(xcUser);
        XcUserRole xcUserRole = new XcUserRole();
        xcUserRole.setId(UUID.randomUUID().toString());
        xcUserRole.setUserId(id);
        xcUserRole.setRoleId("17");//学生角色
        xcUserRole.setCreateTime(LocalDateTime.now());
        xcUserRoleMapper.insert(xcUserRole);
        return xcUser;
    }
}
