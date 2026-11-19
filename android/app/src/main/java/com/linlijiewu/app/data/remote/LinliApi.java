package com.linlijiewu.app.data.remote;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import okhttp3.MultipartBody;

public interface LinliApi {
    @GET("hello")
    Call<ApiResponse<HelloData>> hello();

    /** 附近检索 GET /items 由方舒煜第 3 周接入。 */

    @GET("items/{id}")
    Call<ApiResponse<ItemDetail>> itemDetail(@Path("id") long id);

    /** 🔒 发布物品（需登录）。 */
    @POST("items")
    Call<ApiResponse<ItemDetail>> publishItem(@Body PublishItemRequest request);

    /** 🔒 上传图片（需登录），multipart 字段名 file，jpg/png/webp ≤5MB。 */
    @Multipart
    @POST("images")
    Call<ApiResponse<ImageUploadData>> uploadImage(@Part MultipartBody.Part file);
}
