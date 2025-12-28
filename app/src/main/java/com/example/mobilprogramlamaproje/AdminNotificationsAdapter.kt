package com.example.mobilprogramlamaproje

import android.content.Intent
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mobilprogramlamaproje.databinding.ItemNotificationAdminBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class AdminNotificationsAdapter(private var notificationList: ArrayList<Notification>) :
    RecyclerView.Adapter<AdminNotificationsAdapter.AdminViewHolder>() {

    private val userCache = HashMap<String, Pair<String, String>>()

    inner class AdminViewHolder(val binding: ItemNotificationAdminBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewHolder {
        val binding = ItemNotificationAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AdminViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminViewHolder, position: Int) {
        val notification = notificationList[position]

        holder.binding.textTitle.text = notification.title
        holder.binding.textDescription.text = notification.description
        holder.binding.textStatus.text = notification.status

        notification.timestamp?.let {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            holder.binding.textTime.text = sdf.format(it.toDate())
        }

        when (notification.type) {
            "Acil Durum" -> holder.binding.iconType.setImageResource(R.drawable.emergency)
            "Sağlık" -> holder.binding.iconType.setImageResource(R.drawable.ic_health)
            "Güvenlik" -> holder.binding.iconType.setImageResource(R.drawable.ic_security)
            "Çevre" -> holder.binding.iconType.setImageResource(R.drawable.ic_environment)
            "Kayıp-Buluntu" -> holder.binding.iconType.setImageResource(R.drawable.ic_kayip_buluntu)
            "Teknik Arıza" -> holder.binding.iconType.setImageResource(R.drawable.ic_teknik_ariza)
            else -> holder.binding.iconType.setImageResource(R.drawable.ic_default)
        }

        // RENK MANTIĞI:
        // Eğer "Acil Durum" ise VE durumu "Çözüldü" DEĞİLSE renkli gözükür.
        // Çözüldü olan acil durumlar normal bildirimler gibi beyaz olur.
        val isEmergency = "Acil Durum".equals(notification.type, ignoreCase = true)
        val isResolved = "Çözüldü".equals(notification.status, ignoreCase = true)

        if (isEmergency && !isResolved) {
            holder.binding.root.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.emergency_background))
        } else {
            holder.binding.root.setCardBackgroundColor(Color.WHITE)
        }

        notification.userId?.let { uid ->
            if (userCache.containsKey(uid)) {
                val (name, email) = userCache[uid]!!
                holder.binding.textPublisherName.text = "Oluşturan: $name"
                holder.binding.textPublisherEmail.text = "E-posta: $email"
            } else {
                holder.binding.textPublisherName.text = "Oluşturan: Yükleniyor..."
                holder.binding.textPublisherEmail.text = ""
                FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        if (doc != null && doc.exists()) {
                            val name = doc.getString("nameSurname") ?: "Bilinmiyor"
                            val email = doc.getString("email") ?: ""
                            userCache[uid] = Pair(name, email)
                            notifyItemChanged(holder.bindingAdapterPosition)
                        }
                    }
            }
        }

        holder.binding.textStatus.setOnClickListener { showStatusUpdateDialog(holder, notification) }
        holder.binding.btnEditAdmin.setOnClickListener { showEditDialog(holder, notification) }
        holder.binding.btnDeleteAdmin.setOnClickListener { showDeleteDialog(holder, notification) }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, NotificationDetailActivity::class.java).apply {
                putExtra("NOTIFICATION_ID", notification.id)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    private fun showStatusUpdateDialog(holder: AdminViewHolder, notification: Notification) {
        val context = holder.itemView.context
        val statusOptions = arrayOf("Açık", "Çözüldü", "İnceleniyor")
        AlertDialog.Builder(context).setTitle("Durum Güncelle").setItems(statusOptions) { _, which ->
            FirebaseFirestore.getInstance().collection("notifications").document(notification.id)
                .update("status", statusOptions[which])
                .addOnSuccessListener { 
                    Toast.makeText(context, "Durum güncellendi", Toast.LENGTH_SHORT).show()
                    // Durum değiştiğinde rengin de güncellenmesi için notification nesnesini güncelle ve notify et
                    notification.id // Gereksiz ama referans
                }
        }.show()
    }

    private fun showEditDialog(holder: AdminViewHolder, notification: Notification) {
        val context = holder.itemView.context
        val etDescription = EditText(context).apply {
            hint = "Açıklama"
            setText(notification.description)
            setPadding(60, 40, 60, 40)
        }

        val dialog = AlertDialog.Builder(context).setTitle("Açıklamayı Düzenle").setView(etDescription)
            .setPositiveButton("Güncelle") { _, _ ->
                FirebaseFirestore.getInstance().collection("notifications").document(notification.id)
                    .update("description", etDescription.text.toString().trim())
            }.setNegativeButton("İptal", null).create()

        dialog.show()
        val updateButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        updateButton.isEnabled = etDescription.text.toString().trim().isNotEmpty()

        etDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateButton.isEnabled = s?.toString()?.trim()?.isNotEmpty() ?: false
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showDeleteDialog(holder: AdminViewHolder, notification: Notification) {
        val context = holder.itemView.context
        AlertDialog.Builder(context).setTitle("Bildirimi Sil").setMessage("Bu bildirimi silmek istediğinizden emin misiniz?")
            .setPositiveButton("Sil") { _, _ ->
                FirebaseFirestore.getInstance().collection("notifications").document(notification.id).delete()
            }.setNegativeButton("İptal", null).show()
    }

    override fun getItemCount(): Int = notificationList.size

    fun updateList(newList: List<Notification>) {
        notificationList.clear()
        notificationList.addAll(newList)
        notifyDataSetChanged()
    }
}