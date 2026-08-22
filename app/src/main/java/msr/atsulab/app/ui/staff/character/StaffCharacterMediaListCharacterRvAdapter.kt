package msr.atsulab.app.ui.staff.character

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.response.anilist.Character
import msr.atsulab.app.databinding.ListMediaCharacterVoiceActorBinding
import msr.atsulab.app.helper.extensions.clicks
import msr.atsulab.app.helper.extensions.show
import msr.atsulab.app.helper.utils.ImageUtil
import msr.atsulab.app.ui.base.BaseRecyclerViewAdapter

class StaffCharacterMediaListCharacterRvAdapter(
    private val context: Context,
    list: List<Character>,
    private val appSetting: AppSetting,
    private val listener: StaffCharacterListListener
) : BaseRecyclerViewAdapter<Character, ListMediaCharacterVoiceActorBinding>(list) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListMediaCharacterVoiceActorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(view)
    }

    inner class ItemViewHolder(private val binding: ListMediaCharacterVoiceActorBinding) : ViewHolder(binding) {
        override fun bind(item: Character, index: Int) {
            binding.apply {
                voiceActorName.text = item.name.userPreferred
                voiceActorRoleNote.show(false)
                voiceActorDubGroup.show(false)
                ImageUtil.loadCircleImage(context, item.getImage(appSetting), voiceActorImage)
                root.clicks { listener.navigateToCharacter(item) }
            }
        }
    }
}