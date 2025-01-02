import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.androidca.R
import com.example.androidca.Ranking
import java.text.SimpleDateFormat
import java.util.*

class RankingAdapter(private val rankings: List<Ranking>) : RecyclerView.Adapter<RankingAdapter.RankingViewHolder>() {

    class RankingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTimeText: TextView = itemView.findViewById(R.id.rankingDateTime)
        val scoreText: TextView = itemView.findViewById(R.id.rankingScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ranking, parent, false)
        return RankingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        val ranking = rankings[position]

        // 格式化日期时间戳（以毫秒为单位）为秒级别
        val dateTimeMillis = ranking.dateTime.toLongOrNull() ?: 0L
        val formattedDateTime = formatDateTime(dateTimeMillis)

        holder.dateTimeText.text = formattedDateTime
        holder.scoreText.text = ranking.completionTime.toString()
    }

    override fun getItemCount() = rankings.size

    private fun formatDateTime(milliseconds: Long): String {
        val date = Date(milliseconds)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(date)
    }
}
