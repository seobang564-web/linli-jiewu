package com.linlijiewu.server.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.dao.DataAccessResourceFailureException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ApiResponse.failure(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        var error = exception.getBindingResult().getFieldError();
        String message = error == null ? "参数校验失败" : error.getField() + "：" + error.getDefaultMessage();
        return ResponseEntity.badRequest().body(ApiResponse.failure(40001, message));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestPartException.class,
            HttpMediaTypeNotSupportedException.class,
            org.springframework.web.multipart.MultipartException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadInput(Exception exception) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(40001, "请求参数或格式不正确"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleLargeUpload(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(413).body(ApiResponse.failure(41301, "图片不能超过 5 MB"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethod(HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(405).body(ApiResponse.failure(40001, "请求方法不支持"));
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabase(DataAccessResourceFailureException exception) {
        LOGGER.warn("数据库暂不可用：{}", exception.getClass().getSimpleName());
        return ResponseEntity.status(503).body(ApiResponse.failure(50301, "数据库暂不可用"));
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnavailable(ServiceUnavailableException exception) {
        LOGGER.warn("数据库健康检查失败：{}", exception.getCause().getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(50301, exception.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(40401, "资源不存在"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        LOGGER.error("请求处理失败", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(50000, "服务器内部错误"));
    }
}
