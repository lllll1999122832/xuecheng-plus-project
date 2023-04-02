package tang.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tang.ucenter.feignClient.CheckCodeClient;
import tang.ucenter.mapper.XcUserMapper;
import tang.ucenter.model.dto.AuthParamsDto;
import tang.ucenter.model.dto.XcUserExt;
import tang.ucenter.model.po.XcUser;
import tang.ucenter.service.AuthService;

import java.util.Objects;

/**
 * password验证
 */
@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {
    @Autowired
    XcUserMapper xcUserMapper;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    CheckCodeClient checkCodeClient;
    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        //todo 校验验证码 远程调用验证码接口去验证验证码
        //前端输入的验证码
        String checkcode = authParamsDto.getCheckcode();
        //验证码对应的key
        String checkcodekey = authParamsDto.getCheckcodekey();
        if(StringUtils.isEmpty(checkcode)||StringUtils.isEmpty(checkcodekey)){
            throw new RuntimeException("输入验证码为空!");
        }
        Boolean verify = checkCodeClient.verify(checkcodekey, checkcode);
        if(Objects.isNull(verify)||!verify){
            throw new RuntimeException("验证码输入错误!");
        }
        //根据username查询数据库
        LambdaQueryWrapper<XcUser> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(XcUser::getUsername,authParamsDto.getUsername());
        XcUser xcUser = xcUserMapper.selectOne(lambdaQueryWrapper);
        //查询数据库不存在,返回NUll就可以,springSecurity框架抛出异常用户不存在
        if(Objects.isNull(xcUser)){
//            return null;
            throw new RuntimeException("账号不存在!");
        }
        //验证密码是否正确
        //假如查到用户,就拿到正确的密码,最终封装成UserDetail给SpringSecurity框架进行返回,由于框架进行密码比对
       //输入的密码
        String password = xcUser.getPassword();
        //用户输入的密码
        String dtoPassword = authParamsDto.getPassword();
        boolean matches = passwordEncoder.matches(dtoPassword, password);
        if(!matches){
            throw new RuntimeException("账号不存在或者密码错误!");
        }
        XcUserExt xcUserExt=new XcUserExt();
        BeanUtils.copyProperties(xcUser,xcUserExt);
        return xcUserExt;
    }
}
