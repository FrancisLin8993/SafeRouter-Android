package com.example.saferouter.network;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface RoutingAlgorithmApiInterface {
    String RoutingAlgorithm_URL = "http://Saferouter-env.7p2ipt5mmc.us-east-2.elasticbeanstalk.com/";
    @Headers("Content-Type: application/json")
    @POST("api")
    Call<ResponseBody> getRoutingAlgorithm(@Body RequestBody params);
}
