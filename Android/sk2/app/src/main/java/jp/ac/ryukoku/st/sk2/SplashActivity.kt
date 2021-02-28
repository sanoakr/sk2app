package jp.ac.ryukoku.st.sk2

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import jp.ac.ryukoku.st.sk2.MainActivityUi.Companion.TEXT_HUGE
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.APP_NAME
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.APP_TITLE
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.COLOR_NORMAL
import org.jetbrains.anko.*


/** ////////////////////////////////////////////////////////////////////////////// **/
class SplashActivity : Activity(), AnkoLogger {
    private var splashUi = SplashActivityUi()

    companion object {
        private val TIME_SPLASH = 1000L // msec
    }

    private fun startLogin() {
        /** ユーザIDと教室情報JSON が空でなければ MainActivity へ **/
        info(Sk2Preferences.getString(Sk2Preferences.Key.USER))
        info(Sk2Preferences.getBoolean(Sk2Preferences.Key.ACCEPT_POLICY))
        info(Sk2Preferences.getInt(Sk2Preferences.Key.APP_CODE))
        info(BuildConfig.VERSION_CODE)
        if (Sk2Preferences.getString(Sk2Preferences.Key.USER).isNotBlank()
            && Sk2Preferences.getBoolean(Sk2Preferences.Key.ACCEPT_POLICY)
            && (Sk2Preferences.getInt(Sk2Preferences.Key.APP_CODE) == BuildConfig.VERSION_CODE)
        ) {
            /** Room Map / InfoQueue を作成 **/
            Rooms.add(Sk2Preferences.getString(Sk2Preferences.Key.ROOM_JSON))

            /** そのままメインアクティビティへ **/
            startActivity<MainActivity>()
            //val intent = Intent(this, MainActivity::class.java)
            //intent.flags = (Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            //startActivity(intent)
            return
        }

        /** Main に飛ばなければ Login へ**/
        startActivity<LoginActivity>()
        // for test
        //startActivity<MainActivity>()
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // App バージョン // to sk3Globals constant
        //prefs.setString(Sk2Preferences.Key.APP_VERSION, BuildConfig.VERSION_NAME)
        //prefs.setInt(Sk2Preferences.Key.APP_CODE, BuildConfig.VERSION_CODE)

        // Show Splash
        window.statusBarColor = COLOR_NORMAL
        splashUi.setContentView(this)

        Handler(Looper.getMainLooper()).postDelayed({ startLogin() },TIME_SPLASH)
    }

    /** ////////////////////////////////////////////////////////////////////////////// **/
    /** Disable Back Key **/
    override fun onBackPressed() {}
}

/** ////////////////////////////////////////////////////////////////////////////// **/
/** UI構成 via Anko **/
class SplashActivityUi: AnkoComponent<SplashActivity> {
    /** ////////////////////////////////////////////////////////////////////////////// **/
    override fun createView(ui: AnkoContext<SplashActivity>) = with(ui) {
        relativeLayout {
            backgroundColor = COLOR_NORMAL
            /** ////////////////////////////////////////////////////////////////////////////// **/
            textView("$APP_TITLE $APP_NAME") {
                textSize = TEXT_HUGE
                textColor = Color.WHITE
            }.lparams{
                centerInParent()
            }
        }
    }
}