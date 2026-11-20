package com.imista.forward.shared.network

import com.imista.forward.shared.model.NotificationPayload
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RelayRepository(
    private val baseUrl: String
) {
    private val api: RelayApi by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.NONE }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(baseUrl.ensureEndsWithSlash())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RelayApi::class.java)
    }

    private fun String.ensureEndsWithSlash(): String = if (endsWith("/")) this else "$this/"

    suspend fun register(pairingId: String, deviceId: String, deviceName: String, role: String): Result<RegisterResponse> {
        return try {
            val resp = api.register(RegisterRequest(pairingId, deviceId, deviceName, role))
            if (resp.isSuccessful && resp.body()?.ok == true) Result.success(resp.body()!!)
            else Result.failure(Exception(resp.body()?.error ?: resp.errorBody()?.string() ?: "Register failed"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getPairing(pairingId: String): Result<RegisterResponse> {
        return try {
            val resp = api.getPairing(pairingId)
            if (resp.isSuccessful && resp.body()?.ok == true) Result.success(resp.body()!!)
            else Result.failure(Exception(resp.body()?.error ?: "Pairing not found"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun forward(pairingId: String, deviceId: String, payload: NotificationPayload): Result<ForwardResponse> {
        return try {
            val req = ForwardRequest(pairingId, deviceId, payload)
            val resp = api.forward(pairingId, deviceId, req)
            if (resp.isSuccessful && resp.body()?.ok == true) Result.success(resp.body()!!)
            else Result.failure(Exception(resp.body()?.error ?: resp.errorBody()?.string() ?: "Forward failed"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun poll(pairingId: String, deviceId: String, since: Long, limit: Int = 50): Result<PollResponse> {
        return try {
            val resp = api.poll(pairingId, deviceId, since, limit)
            if (resp.isSuccessful && resp.body()?.ok == true) Result.success(resp.body()!!)
            else Result.failure(Exception(resp.body()?.error ?: "Poll failed"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun health(): Result<Map<String, Any>> {
        return try {
            val resp = api.health()
            if (resp.isSuccessful && resp.body() != null) Result.success(resp.body()!!)
            else Result.failure(Exception("Health check failed"))
        } catch (e: Exception) { Result.failure(e) }
    }
}
