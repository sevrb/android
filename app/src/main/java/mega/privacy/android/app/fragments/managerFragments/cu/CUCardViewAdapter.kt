package mega.privacy.android.app.fragments.managerFragments.cu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemCuCardBinding
import mega.privacy.android.app.fragments.managerFragments.cu.CameraUploadsFragment.*
import nz.mega.sdk.MegaNode

class CUCardViewAdapter(private val cardViewType: Int) :
    RecyclerView.Adapter<CUCardViewHolder>(), SectionTitleProvider {

    private var cards: List<Pair<CUCard, MegaNode>> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CUCardViewHolder {
        return CUCardViewHolder(
            cardViewType,
            ItemCuCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: CUCardViewHolder, position: Int) {
        holder.bind(cards[position])
    }

    override fun getItemCount(): Int {
        return cards.size
    }

    override fun getSectionTitle(position: Int): String {
        return if (position < 0 || position >= cards.size) ""
        else {
            val date = cards[position].first

            when (cardViewType) {
                YEARS_VIEW -> date.year
                MONTHS_VIEW -> date.month + " " + date.year
                DAYS_VIEW -> date.day + " " + date.month + " " + date.year
                else -> ""
            }
        }
    }
}