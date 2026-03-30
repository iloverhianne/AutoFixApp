package com.example.autofixapp

import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @FormUrlEncoded
    @POST("api-mobile.php?action=login")
    fun login(
        @Query("action") actionQuery: String,
        @Field("action") actionField: String,
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    // 2.5.2 Service Appointment Booking
    @POST("api-mobile.php?action=get_services")
    fun getServices(
        @Query("tid") tenantId: String
    ): Call<ServiceResponse>

    @FormUrlEncoded
    @POST("api-mobile.php?action=book_appointment")
    fun bookAppointment(
        @Query("action") actionQuery: String,
        @Query("tid") tenantIdQuery: String,
        @Field("action") actionField: String,
        @Field("tid") tenantIdField: String,
        @Field("customer_id") customerId: String,
        @Field("service_id") serviceId: String,
        @Field("vehicle_id") vehicleId: String = "0",
        @Field("date") date: String,
        @Field("time") time: String,
        @Field("estimate") estimate: String,
        @Field("mechanic_id") mechanicId: String? = null,
        @Field("bay_id") bayId: String? = null
    ): Call<BaseResponse>

    @POST("api-mobile.php?action=get_availability")
    fun getAvailability(
        @Query("tid") tenantId: String
    ): Call<AvailabilityResponse>

    @POST("api-mobile.php?action=get_mechanics_and_bays")
    fun getMechanicsAndBays(
        @Query("tid") tenantId: String
    ): Call<MechanicsBaysResponse>

    // 2.5.4 Repair Status Tracking
    @FormUrlEncoded
    @POST("api-mobile.php?action=track_repair")
    fun trackRepair(
        @Query("action") actionQuery: String,
        @Query("tid") tenantIdQuery: String,
        @Field("action") actionField: String,
        @Field("tid") tenantIdField: String,
        @Field("job_id") jobId: String,
        @Field("customer_id") customerId: String
    ): Call<TrackingResponse>

    // 2.5.5 Service & Payment History
    @FormUrlEncoded
    @POST("api-mobile.php?action=get_history")
    fun getHistory(
        @Query("action") actionQuery: String,
        @Query("tid") tenantIdQuery: String,
        @Field("action") actionField: String,
        @Field("tid") tenantIdField: String,
        @Field("customer_id") customerId: String
    ): Call<HistoryResponse>

    // 2.5.6 Garage / Vehicle Management
    @FormUrlEncoded
    @POST("api-mobile.php?action=get_garage")
    fun getGarage(
        @Query("action") actionQuery: String,
        @Query("tid") tenantIdQuery: String,
        @Field("action") actionField: String,
        @Field("tid") tenantIdField: String,
        @Field("customer_id") customerId: String
    ): Call<GarageResponse>

    @FormUrlEncoded
    @POST("api-mobile.php?action=add_vehicle")
    fun addVehicle(
        @Query("action") actionQuery: String,
        @Query("tid") tenantIdQuery: String,
        @Field("action") actionField: String,
        @Field("tid") tenantIdField: String,
        @Field("customer_id") customerId: String,
        @Field("plate_no") plateNo: String,
        @Field("make") make: String,
        @Field("model") model: String,
        @Field("year") year: String
    ): Call<BaseResponse>

    // 2.5.7 Loyalty & Rewards
    @FormUrlEncoded
    @POST("api-mobile.php?action=loyalty_status")
    fun getLoyaltyStatus(
        @Query("action") actionQuery: String,
        @Query("tid") tenantIdQuery: String,
        @Field("action") actionField: String,
        @Field("tid") tenantIdField: String,
        @Field("customer_id") customerId: String
    ): Call<LoyaltyResponse>

    // 2.5.8 Reviews & Ratings
    @FormUrlEncoded
    @POST("api-mobile.php?action=submit_review")
    fun submitReview(
        @Query("action") actionQuery: String,
        @Query("tid") tenantIdQuery: String,
        @Field("action") actionField: String,
        @Field("tid") tenantIdField: String,
        @Field("job_id") jobId: String,
        @Field("rating") rating: Int,
        @Field("comment") comment: String
    ): Call<BaseResponse>

    // 2.5.9 Chat & Support
    @FormUrlEncoded
    @POST("api-mobile.php?action=get_messages")
    fun getMessages(
        @Query("action") actionQuery: String,
        @Query("tid") tenantIdQuery: String,
        @Field("action") actionField: String,
        @Field("tid") tenantIdField: String,
        @Field("customer_id") customerId: String
    ): Call<ChatResponse>

    @FormUrlEncoded
    @POST("api-mobile.php?action=send_message")
    fun sendMessage(
        @Query("action") actionQuery: String,
        @Query("tid") tenantIdQuery: String,
        @Field("action") actionField: String,
        @Field("tid") tenantIdField: String,
        @Field("customer_id") customerId: String,
        @Field("message") message: String
    ): Call<BaseResponse>

    @FormUrlEncoded
    @POST("api-mobile.php?action=get_booked_slots")
    fun getBookedSlots(
        @Query("action") actionQuery: String,
        @Query("tid") tenantIdQuery: String,
        @Field("action") actionField: String,
        @Field("tid") tenantIdField: String,
        @Field("date") date: String
    ): Call<BookedSlotsResponse>
}

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
    val tier: String, // Gold, Silver, Bronze
    val available_promos: List<Promo>
)

data class ChatResponse(
    val status: String,
    val data: List<Message>
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
    val price: String,
    val icon_url: String? = null
)

data class Vehicle(
    val vehicle_id: String,
    val plate_no: String,
    val make: String,
    val model: String,
    val year: String,
    val last_service_date: String? = null
)

data class Message(
    val message_id: String,
    val sender_type: String, // 'customer' or 'shop'
    val content: String,
    val created_at: String
)

data class Promo(
    val promo_id: String,
    val title: String,
    val description: String,
    val discount: String
)

data class RepairHistory(
    val job_id: String,
    val plate_no: String,
    val status: String,
    val total_amount: String,
    val date: String,
    val rating: Int? = null // For reviews
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
    val specialization: String?,
    val avatar_url: String? = null
)

data class Bay(
    val bay_id: String,
    val bay_name: String
)

data class TimelineItem(
    val status_update: String,
    val remarks: String?,
    val created_at: String,
    val inspection_photo: String? = null // For visual updates
)
