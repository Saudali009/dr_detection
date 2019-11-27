package com.dr.detection.network;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiInterface {

    @FormUrlEncoded
    @POST("detect/")
    Call<JsonObject> sendClassificationRequest(
            @Field("image") String imageHash
    );
}
