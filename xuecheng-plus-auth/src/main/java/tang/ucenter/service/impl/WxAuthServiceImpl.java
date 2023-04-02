package tang.ucenter.service.impl;

import org.springframework.stereotype.Service;
import tang.ucenter.model.dto.AuthParamsDto;
import tang.ucenter.model.dto.XcUserExt;
import tang.ucenter.service.AuthService;

/**
 * 微信验证
 */
@Service("wx_authservice")
public class WxAuthServiceImpl implements AuthService {
    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        return null;
    }
}
