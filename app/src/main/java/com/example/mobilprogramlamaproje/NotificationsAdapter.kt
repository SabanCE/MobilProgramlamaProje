package com.example.mobilprogramlamaproje
import android.view.LayoutInflater
import android.view.ViewGroup
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

        when (currentNotification.type) {
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
            // Detay sayfasına yönlendirme mantığı buraya gelebilir.
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
