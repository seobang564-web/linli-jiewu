package com.linlijiewu.server.common;

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(Throwable cause) {
        super("数据库连接或基础表未就绪", cause);
    }
}
