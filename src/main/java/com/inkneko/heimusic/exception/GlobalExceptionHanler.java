package com.inkneko.heimusic.exception;

import com.inkneko.heimusic.model.vo.Response;
import com.inkneko.heimusic.util.lrclib.LrclibRateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHanler {

    Logger logger = LoggerFactory.getLogger(GlobalExceptionHanler.class);

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    public Response<?> serviceExceptionHandler(ServiceException e){
        return new Response<>(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    @ResponseBody
    public Response<?> badSqlGrammarExceptionHandler(BadSqlGrammarException e){
        logger.error("controller层截获到sql错误", e);
        return new Response<>(500, "服务内部错误");
    }

    /**
     * LRCLIB 限流异常的处理（手动拉取歌词时可能触发），提示调用方稍后重试
     * @param e 限流异常
     * @return 业务输出
     */
    @ExceptionHandler(LrclibRateLimitException.class)
    @ResponseBody
    public Response<?> lrclibRateLimitExceptionHandler(LrclibRateLimitException e){
        logger.warn("LRCLIB请求被限流，Retry-After：{}秒", e.getRetryAfterSeconds());
        return new Response<>(429, "LRCLIB 请求过于频繁，请稍后重试");
    }

    /**
     * 对 @Validated 校验异常的处理，将其转换为业务输出统一结构
     * @param e 校验异常
     * @return 业务输出
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public Response<?> validationExceptionHandler(MethodArgumentNotValidException e){
        List<FieldError> fieldErrors = e.getFieldErrors();
        String errorMsg = fieldErrors.stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining(", "));
        return new Response<>(400, errorMsg);
    }


}
