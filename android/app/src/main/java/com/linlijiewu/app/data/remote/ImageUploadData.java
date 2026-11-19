package com.linlijiewu.app.data.remote;

/**
 * 🔒 POST /images 响应：服务端重新编码并随机命名后的图片标识与访问地址。
 */
public final class ImageUploadData {
    private String imageId;
    private String url;

    public String getImageId() {
        return imageId;
    }

    public String getUrl() {
        return url;
    }
}
