package com.linlijiewu.server.common;

public class BusinessException extends RuntimeException {
    private final int code;
    private final int status;

    public BusinessException(int status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int getCode() { return code; }
    public int getStatus() { return status; }
    public static BusinessException badRequest(String message) {
        return new BusinessException(400, 40001, message);
    }
    public static BusinessException unauthorized() {
        return new BusinessException(401, 40101, "请先登录或重新登录");
    }
    public static BusinessException forbidden() {
        return new BusinessException(403, 40301, "无权操作此资源");
    }
    public static BusinessException notFound() {
        return new BusinessException(404, 40401, "资源不存在或已下架");
    }
    public static BusinessException conflict(String message) {
        return new BusinessException(409, 40901, message);
    }
}
