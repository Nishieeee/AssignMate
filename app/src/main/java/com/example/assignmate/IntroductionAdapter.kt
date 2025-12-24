package com.example.assignmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class IntroItem(
    val imageRes: Int,
    val title: String,
    val body: String
)

class IntroductionAdapter(private val items: List<IntroItem>) : RecyclerView.Adapter<IntroductionAdapter.IntroViewHolder>() {

    inner class IntroViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.pageImage)
        val title: TextView = view.findViewById(R.id.pageTitle)
        val body: TextView = view.findViewById(R.id.pageBody)

        fun bind(item: IntroItem) {
            image.setImageResource(item.imageRes)
            title.text = item.title
            body.text = item.body
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntroViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_introduction, parent, false)
        return IntroViewHolder(view)
    }

    override fun onBindViewHolder(holder: IntroViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}