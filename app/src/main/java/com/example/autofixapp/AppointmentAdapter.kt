package com.example.autofixapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class AppointmentAdapter(private var appointments: List<RepairHistory>) :
    RecyclerView.Adapter<AppointmentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvHistoryServiceName)
        val tvStatus: TextView = view.findViewById(R.id.tvHistoryStatus)
        val tvDate: TextView = view.findViewById(R.id.tvHistoryDate)
        val tvAmount: TextView = view.findViewById(R.id.tvHistoryAmount)
        val btnPay: MaterialButton = view.findViewById(R.id.btnHistoryPay)
        
        // Billing fields
        val layoutDownpayment: View = view.findViewById(R.id.layoutDownpayment)
        val tvPaid: TextView = view.findViewById(R.id.tvHistoryPaid)
        val layoutBalance: View = view.findViewById(R.id.layoutBalance)
        val tvBalance: TextView = view.findViewById(R.id.tvHistoryBalance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appt = appointments[position]
        holder.tvName.text = "${appt.service_name ?: "Service"} (${appt.plate_no ?: "N/A"})"
        holder.tvStatus.text = appt.status?.uppercase() ?: "UNKNOWN"
        holder.tvDate.text = appt.date ?: "--"
        holder.tvAmount.text = "₱${appt.total_amount ?: "0.00"}"
        
        // Billing Logic
        val total = appt.total_amount?.toDoubleOrNull() ?: 0.0
        val paid = appt.paid_amount?.toDoubleOrNull() ?: 0.0
        val balance = total - paid
        
        if (paid > 0) {
            holder.layoutDownpayment.visibility = View.VISIBLE
            holder.tvPaid.text = "₱${String.format("%.2f", paid)}"
            
            if (balance > 0) {
                holder.layoutBalance.visibility = View.VISIBLE
                holder.tvBalance.text = "₱${String.format("%.2f", balance)}"
            } else {
                holder.layoutBalance.visibility = View.GONE
            }
        } else {
            holder.layoutDownpayment.visibility = View.GONE
            holder.layoutBalance.visibility = View.GONE
        }

        // Dynamic status colors
        val context = holder.itemView.context
        
        if (appt.status?.equals("COMPLETED", ignoreCase = true) == true && balance > 0) {
            holder.btnPay.visibility = View.VISIBLE
            holder.btnPay.text = "Pay Remaining Balance"
            holder.btnPay.setOnClickListener {
                val intent = Intent(context, PaymentActivity::class.java).apply {
                    putExtra("AMOUNT", String.format("%.2f", balance))
                    putExtra("JOB_ID", appt.job_id)
                }
                context.startActivity(intent)
            }
        } else {
            holder.btnPay.visibility = View.GONE
        }

        when (appt.status?.lowercase()) {
            "pending" -> {
                holder.tvStatus.setTextColor(context.getColor(R.color.status_pending_text))
                holder.tvStatus.setBackgroundResource(R.drawable.status_badge_bg)
            }
            "confirmed" -> {
                holder.tvStatus.setTextColor(context.getColor(R.color.status_completed_text))
                holder.tvStatus.setBackgroundResource(R.drawable.status_success_badge)
            }
            else -> {
                holder.tvStatus.setTextColor(context.getColor(R.color.status_active_text))
                holder.tvStatus.setBackgroundResource(R.drawable.status_active_badge)
            }
        }
    }

    override fun getItemCount() = appointments.size

    fun updateData(newAppointments: List<RepairHistory>) {
        this.appointments = newAppointments
        notifyDataSetChanged()
    }
}
