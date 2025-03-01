package com.example.ssul.repository

import com.example.ssul.api.RetrofitClient
import com.example.ssul.api.collegeCodeMap
import com.example.ssul.api.degreeCodeMap
import com.example.ssul.model.StoreInfoModel
import com.example.ssul.model.StoreModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StoreRepository {
    suspend fun getStores(college: String, degree: String): Result<MutableList<StoreModel>> =
        withContext(Dispatchers.IO) {
            val collegeCode = collegeCodeMap[college]
                ?: return@withContext Result.failure(IllegalArgumentException("Unknown college name: $college"))
            val degreeCode = degreeCodeMap[degree]
                ?: return@withContext Result.failure(IllegalArgumentException("Unknown degree name: $degree"))

            runCatching {
                val response = RetrofitClient.apiService.getStores(collegeCode, degreeCode).execute()
                if (response.isSuccessful) {
                    val storeResponses = response.body() ?: emptyList()
                    storeResponses.map { storeResponse ->
                        StoreModel(
                            id = storeResponse.id,
                            name = storeResponse.name,
                            address = storeResponse.address,
                            isFilterGroupChecked = storeResponse.themes.contains("THE-001"),
                            isFilterDateChecked = storeResponse.themes.contains("THE-002"),
                            isFilterEfficiencyChecked = storeResponse.themes.contains("THE-003"),
                            imageUrl = storeResponse.imageUrl,
                            isAssociated = storeResponse.isAssociated
                        )
                    }.toMutableList()
                } else {
                    throw Exception("Failed to fetch stores: ${response.errorBody()?.string()}")
                }
            }
        }

    suspend fun getStoreInfo(storeId: Int, college: String, degree: String): Result<StoreInfoModel> =
        withContext(Dispatchers.IO) {
            val collegeCode = collegeCodeMap[college]
                ?: return@withContext Result.failure(IllegalArgumentException("Unknown college name: $college"))
            val degreeCode = degreeCodeMap[degree]
                ?: return@withContext Result.failure(IllegalArgumentException("Unknown degree name: $degree"))

            runCatching {
                val response = RetrofitClient.apiService.getStoreInfo(storeId, collegeCode, degreeCode).execute()
                if (response.isSuccessful) {
                    val storeResponse = response.body() ?: throw IllegalArgumentException("Invalid Store Response")
                    val associationTarget = collegeCodeMap.entries.find { it.value == storeResponse.associationInfo?.target }?.key
                        ?: degreeCodeMap.entries.find { it.value == storeResponse.associationInfo?.target }?.key
                        ?: "정보 없음"

                    StoreInfoModel(
                        id = storeResponse.id,
                        name = storeResponse.name,
                        isAssociated = storeResponse.isAssociated,
                        address = storeResponse.address,
                        contact = storeResponse.contact ?: "정보 없음",
                        associationInfo = associationTarget to (storeResponse.associationInfo?.description ?: "정보 없음"),
                        menus = storeResponse.menus.map { menu ->
                            StoreInfoModel.MenuItem(
                                name = menu.name,
                                price = menu.price,
                                imageUrl = menu.imageUrl ?: ""
                            )
                        }
                    )
                } else {
                    throw IllegalArgumentException("Failed to fetch store info: ${response.errorBody()?.string()}")
                }
            }
        }
}
