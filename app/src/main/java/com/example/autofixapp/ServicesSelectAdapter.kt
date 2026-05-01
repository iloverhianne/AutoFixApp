package com.example.autofixapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ServicesSelectAdapter(
    private val services: List<Service>,
    private val startingChecked: List<String>,
    private val onItemSelected: (String, Boolean) -> Unit
) : RecyclerView.Adapter<ServicesSelectAdapter.ViewHolder>() {

    private val checkedIds = startingChecked.toMutableSet()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvServiceName)
        val tvPrice: TextView = view.findViewById(R.id.tvServicePrice)
        val cbSelect: CheckBox = view.findViewById(R.id.cbServiceSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service_select, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val service = services[position]
        holder.tvName.text = service.service_name
        holder.tvPrice.text = "₱${service.price}"
        
        holder.cbSelect.setOnCheckedChangeListener(null)
        holder.cbSelect.isChecked = checkedIds.contains(service.service_id)
        
        holder.cbSelect.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) checkedIds.add(service.service_id) else checkedIds.remove(service.service_id)
            onItemSelected(service.service_id, isChecked)
        }
        
        holder.itemView.setOnClickListener {
            holder.cbSelect.toggle()
        }
    }

    override fun getItemCount() = services.size
}
