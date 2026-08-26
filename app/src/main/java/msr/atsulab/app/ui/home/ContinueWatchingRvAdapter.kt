package msr.atsulab.app.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import msr.atsulab.app.R
import msr.atsulab.app.databinding.ListContinueWatchingBinding
import msr.atsulab.app.helper.extensions.clicks
import msr.atsulab.app.helper.extensions.show
import msr.atsulab.app.helper.utils.ImageUtil
import msr.atsulab.app.player.domain.model.PlaybackProgress
import msr.atsulab.app.ui.base.BaseRecyclerViewAdapter

class ContinueWatchingRvAdapter(
    private val context: Context,
    list: List<PlaybackProgress>,
    private val listener: HomeListener.ContinueWatchingListener
) : BaseRecyclerViewAdapter<PlaybackProgress, ListContinueWatchingBinding>(list) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListContinueWatchingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(view)
    }

    inner class ItemViewHolder(private val binding: ListContinueWatchingBinding) : ViewHolder(binding) {
        override fun bind(item: PlaybackProgress, index: Int) {
            with(binding) {
                continueTitleText.text = item.animeTitle.ifBlank { context.getString(R.string.unknown) }
                continueEpisodeText.text = context.getString(
                    R.string.continue_watching_episode,
                    formatEpisodeNumber(item),
                    item.percent
                )
                continueProgressBar.progress = item.percent

                if (item.thumbnailImageUrl.isNotBlank()) {
                    ImageUtil.loadImage(context, item.thumbnailImageUrl, continueCoverImage)
                    continueCoverImage.show(true)
                } else {
                    continueCoverImage.show(false)
                }

                root.clicks { listener.resumePlayback(item) }
                continueRemoveButton.clicks { listener.removeProgress(item) }
            }
        }

        private fun formatEpisodeNumber(progress: PlaybackProgress): String {
            val wholeNumber = progress.episodeNumber.toInt().toFloat()
            return if (progress.episodeNumber == wholeNumber) {
                wholeNumber.toInt().toString()
            } else {
                progress.episodeNumber.toString()
            }
        }
    }
}
