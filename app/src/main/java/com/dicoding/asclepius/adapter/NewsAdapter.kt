package com.dicoding.asclepius.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dicoding.asclepius.R
import com.dicoding.asclepius.retrofit.ArticlesItem
import com.dicoding.asclepius.view.WebViewActivity

class NewsAdapter(private var newsList: List<ArticlesItem>) :
    RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    fun updateNewsList(newList: List<ArticlesItem?>) {
        newsList = newList as List<ArticlesItem>
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val article = newsList[position]
        holder.bind(article)
    }

    override fun getItemCount(): Int = newsList.size

    class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.articleImage)
        private val titleTextView: TextView = itemView.findViewById(R.id.articleTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.articleDescription)

        fun bind(article: ArticlesItem) {
            titleTextView.text = article.title
            descriptionTextView.text = article.description

            Glide.with(itemView.context)
                .load(article.urlToImage)
                .error(R.drawable.error_image)
                .into(imageView)

            itemView.setOnClickListener {
                val intent = Intent(itemView.context, WebViewActivity::class.java)
                intent.putExtra("URL", article.url)
                itemView.context.startActivity(intent)
            }
        }
    }
}
