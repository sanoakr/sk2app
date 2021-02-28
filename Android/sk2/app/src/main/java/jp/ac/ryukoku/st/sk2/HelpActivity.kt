package jp.ac.ryukoku.st.sk2

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import jp.ac.ryukoku.st.sk2.MainActivityUi.Companion.TEXT_HUGE
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.COLOR_BACKGROUND
import org.jetbrains.anko.*

/** ////////////////////////////////////////////////////////////////////////////// **/
/*** ヘルプ表示 ***/
class HelpActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val helpUi = HelpActivityUi()
        helpUi.setContentView(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.statusBarColor = COLOR_BACKGROUND
            //window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }
}
/** ////////////////////////////////////////////////////////////////////////////// **/
class HelpActivityUi: AnkoComponent<HelpActivity> {
    companion object {
        const val TITLE = 1
        const val HELP_URI = "https://sk2.st.ryukoku.ac.jp/"
        const val TITLE_TEXT = "about sk2"
    }
    override fun createView(ui: AnkoContext<HelpActivity>) = with(ui) {

        relativeLayout {
            backgroundColor = COLOR_BACKGROUND
            ////////////////////////////////////////
            textView(TITLE_TEXT) {
                id = TITLE
                textColor = Color.BLACK
                textSize = TEXT_HUGE
                backgroundColor = Sk2Globals.COLOR_BACKGROUND_TITLE
                topPadding = dip(10); bottomPadding = dip(10)
                gravity = Gravity.CENTER_HORIZONTAL
            }.lparams { alignParentTop(); alignParentStart(); alignParentEnd()
                centerHorizontally()
            }
            /** ////////////////////////////////////////////////////////////////////////////// **/            ////////////////////////////////////////
            webView {
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                loadUrl(HELP_URI) // sk2 のヘルプページの URI
            }.lparams {
                below(TITLE)
                alignParentStart(); alignParentEnd(); alignParentBottom()
            }
        }
    }
}