package com.example.appconitag;

import java.util.Date;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface apiService {

    @Multipart
    @POST("api/MobileApi/assets")
    Call<ResponseBody> uploadAsset(
            @Part("AssetTag") RequestBody assetTag,
            @Part("AssetName") RequestBody assetName,
            @Part("Room") RequestBody room,
            @Part("Condition") RequestBody condition,
            @Part("Location") RequestBody location,
            @Part("Notes") RequestBody notes,
            @Part MultipartBody.Part Picture
    );
}
