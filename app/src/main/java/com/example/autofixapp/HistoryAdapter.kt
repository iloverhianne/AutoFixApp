package com.example.autofixapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class HistoryAdapter(private var repairs: List<RepairHistory>, private val onItemClick: ((String) -> Unit)? = null) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

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
        val repair = repairs[position]
        holder.tvName.text = "${repair.service_name ?: "Repair"} #${repair.job_id ?: "0"} (${repair.plate_no ?: "N/A"})"
        holder.tvStatus.text = repair.status?.uppercase() ?: "UNKNOWN"
        holder.tvDate.text = repair.date ?: "--"
        holder.tvAmount.text = "₱${repair.total_amount ?: "0.00"}"
        
        // Billing Logic
        val total = repair.total_amount?.toDoubleOrNull() ?: 0.0
        val paid = repair.paid_amount?.toDoubleOrNull() ?: 0.0
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

        // Show Pay Button if Status is COMPLETED (Ready for final payment)
        if (repair.status?.equals("COMPLETED", ignoreCase = true) == true && balance > 0.01) {
            holder.btnPay.visibility = View.VISIBLE
            holder.btnPay.text = "Pay Remaining Balance"
            holder.btnPay.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, PaymentActivity::class.java).apply {
                    putExtra("AMOUNT", String.format("%.2f", balance))
                    putExtra("JOB_ID", repair.job_id)
                }
                context.startActivity(intent)
            }
        } else {
            holder.btnPay.visibility = View.GONE
        }
        
        // Dynamic status colors
        val context = holder.itemView.context
        when (repair.status?.lowercase()) {
            "completed" -> {
                holder.tvStatus.setTextColor(context.getColor(R.color.status_completed_text))
                holder.tvStatus.setBackgroundResource(R.drawable.status_success_badge)
            }
            "cancelled" -> {
                holder.tvStatus.setTextColor(context.getColor(R.color.status_cancelled_text))
                holder.tvStatus.setBackgroundResource(R.drawable.status_danger_badge)
            }
            else -> {
                holder.tvStatus.setTextColor(context.getColor(R.color.status_active_text))
                holder.tvStatus.setBackgroundResource(R.drawable.status_active_badge)
            }
        }
        
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(repair.job_id ?: "")
        }
    }

    override fun getItemCount() = repairs.size

    fun updateData(newRepairs: List<RepairHistory>) {
        this.repairs = newRepairs
        notifyDataSetChanged()
    }
}

