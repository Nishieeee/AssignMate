package com.example.assignmate

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView

class FullScreenImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        val imageUrl = intent.getStringExtra("IMAGE_URL")
        val imageFileName = intent.getStringExtra("IMAGE_NAME") ?: "downloaded_image"

        val photoView: PhotoView = findViewById(R.id.full_screen_image_view)
        val closeButton: ImageButton = findViewById(R.id.close_button)
        val downloadButton: ImageButton = findViewById(R.id.download_button)

        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .into(photoView)
        }

        closeButton.setOnClickListener {
            finish()
        }

        downloadButton.setOnClickListener {
            if (imageUrl != null) {
                downloadImage(this, imageUrl, imageFileName)
            }
        }
    }

    private fun downloadImage(context: Context, url: String, fileName: String) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(url)
            val request = DownloadManager.Request(uri)
            
            var name = fileName
            // Ensure filename has an extension if possible, or let DM handle it.
            // But we don't always know the extension from the URL if it's raw.
            // Cloudinary URLs usually end in .jpg or .png
            
            request.setTitle(name)
            request.setDescription("Downloading image...")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            
            downloadManager.enqueue(request)
            Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
