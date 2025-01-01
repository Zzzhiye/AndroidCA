import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.androidca.R
import com.example.androidca.Ranking

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
        holder.dateTimeText.text = ranking.dateTime
        holder.scoreText.text = ranking.completionTime.toString()
    }

    override fun getItemCount() = rankings.size
}
