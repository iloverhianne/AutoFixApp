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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val payment = payments[position]
        holder.tvMethod.text = payment.payment_method.uppercase()
        holder.tvStatus.text = payment.status.uppercase()
        holder.tvDate.text = payment.date
        holder.tvType.text = "Type: ${payment.payment_type.uppercase()}"
        holder.tvAmount.text = "₱${payment.amount}"

        // Dynamic status color
        when (payment.status.lowercase()) {
            "paid", "completed", "success" -> {
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#03543F"))
                holder.tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#DEF7EC"))
            }
            "failed", "cancelled" -> {
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#9B1C1C"))
                holder.tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#FDE2E2"))
            }
            else -> {
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#1E40AF"))
                holder.tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#DBEAFE"))
            }
        }
    }

    override fun getItemCount() = payments.size

    fun updateData(newPayments: List<PaymentHistory>) {
        this.payments = newPayments
        notifyDataSetChanged()
    }
}
