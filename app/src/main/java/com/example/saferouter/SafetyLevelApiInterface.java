package com.example.saferouter;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface SafetyLevelApiInterface {
    String BASE_URL = "http://safetyclassifiermodel-env.nbr93wijua.us-east-2.elasticbeanstalk.com/";

    @Headers("Content-Type: application/json")
    @POST("api")
    Call<ResponseBody> getSafetyLevel(@Body RequestBody params);
}
