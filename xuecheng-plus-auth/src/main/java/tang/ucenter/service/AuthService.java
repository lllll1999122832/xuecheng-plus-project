package tang.ucenter.service;

import tang.ucenter.model.dto.AuthParamsDto;
import tang.ucenter.model.dto.XcUserExt;

/**
 * 统一认证接口
 */
public interface AuthService {

    XcUserExt execute(AuthParamsDto authParamsDto);
}
