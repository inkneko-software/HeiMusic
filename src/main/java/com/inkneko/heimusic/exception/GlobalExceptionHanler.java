package com.inkneko.heimusic.exception;

import com.inkneko.heimusic.model.vo.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHanler {

    Logger logger = LoggerFactory.getLogger(GlobalExceptionHanler.class);

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    public Response serviceExceptionHandler(ServiceException e){
        return new Response(e);
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    @ResponseBody
    public Response badSqlGrammarExceptionHandler(BadSqlGrammarException e){
        logger.error("controller层截获到sql错误", e);
        return new Response(500, "服务内部错误");
    }

}
