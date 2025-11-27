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

        // --- Verileri daha güvenli bir şekilde ata ---
        // Eğer veri null (boş) gelirse, çökmesin diye varsayılan bir metin göster.
        holder.binding.textNotificationTitle.text = currentNotification.title.ifEmpty { "Başlık Yok" }
        holder.binding.textNotificationDescription.text = currentNotification.description.ifEmpty { "Açıklama Yok" }
        holder.binding.chipNotificationStatus.text = currentNotification.status.ifEmpty { "Durum Belirtilmemiş" }

        // Tarih null gelirse ne yapacağını söyle
        if (currentNotification.timestamp != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            holder.binding.textNotificationTime.text = sdf.format(currentNotification.timestamp.toDate())
        } else {
            holder.binding.textNotificationTime.text = "Tarih Yok"
        }

        // İkonları daha sonra ekleyebilirsin
        // holder.binding.iconNotificationType.setImageResource(R.drawable.ic_default)

        holder.itemView.setOnClickListener {
            // Detay sayfasına yönlendirme (daha sonra yapılabilir)
        }
    }

    override fun getItemCount(): Int {
        return notificationList.size
    }

    // Listeyi güncellemek için kullanılan fonksiyon
    fun updateList(newList: ArrayList<Notification>) {
        notificationList = newList
        notifyDataSetChanged() // RecyclerView'a "veriler değişti, kendini yenile" der.
    }
}
