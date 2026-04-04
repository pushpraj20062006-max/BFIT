package com.example.bfit.network

import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApiService {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getFoodData(@Path("barcode") barcode: String): FoodResponse
}
