package tang.xuechengplusbase.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(XueChengPlusException.class)
    @ResponseStatus(HttpStatus.INSUFFICIENT_STORAGE) //设置状态返回码 500
    public RestErrorResponse customException(XueChengPlusException xueChengPlusException){
        //打印异常
        log.error("出现了异常!{}",xueChengPlusException);
        //从异常信息中获取提示异常信息封装返回
        return new RestErrorResponse(xueChengPlusException.getMessage());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    //处理validation抛出的异常
    @ResponseStatus(HttpStatus.INSUFFICIENT_STORAGE) //设置状态返回码 500
    public RestErrorResponse customException(MethodArgumentNotValidException methodArgumentNotValidException){
        //打印异常
        log.error("出现了异常!{}",methodArgumentNotValidException);
        //从异常信息中获取提示异常信息封装返回
        BindingResult bindingResult = methodArgumentNotValidException.getBindingResult();
        //存储错误信息,错误信息不止一个
        List<String> errors = bindingResult.getFieldErrors().stream().map(error ->
                        error.getDefaultMessage())
                .collect(Collectors.toList());
        //工具类分割
        String resultError = StringUtils.join(errors, ",");
        return new RestErrorResponse(resultError);
    }


    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INSUFFICIENT_STORAGE)
    public RestErrorResponse customException(Exception e){
        //打印异常
        log.error("出现了异常!{}",e);
        //从异常信息中获取提示异常信息封装返回
        return new RestErrorResponse(CommonError.UNKOWN_ERROR.getErrMessage());
    }
}
