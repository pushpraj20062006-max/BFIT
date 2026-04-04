package com.example.bfit.network

import com.google.gson.annotations.SerializedName

data class FoodResponse(
    val product: Product?
)

data class Product(
    @SerializedName("product_name")
    val productName: String?,
    val nutriments: Nutriments?
)

data class Nutriments(
    @SerializedName("energy-kcal_100g")
    val energyKcal100g: Double?,
    @SerializedName("proteins_100g")
    val proteins_100g: Double?,
    @SerializedName("carbohydrates_100g")
    val carbohydrates_100g: Double?,
    @SerializedName("fat_100g")
    val fat_100g: Double?
)
