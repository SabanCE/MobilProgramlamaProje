package com.example.mobilprogramlamaproje

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mobilprogramlamaproje.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationsAdapter(private var notificationList: ArrayList<Notification>) :
    RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val currentNotification = notificationList[position]

        holder.binding.textNotificationTitle.text = currentNotification.title
        holder.binding.textNotificationDescription.text = currentNotification.description
        holder.binding.chipNotificationStatus.text = currentNotification.status

        currentNotification.timestamp?.let { timestamp ->
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            holder.binding.textNotificationTime.text = sdf.format(timestamp.toDate())
        }

        // Set background color based on notification type
        if ("Acil Durum".equals(currentNotification.type, ignoreCase = true)) {
            holder.binding.root.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.emergency_background))
        } else {
            // Reset to default background
            holder.binding.root.setCardBackgroundColor(Color.WHITE) // veya varsayılan renginiz
        }

        when (currentNotification.type) {
            "Acil Durum" -> {
                holder.binding.iconNotificationType.setImageResource(R.drawable.emergency)
            }
            "Sağlık" -> {
                holder.binding.iconNotificationType.setImageResource(R.drawable.ic_health)
            }
            "Güvenlik" -> {
                holder.binding.iconNotificationType.setImageResource(R.drawable.ic_security)
            }
            "Çevre" -> {
                holder.binding.iconNotificationType.setImageResource(R.drawable.ic_environment)
            }
            "Kayıp-Buluntu" -> {
                holder.binding.iconNotificationType.setImageResource(R.drawable.ic_kayip_buluntu)
            }
            "Teknik Arıza" -> {
                holder.binding.iconNotificationType.setImageResource(R.drawable.ic_teknik_ariza)
            }
            else -> {
                holder.binding.iconNotificationType.setImageResource(R.drawable.ic_default)
            }
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, NotificationDetailActivity::class.java).apply {
                putExtra("NOTIFICATION_ID", currentNotification.id)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return notificationList.size
    }

    fun updateList(newList: ArrayList<Notification>) {
        notificationList.clear()
        notificationList.addAll(newList)
        notifyDataSetChanged()
    }
}
