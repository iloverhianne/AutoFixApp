package com.example.autofixapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PaymentHistoryAdapter(private var payments: List<PaymentHistory>) :
    RecyclerView.Adapter<PaymentHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMethod: TextView = view.findViewById(R.id.tvPaymentMethod)
        val tvStatus: TextView = view.findViewById(R.id.tvPaymentStatus)
        val tvDate: TextView = view.findViewById(R.id.tvPaymentDate)
        val tvType: TextView = view.findViewById(R.id.tvPaymentType)
        val tvAmount: TextView = view.findViewById(R.id.tvPaymentAmount)
        val tvDetails: TextView = view.findViewById(R.id.tvPaymentDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val payment = payments[position]
        holder.tvMethod.text = payment.payment_method?.uppercase() ?: "UNKNOWN"
        holder.tvStatus.text = payment.status?.uppercase() ?: "UNKNOWN"
        val rawTime = payment.time
        val formattedTime = if (!rawTime.isNullOrEmpty()) {
            try {
                if (rawTime.contains("AM", ignoreCase = true) || rawTime.contains("PM", ignoreCase = true)) {
                    rawTime
                } else {
                    val parts = rawTime.split(":")
                    if (parts.size >= 2) {
                        var hour24 = parts[0].toInt()
                        val minute = parts[1]

                        // Smart Fix: If hour is 1-7, it's likely PM (Shop opens at 8AM)
                        if (hour24 in 1..7) hour24 += 12

                        val ampm = if (hour24 >= 12) "PM" else "AM"
                        val hour12 = when {
                            hour24 == 0 -> 12
                            hour24 > 12 -> hour24 - 12
                            else -> hour24
                        }
                        String.format("%02d:%s %s", hour12, minute, ampm)
                    } else {
                        rawTime
                    }
                }
            } catch (e: Exception) {
                rawTime
            }
        } else {
            null
        }

        holder.tvDate.text = if (!formattedTime.isNullOrEmpty()) {
            "${payment.date ?: "--"} · $formattedTime"
        } else {
            payment.date ?: "--"
        }
        holder.tvType.text = "Type: ${payment.payment_type?.uppercase() ?: "UNKNOWN"}"
        holder.tvAmount.text = "₱${payment.amount ?: "0.00"}"

        if (!payment.plate_no.isNullOrEmpty() || !payment.service_name.isNullOrEmpty()) {
            holder.tvDetails.visibility = View.VISIBLE
            holder.tvDetails.text = "${payment.plate_no ?: ""} • ${payment.service_name ?: ""}"
        } else {
            holder.tvDetails.visibility = View.GONE
        }

        // Dynamic status colors
        val context = holder.itemView.context
        when (payment.status?.lowercase() ?: "") {
            "paid", "completed", "success" -> {
                holder.tvStatus.setTextColor(context.getColor(R.color.status_completed_text))
                holder.tvStatus.setBackgroundResource(R.drawable.status_success_badge)
            }
            "failed", "cancelled" -> {
                holder.tvStatus.setTextColor(context.getColor(R.color.status_cancelled_text))
                holder.tvStatus.setBackgroundResource(R.drawable.status_danger_badge)
            }
            else -> {
                holder.tvStatus.setTextColor(context.getColor(R.color.status_active_text))
                holder.tvStatus.setBackgroundResource(R.drawable.status_active_badge)
            }
        }
    }

    override fun getItemCount() = payments.size

    fun updateData(newPayments: List<PaymentHistory>) {
        this.payments = newPayments
        notifyDataSetChanged()
    }
}
