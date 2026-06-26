package com.example.network_logger_lib.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface DemoApi {

    @GET("posts/1")
    suspend fun getSuccess(): Post

    @GET("posts/99999")
    suspend fun getFailure(): Post

    @POST("posts")
    suspend fun createPost(@Body post: NewPost): Post
}

data class Post(
    val id: Int,
    val title: String,
    val body: String,
    val userId: Int,
)

data class NewPost(
    val title: String,
    val body: String,
    val userId: Int,
)
