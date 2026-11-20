package com.linlijiewu.app.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Body;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import okhttp3.MultipartBody;

public interface LinliApi {
    @GET("hello")
    Call<ApiResponse<HelloData>> hello();
    @POST("auth/register")
    Call<ApiResponse<AuthData>> register(@Body AuthData.Register request);
    @POST("auth/login")
    Call<ApiResponse<AuthData>> login(@Body AuthData.Login request);
    @GET("users/me")
    Call<ApiResponse<AuthData.User>> me();
    @GET("items/{id}")
    Call<ApiResponse<ItemData>> item(@Path("id") long id);
    @POST("items")
    Call<ApiResponse<ItemData>> publish(@Body ItemData.Write request);
    @PUT("items/{id}")
    Call<ApiResponse<ItemData>> edit(@Path("id") long id, @Body ItemData.Write request);
    @POST("items/{id}/offline")
    Call<ApiResponse<ItemData>> offline(@Path("id") long id);
    @GET("users/me/items")
    Call<ApiResponse<ItemData.Page>> mine(@Query("page") int page, @Query("size") int size);
    @Multipart
    @POST("images")
    Call<ApiResponse<ItemData.Uploaded>> upload(@Part MultipartBody.Part file);
}
