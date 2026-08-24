package msr.atsulab.app.player.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import msr.atsulab.app.R

class PlayerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val aniListId = intent.getIntExtra(EXTRA_ANILIST_ID, INVALID_ANILIST_ID)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val initialEpisode = intent.getIntExtra(EXTRA_INITIAL_EPISODE, DEFAULT_INITIAL_EPISODE)
            .coerceAtLeast(DEFAULT_INITIAL_EPISODE)

        if (aniListId == INVALID_ANILIST_ID || title.isBlank()) {
            finish()
            return
        }

        val statusView = TextView(this).apply {
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
            setTextColor(Color.WHITE)
            text = getString(R.string.player_shell_status_format, title, initialEpisode)
        }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(Color.BLACK)
                addView(
                    statusView,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )
            }
        )
    }

    companion object {
        const val EXTRA_ANILIST_ID = "EXTRA_ANILIST_ID"
        const val EXTRA_MAL_ID = "EXTRA_MAL_ID"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_COVER_IMAGE_URL = "EXTRA_COVER_IMAGE_URL"
        const val EXTRA_BANNER_IMAGE_URL = "EXTRA_BANNER_IMAGE_URL"
        const val EXTRA_TOTAL_EPISODES = "EXTRA_TOTAL_EPISODES"
        const val EXTRA_INITIAL_EPISODE = "EXTRA_INITIAL_EPISODE"

        internal const val INVALID_ANILIST_ID = 0
        internal const val DEFAULT_INITIAL_EPISODE = 1
    }
}
