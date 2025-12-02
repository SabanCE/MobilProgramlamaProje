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

    // *** DEĞİŞİKLİK VE ÇÖZÜM BU FONKSİYONUN İÇİNDE ***
    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val currentNotification = notificationList[position]

        // Metinleri ata (bu kısımlar zaten doğru)
        holder.binding.textNotificationTitle.text = currentNotification.title
        holder.binding.textNotificationDescription.text = currentNotification.description
        holder.binding.chipNotificationStatus.text = currentNotification.status

        // Zamanı formatla (bu kısım da doğru)
        currentNotification.timestamp?.let { timestamp ->
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            holder.binding.textNotificationTime.text = sdf.format(timestamp.toDate())
        }

        // --- YENİ EKLENEN VE İŞİ YAPAN KISIM ---
        // Bildirimin 'type' alanına göre doğru ikonu seç ve ata.
        when (currentNotification.type) {
            "Sağlık" -> {
                holder.binding.iconNotificationType.setImageResource(R.drawable.ic_health) // Sağlık ikonu
            }
            "Güvenlik" -> {
                holder.binding.iconNotificationType.setImageResource(R.drawable.ic_security) // Güvenlik ikonu
            }
            "Çevre" -> {
                holder.binding.iconNotificationType.setImageResource(R.drawable.ic_environment) // Çevre ikonu
            }
            "Kayıp" -> {
                holder.binding.iconNotificationType.setImageResource(R.drawable.ic_loss) // Kayıp ikonu
            }
            else -> {
                // Eğer bilmediğimiz bir tür gelirse veya tür boşsa, varsayılan bir ikon göster.
                holder.binding.iconNotificationType.setImageResource(R.drawable.ic_default)
            }
        }
        // --- YENİ EKLENEN KISIM BİTTİ ---

        // Her bir satıra tıklama olayı (isteğe bağlı)
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
