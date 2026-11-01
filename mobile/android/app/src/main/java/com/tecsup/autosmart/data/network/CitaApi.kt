
package com.tecsup.autosmart.data.network

import com.tecsup.autosmart.data.model.CitaRequest
import com.tecsup.autosmart.data.model.CitaCreadaResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface CitaApi {

    @POST("appointments")
    suspend fun crearCita(
        @Body request: CitaRequest
    ): Response<CitaCreadaResponse>
}
