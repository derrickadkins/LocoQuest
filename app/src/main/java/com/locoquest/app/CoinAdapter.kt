package com.locoquest.app

import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.locoquest.app.dto.Coin

class CoinAdapter(private val coins: ArrayList<Coin>,
                  private val longClickListener: OnLongClickListener,
                  private val clickListener: OnClickListener) : RecyclerView.Adapter<CoinAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.coin, parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return coins.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val coin = coins[position]
        holder.pid.text = "${coin.pid}:"
        holder.name.text = coin.name
        holder.latlng.text = "${coin.lat} ${coin.lon}"
        holder.lastVisited.text = Converters.formatSeconds(coin.lastVisited)

        holder.itemView.setOnLongClickListener(longClickListener)
        holder.itemView.setOnClickListener(clickListener)
    }

    fun removeCoin(pid: String): Coin? {
        val index = coins.indexOfFirst { it.pid == pid }
        return if (index != -1) coins.removeAt(index) else null
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pid: TextView = itemView.findViewById(R.id.pid)
        val name: TextView = itemView.findViewById(R.id.name)
        val latlng: TextView = itemView.findViewById(R.id.latlng)
        val lastVisited: TextView = itemView.findViewById(R.id.last)
    }
}