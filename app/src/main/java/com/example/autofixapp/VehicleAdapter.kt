package com.example.autofixapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VehicleAdapter(private var vehicles: List<Vehicle>) :
    RecyclerView.Adapter<VehicleAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMakeModel: TextView = view.findViewById(R.id.tvVehicleMakeModel)
        val tvPlate: TextView = view.findViewById(R.id.tvVehiclePlate)
        val tvLastService: TextView = view.findViewById(R.id.tvVehicleLastService)
        val tvActiveJobs: TextView = view.findViewById(R.id.tvVehicleActiveJobs)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vehicle, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val vehicle = vehicles[position]
        holder.tvMakeModel.text = "${vehicle.make} ${vehicle.model}"
        holder.tvPlate.text = vehicle.plate_no.uppercase()
        holder.tvLastService.text = vehicle.last_service_date ?: "No service yet"
        holder.tvActiveJobs.text = "0" // Logic would go here
    }

    override fun getItemCount() = vehicles.size

    fun updateData(newVehicles: List<Vehicle>) {
        this.vehicles = newVehicles
        notifyDataSetChanged()
    }
}
