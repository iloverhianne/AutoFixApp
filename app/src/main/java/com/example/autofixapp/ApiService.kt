package com.example.autofixapp

import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @FormUrlEncoded
    @POST("api-mobile.php")
    fun login(
        @Query("action") action: String,
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    // 2.5.2 Service Appointment Booking
    @FormUrlEncoded
    @POST("api-mobile.php")
    fun getServices(
        @Field("action") action: String = "get_services",
        @Field("tid") tenantId: String
    ): Call<ServiceResponse>

    @FormUrlEncoded
    @POST("api-mobile.php")
    fun bookAppointment(
        @Query("action") action: String = "book_appointment",
        @Query("tid") tenantId: String,
        @Field("customer_id") customerId: String,
        @Field("service_id") serviceId: String,
        @Field("vehicle_id") vehicleId: String = "0",
        @Field("date") date: String,
        @Field("time") time: String,
        @Field("estimate") estimate: String,
        @Field("mechanic_id") mechanicId: String? = null,
        @Field("bay_id") bayId: String? = null
    ): Call<BaseResponse>

    @FormUrlEncoded
    @POST("api-mobile.php")
    fun getAvailability(
        @Field("action") action: String = "get_availability",
        @Field("tid") tenantId: String
    ): Call<AvailabilityResponse>

    @FormUrlEncoded
    @POST("api-mobile.php")
    fun getMechanicsAndBays(
        @Field("action") action: String = "get_mechanics_and_bays",
        @Field("tid") tenantId: String
    ): Call<MechanicsBaysResponse>

    // 2.5.4 Repair Status Tracking
    @FormUrlEncoded
    @POST("api-mobile.php")
    fun trackRepair(
        @Field("action") action: String = "track_repair",
        @Field("tid") tenantId: String,
        @Field("job_id") jobId: String,
        @Field("customer_id") customerId: String
    ): Call<TrackingResponse>

    // 2.5.5 Service & Payment History
    @FormUrlEncoded
    @POST("api-mobile.php")
    fun getHistory(
        @Field("action") action: String = "history",
        @Field("tid") tenantId: String,
        @Field("customer_id") customerId: String
    ): Call<HistoryResponse>
}

// Response Wrappers to match PHP structure
data class ServiceResponse(
    val status: String,
    val data: List<Service>
)

data class HistoryResponse(
    val status: String,
    val repairs: List<RepairHistory>,
    val payments: List<PaymentHistory>
)

data class TrackingResponse(
    val status: String,
    val jobInfo: JobInfo,
    val timeline: List<TimelineItem>
)

data class BaseResponse(
    val status: String,
    val message: String,
    val appointment_id: String? = null
)

// Entities
data class Service(
    val service_id: String,
    val service_name: String,
    val description: String,
    val price: String
)

data class RepairHistory(
    val job_id: String,
    val plate_no: String,
    val status: String,
    val total_amount: String,
    val date: String
)

data class PaymentHistory(
    val amount: String,
    val payment_method: String,
    val payment_type: String,
    val status: String,
    val date: String
)

data class JobInfo(
    val status: String,
    val total_amount: String,
    val notes: String?,
    val plate_no: String,
    val make: String,
    val model: String
)

data class AvailabilityResponse(
    val status: String,
    val available_mechanics: Int,
    val available_bays: Int,
    val waiting_time: String,
    val active_jobs: Int
)

data class MechanicsBaysResponse(
    val status: String,
    val mechanics: List<Mechanic>,
    val bays: List<Bay>
)

data class Mechanic(
    val mechanic_id: String,
    val full_name: String,
    val specialization: String?
)

data class Bay(
    val bay_id: String,
    val bay_name: String
)

data class TimelineItem(
    val status_update: String,
    val remarks: String?,
    val created_at: String
)
