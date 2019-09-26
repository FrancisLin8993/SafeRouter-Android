package com.example.saferouter.network;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * The interface for network request for safety level using retrofit library
 */
public interface SafetyLevelApiInterface {
    //String BASE_URL = "http://safetyclassifiermodel-env.nbr93wijua.us-east-2.elasticbeanstalk.com/";
    String BASE_URL = "http://safetylevelcategorisation.ap-southeast-2.elasticbeanstalk.com/";

    @Headers("Content-Type: application/json")
    @POST("api")
    Call<ResponseBody> getSafetyLevel(@Body RequestBody params);
}
