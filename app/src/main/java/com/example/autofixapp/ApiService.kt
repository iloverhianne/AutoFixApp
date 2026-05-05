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

    @GET("api-mobile.php")
    fun getGarage(
        @Query("action") action: String,
        @Query("customer_id") customerId: String,
        @Query("tid") tenantId: String
    ): Call<GarageResponse>

    @GET("api-mobile.php")
    fun getServices(
        @Query("action") action: String,
        @Query("tid") tenantId: String
    ): Call<ServiceResponse>

    @FormUrlEncoded
    @POST("api-mobile.php")
    fun bookAppointment(
        @Query("action") action: String,
        @Query("tid") tenantId: String,
        @Field("customer_id") customerId: String,
        @Field("service_id") serviceId: String,
        @Field("vehicle_id") vehicleId: String,
        @Field("date") date: String,
        @Field("time") time: String,
        @Field("estimate") estimate: String,
        @Field("mechanic_id") mechanicId: String? = null,
        @Field("bay_id") bayId: String? = null
    ): Call<BaseResponse>

    @GET("api-mobile.php")
    fun getAvailability(
        @Query("action") action: String,
        @Query("tid") tenantId: String
    ): Call<AvailabilityResponse>

    @GET("api-mobile.php")
    fun getMechanicsAndBays(
        @Query("action") action: String,
        @Query("tid") tenantId: String
    ): Call<MechanicsBaysResponse>

    @GET("api-mobile.php")
    fun getAvailableMechanics(
        @Query("action") action: String,
        @Query("tid") tenantId: String,
        @Query("date") date: String,
        @Query("time") time: String
    ): Call<MechanicsBaysResponse>

    @FormUrlEncoded
    @POST("api-mobile.php")
    fun trackRepair(
        @Query("action") action: String,
        @Query("tid") tenantId: String,
        @Field("job_id") jobId: String,
        @Field("customer_id") customerId: String
    ): Call<TrackingResponse>

    @GET("api-mobile.php")
    fun getHistory(
        @Query("action") action: String,
        @Query("tid") tenantId: String,
        @Query("customer_id") customerId: String,
        @Query("email") email: String = ""
    ): Call<HistoryResponse>

    @FormUrlEncoded
    @POST("api-mobile.php")
    fun addVehicle(
        @Query("action") action: String,
        @Field("tid") tid: String,
        @Field("customer_id") customerId: String,
        @Field("plate_no") plateNo: String,
        @Field("make") make: String,
        @Field("model") model: String,
        @Field("year") year: String
    ): Call<BaseResponse>

    @FormUrlEncoded
    @POST("api-mobile.php")
    fun deleteVehicle(
        @Query("action") action: String,
        @Field("tid") tid: String,
        @Field("vehicle_id") vehicleId: String,
        @Field("customer_id") customerId: String
    ): Call<BaseResponse>

    @GET("api-mobile.php")
    fun getLoyaltyStatus(
        @Query("action") action: String,
        @Query("tid") tenantId: String,
        @Query("customer_id") customerId: String
    ): Call<LoyaltyResponse>

    @FormUrlEncoded
    @POST("api-mobile.php")
    fun submitReview(
        @Query("action") action: String,
        @Query("tid") tenantId: String,
        @Field("job_id") jobId: String,
        @Field("rating") rating: Int,
        @Field("comment") comment: String
    ): Call<BaseResponse>

    @GET("api-mobile.php")
    fun getMessages(
        @Query("action") action: String,
        @Query("tid") tenantId: String,
        @Query("customer_id") customerId: String
    ): Call<ChatResponse>

    @FormUrlEncoded
    @POST("api-mobile.php")
    fun sendMessage(
        @Query("action") action: String,
        @Query("tid") tenantId: String,
        @Field("customer_id") customerId: String,
        @Field("message") message: String
    ): Call<BaseResponse>

    @FormUrlEncoded
    @POST("api-mobile.php")
    fun createPaymentIntent(
        @Query("action") action: String,
        @Query("tid") tenantId: String,
        @Field("customer_id") customerId: String,
        @Field("amount") amount: String,
        @Field("type") type: String
    ): Call<PaymentIntentResponse>

    @FormUrlEncoded
    @POST("api-mobile.php")
    fun recordPayment(
        @Query("action") action: String,
        @Query("tid") tenantId: String,
        @Field("customer_id") customerId: String,
        @Field("amount") amount: String,
        @Field("type") type: String,
        @Field("method") method: String,
        @Field("ref_id") refId: String? = null
    ): Call<BaseResponse>

    @GET("api-mobile.php")
    fun getBookedSlots(
        @Query("action") action: String,
        @Query("tid") tenantId: String,
        @Query("date") date: String
    ): Call<BookedSlotsResponse>
    
    @GET("api-mobile.php")
    fun getSchedules(
        @Query("action") action: String,
        @Query("tid") tenantId: String,
        @Query("date") date: String,
        @Query("service_id") serviceId: String
    ): Call<SchedulesResponse>
}

// All response classes with Nullable types and Default Values for GSON stability

data class PaymentIntentResponse(
    val status: String? = null,
    val checkout_url: String? = null,
    val transaction_id: String? = null
)

data class BookedSlotsResponse(
    val status: String? = null,
    val booked_slots: List<String>? = null
)

data class ScheduleSlot(
    val schedule_id: Int? = null,
    val time_slot_id: Int? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val display_time: String? = null,
    val available_mechanics_count: Int? = null,
    val time_range: String? = null
)

data class SchedulesResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val message: String? = null,
    val time_slots: List<ScheduleSlot>? = null,
    val schedules: List<ScheduleSlot>? = null
)

data class ServiceResponse(
    val status: String? = null,
    val data: List<Service>? = null
)

data class GarageResponse(
    val status: String? = null,
    val data: List<Vehicle>? = null
)

data class LoyaltyResponse(
    val status: String? = null,
    val points: Int? = null,
    @com.google.gson.annotations.SerializedName("member_level") val tier: String? = null,
    @com.google.gson.annotations.SerializedName("next_tier") val nextTier: String? = null,
    val available_promos: List<Promo>? = null
)

data class ChatResponse(
    val status: String? = null,
    val data: List<Message>? = null
)

data class HistoryResponse(
    val status: String? = null,
    val message: String? = null,
    val repairs: List<RepairHistory>? = null, 
    val bookings: List<RepairHistory>? = null,
    val services: List<RepairHistory>? = null,
    val payments: List<PaymentHistory>? = null
)

data class TrackingResponse(
    val status: String? = null,
    val jobInfo: JobInfo? = null,
    val timeline: List<TimelineItem>? = null
)

data class BaseResponse(
    val status: String? = null,
    val message: String? = null,
    val appointment_id: String? = null
)

data class Service(
    val service_id: String? = null,
    val service_name: String? = null,
    val description: String? = null,
    val price: String? = null,
    val icon_url: String? = null
)

data class Vehicle(
    val vehicle_id: String? = null,
    val plate_no: String? = null,
    val make: String? = null,
    val model: String? = null,
    @com.google.gson.annotations.SerializedName("year_model") val year: String? = null,
    val last_service_date: String? = null,
    val active_jobs: Int? = 0
)

data class Message(
    val message_id: String? = null,
    val sender_type: String? = null,
    val content: String? = null,
    val created_at: String? = null
)

data class Promo(
    val promo_id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val discount: String? = null
)

data class RepairHistory(
    val job_id: String? = null,
    val plate_no: String? = null,
    val service_name: String? = "General Repair",
    val status: String? = null,
    val total_amount: String? = null,
    val paid_amount: String? = "0.00",
    @com.google.gson.annotations.SerializedName("date") val date: String? = null,
    @com.google.gson.annotations.SerializedName("time") val time: String? = null,
    val created_at: String? = null,
    val rating: Int? = null
)

data class PaymentHistory(
    val amount: String? = null,
    val payment_method: String? = null,
    val payment_type: String? = null,
    val method: String? = null,
    val type: String? = null,
    val status: String? = null,
    val date: String? = null,
    val time: String? = null,
    val created_at: String? = null,
    val plate_no: String? = null,
    val service_name: String? = null,
    val appointment_id: String? = null,
    val ref_id: String? = null
)

data class JobInfo(
    val status: String? = null,
    val total_amount: String? = null,
    val notes: String? = null,
    val plate_no: String? = null,
    val make: String? = null,
    val model: String? = null
)

data class BookingInitResponse(
    val status: String? = null,
    val vehicles: List<Vehicle>? = null,
    val services: List<Service>? = null,
    val mechanics: List<Mechanic>? = null,
    val bays: List<Bay>? = null
)

data class AvailabilityResponse(
    val status: String? = null,
    val available_mechanics: String? = null,
    val available_bays: String? = null,
    val waiting_time: String? = null,
    val active_jobs: String? = null
)

data class MechanicsBaysResponse(
    val status: String? = null,
    val mechanics: List<Mechanic>? = null,
    val bays: List<Bay>? = null
)

data class Mechanic(
    val mechanic_id: String? = null,
    val full_name: String? = null,
    val specialization: String? = null,
    val avatar_url: String? = null
)

data class Bay(
    val bay_id: String? = null,
    val bay_name: String? = null
)

data class TimelineItem(
    val status_update: String? = null,
    val remarks: String? = null,
    val created_at: String? = null,
    val inspection_photo: String? = null
)
