package com.tecsup.autosmart.data.network

import com.tecsup.autosmart.data.model.AuthResponse
import com.tecsup.autosmart.data.model.LoginRequest
import com.tecsup.autosmart.data.model.RegisterRequest
import com.tecsup.autosmart.data.model.RegisterResponse
import com.tecsup.autosmart.data.model.GoogleLoginRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import com.tecsup.autosmart.data.model.ForgotPasswordRequest
import com.tecsup.autosmart.data.model.ResetPasswordRequest

interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/google")
    suspend fun loginConGoogle(@Body request: GoogleLoginRequest): Response<AuthResponse>

    @POST("auth/forgot-password")
    suspend fun olvideContrasena(@Body request: ForgotPasswordRequest): Response<Unit>

    @POST("auth/reset-password")
    suspend fun restablecerContrasena(@Body request: ResetPasswordRequest): Response<Unit>
}