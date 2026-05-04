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
        val rawDate = payment.date ?: "--"
        val displayDate = if (rawDate.contains(" ")) {
            try {
                val parts = rawDate.split(" ")
                val datePart = parts[0]
                val timePart = parts[1]
                val tParts = timePart.split(":")
                if (tParts.size >= 2) {
                    var h = tParts[0].toInt()
                    val m = tParts[1]
                    val ampm = if (h >= 12) "PM" else "AM"
                    val h12 = when {
                        h == 0 -> 12
                        h > 12 -> h - 12
                        else -> h
                    }
                    "$datePart · ${String.format("%02d:%s %s", h12, m, ampm)}"
                } else rawDate
            } catch (e: Exception) { rawDate }
        } else rawDate

        holder.tvDate.text = displayDate
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
