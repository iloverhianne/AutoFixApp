package com.example.autofixapp

import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @FormUrlEncoded
    @POST("api-mobile.php")
    fun login(
        @Query("action") action: String = "login",
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    // 2.5.2 Service Appointment Booking
    @GET("api-mobile.php")
    fun getServices(
        @Query("action") action: String = "get_services",
        @Query("tid") tenantId: String
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

    @GET("api-mobile.php")
    fun getAvailability(
        @Query("action") action: String = "get_availability",
        @Query("tid") tenantId: String
    ): Call<AvailabilityResponse>

    @GET("api-mobile.php")
    fun getMechanicsAndBays(
        @Query("action") action: String = "get_mechanics_and_bays",
        @Query("tid") tenantId: String
    ): Call<MechanicsBaysResponse>

    // 2.5.4 Repair Status Tracking
    @FormUrlEncoded
    @POST("api-mobile.php")
    fun trackRepair(
        @Query("action") action: String = "track_repair",
        @Query("tid") tenantId: String,
        @Field("job_id") jobId: String,
        @Field("customer_id") customerId: String
    ): Call<TrackingResponse>

    // 2.5.5 Service & Payment History
    @GET("api-mobile.php")
    fun getHistory(
        @Query("action") action: String = "get_history",
        @Query("tid") tenantId: String,
        @Query("customer_id") customerId: String
    ): Call<HistoryResponse>

    // 2.5.6 Garage / Vehicle Management
    @GET("api-mobile.php")
    fun getGarage(
        @Query("action") action: String = "get_garage",
        @Query("tid") tenantId: String,
        @Query("customer_id") customerId: String
    ): Call<GarageResponse>

    @FormUrlEncoded
    @POST("api-mobile.php")
    fun addVehicle(
        @Query("action") action: String = "add_vehicle",
        @Query("tid") tid: String,
        @Field("customer_id") customerId: String,
        @Field("plate_no") plateNo: String,
        @Field("make") make: String,
        @Field("model") model: String,
        @Field("year") year: String
    ): Call<BaseResponse>

    @FormUrlEncoded
    @POST("api-mobile.php")
    fun deleteVehicle(
        @Query("action") action: String = "remove_vehicle",
        @Query("tid") tid: String,
        @Field("customer_id") customerId: String,
        @Field("vehicle_id") vehicleId: String
    ): Call<BaseResponse>

    // 2.5.7 Loyalty & Rewards
    @GET("api-mobile.php")
    fun getLoyaltyStatus(
        @Query("action") action: String = "loyalty_status",
        @Query("tid") tenantId: String,
        @Query("customer_id") customerId: String
    ): Call<LoyaltyResponse>

    // 2.5.8 Reviews & Ratings
    @FormUrlEncoded
    @POST("api-mobile.php")
    fun submitReview(
        @Query("action") action: String = "submit_review",
        @Query("tid") tenantId: String,
        @Field("job_id") jobId: String,
        @Field("rating") rating: Int,
        @Field("comment") comment: String
    ): Call<BaseResponse>

    // 2.5.9 Chat & Support
    @GET("api-mobile.php")
    fun getMessages(
        @Query("action") action: String = "get_messages",
        @Query("tid") tenantId: String,
        @Query("customer_id") customerId: String
    ): Call<ChatResponse>

    @FormUrlEncoded
    @POST("api-mobile.php")
    fun sendMessage(
        @Query("action") action: String = "send_message",
        @Query("tid") tenantId: String,
        @Field("customer_id") customerId: String,
        @Field("message") message: String
    ): Call<BaseResponse>

    @FormUrlEncoded
    @POST("api-mobile.php")
    fun createPaymentIntent(
        @Query("action") action: String = "create_payment_intent",
        @Query("tid") tenantId: String,
        @Field("customer_id") customerId: String,
        @Field("amount") amount: String,
        @Field("type") type: String
    ): Call<PaymentIntentResponse>

    @FormUrlEncoded
    @POST("api-mobile.php")
    fun recordPayment(
        @Query("action") action: String = "record_payment",
        @Query("tid") tenantId: String,
        @Field("customer_id") customerId: String,
        @Field("amount") amount: String,
        @Field("type") type: String,
        @Field("method") method: String,
        @Field("ref_id") refId: String? = null
    ): Call<BaseResponse>

    @GET("api-mobile.php")
    fun getBookedSlots(
        @Query("action") action: String = "get_booked_slots",
        @Query("tid") tenantId: String,
        @Query("date") date: String
    ): Call<BookedSlotsResponse>
}

data class PaymentIntentResponse(
    val status: String,
    val checkout_url: String,
    val transaction_id: String
)

data class BookedSlotsResponse(
    val status: String,
    val booked_slots: List<String>
)

// Response Wrappers
data class ServiceResponse(
    val status: String,
    val data: List<Service>
)

data class GarageResponse(
    val status: String,
    val data: List<Vehicle>
)

data class LoyaltyResponse(
    val status: String,
    val points: Int,
    @com.google.gson.annotations.SerializedName("member_level") val tier: String?,
    @com.google.gson.annotations.SerializedName("next_tier") val nextTier: String?,
    val available_promos: List<Promo>?
)

data class ChatResponse(
    val status: String,
    val data: List<Message>
)

data class HistoryResponse(
    val status: String,
    val message: String? = null,
    val repairs: List<RepairHistory>, // For backward compatibility
    val bookings: List<RepairHistory>? = null,
    val services: List<RepairHistory>? = null,
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
    val service_id: String?,
    val service_name: String?,
    val description: String?,
    val price: String?,
    val icon_url: String? = null
)

data class Vehicle(
    val vehicle_id: String?,
    val plate_no: String?,
    val make: String?,
    val model: String?,
    @com.google.gson.annotations.SerializedName("year_model")
    val year: String?,
    val last_service_date: String? = null,
    val active_jobs: Int? = 0
)

data class Message(
    val message_id: String?,
    val sender_type: String?, // 'customer' or 'shop'
    val content: String?,
    val created_at: String?
)

data class Promo(
    val promo_id: String?,
    val title: String?,
    val description: String?,
    val discount: String?
)

data class RepairHistory(
    val job_id: String?,
    val plate_no: String?,
    val service_name: String? = "General Repair",
    val status: String?,
    val total_amount: String?,
    val paid_amount: String? = "0.00",
    @com.google.gson.annotations.SerializedName("date", alternate = ["appointment_date", "scheduled_date"])
    val date: String?,
    @com.google.gson.annotations.SerializedName("time", alternate = ["appointment_time", "scheduled_time", "service_time", "booking_time"])
    val time: String? = null,
    val created_at: String? = null,
    val rating: Int? = null // For reviews
)

data class PaymentHistory(
    val amount: String?,
    val payment_method: String?,
    val payment_type: String?,
    val status: String?,
    val date: String?,
    val time: String? = null,
    val created_at: String? = null,
    val plate_no: String? = null,
    val service_name: String? = null
)

data class JobInfo(
    val status: String?,
    val total_amount: String?,
    val notes: String?,
    val plate_no: String?,
    val make: String?,
    val model: String?
)

data class AvailabilityResponse(
    val status: String?,
    val available_mechanics: Any?, // Flexible for String or Int
    val available_bays: Any?,
    val waiting_time: String?,
    val active_jobs: Any?
)

data class MechanicsBaysResponse(
    val status: String?,
    val mechanics: List<Mechanic>?,
    val bays: List<Bay>?
)

data class Mechanic(
    val mechanic_id: String?,
    val full_name: String?,
    val specialization: String?,
    val avatar_url: String? = null
)

data class Bay(
    val bay_id: String?,
    val bay_name: String?
)

data class TimelineItem(
    val status_update: String?,
    val remarks: String?,
    val created_at: String?,
    val inspection_photo: String? = null // For visual updates
)
