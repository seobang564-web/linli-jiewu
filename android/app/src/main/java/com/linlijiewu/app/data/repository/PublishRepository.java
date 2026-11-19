package com.linlijiewu.app.data.repository;

import androidx.annotation.NonNull;
import com.linlijiewu.app.data.remote.ApiResponse;
import com.linlijiewu.app.data.remote.ImageUploadData;
import com.linlijiewu.app.data.remote.ItemDetail;
import com.linlijiewu.app.data.remote.LinliApi;
import com.linlijiewu.app.data.remote.PublishItemRequest;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;

/**
 * 发布流程：可选上传压缩后的图片 → 创建物品。调用方负责线程切换与错误提示。
 */
public class PublishRepository {

    private final LinliApi api;

    public PublishRepository(@NonNull LinliApi api) {
        this.api = api;
    }

    /** 上传压缩后的 JPEG，返回 imageId。 */
    public Call<ApiResponse<ImageUploadData>> uploadImage(@NonNull byte[] jpeg) {
        RequestBody body = RequestBody.create(jpeg, MediaType.parse("image/jpeg"));
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", "publish.jpg", body);
        return api.uploadImage(part);
    }

    /** 创建物品；imageId 为 null 表示无图发布。 */
    public Call<ApiResponse<ItemDetail>> publishItem(@NonNull PublishItemRequest request) {
        return api.publishItem(request);
    }
}
