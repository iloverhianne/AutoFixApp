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

        // Dynamic status colors
        val context = holder.itemView.context
        when (payment.status.lowercase()) {
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
