package msr.atsulab.app.ui.media.character

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.response.anilist.StaffRoleType
import msr.atsulab.app.databinding.ListMediaCharacterVoiceActorBinding
import msr.atsulab.app.helper.extensions.clicks
import msr.atsulab.app.helper.extensions.show
import msr.atsulab.app.helper.utils.ImageUtil
import msr.atsulab.app.ui.base.BaseRecyclerViewAdapter

class MediaCharacterListVoiceActorRvAdapter(
    private val context: Context,
    list: List<StaffRoleType>,
    private val appSetting: AppSetting,
    private val listener: MediaCharacterListRvAdapter.MediaCharacterListListener
) : BaseRecyclerViewAdapter<StaffRoleType, ListMediaCharacterVoiceActorBinding>(list) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListMediaCharacterVoiceActorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(view)
    }

    inner class ItemViewHolder(private val binding: ListMediaCharacterVoiceActorBinding) : ViewHolder(binding) {
        override fun bind(item: StaffRoleType, index: Int) {
            binding.apply {
                voiceActorName.text = item.voiceActor.name.userPreferred
                voiceActorRoleNote.text = "(${item.roleNote})"
                voiceActorRoleNote.show(item.roleNote.isNotBlank())
                voiceActorDubGroup.text = item.dubGroup
                voiceActorDubGroup.show(item.dubGroup.isNotBlank())
                ImageUtil.loadCircleImage(context, item.voiceActor.getImage(appSetting), voiceActorImage)

                root.clicks { listener.navigateToStaff(item.voiceActor) }
            }
        }
    }
}