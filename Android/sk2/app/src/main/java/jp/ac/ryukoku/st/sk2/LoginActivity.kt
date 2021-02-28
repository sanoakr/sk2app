package jp.ac.ryukoku.st.sk2

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import jp.ac.ryukoku.st.sk2.MainActivity.Companion.TOAST_CANT_CONNECT_SERVER
import jp.ac.ryukoku.st.sk2.Sk2Connector.Companion.COMMAND_AUTH
import jp.ac.ryukoku.st.sk2.Sk2Connector.Companion.REPLY_AUTH_FAIL
import jp.ac.ryukoku.st.sk2.Sk2Connector.Companion.REPLY_FAIL
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.NAME_START_TESTUSER
import jp.ac.ryukoku.st.sk2.Sk2Preferences.setBoolean
import jp.ac.ryukoku.st.sk2.Sk2Preferences.setInt
import jp.ac.ryukoku.st.sk2.Sk2Preferences.setString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.anko.*

/** ////////////////////////////////////////////////////////////////////////////// **/
class LoginActivity : Activity() {
    companion object {
        const val TOAST_LOGIN_ATTEMPT_UID = "学籍番号を入力して下さい"
        const val TOAST_LOGIN_ATTEMPT_PASSWD = "パスワードを入力して下さい"
        const val TOAST_LOGIN_ATTEMPT_ATMARK = "認証IDに @ 以降を含めないで下さい"
        const val TOAST_LOGIN_SUCCESS = "ログインします"
        const val TOAST_LOGIN_FAIL = "ログインに失敗しました"
    }

    private var loginUi = LoginActivityUi()

    /** ////////////////////////////////////////////////////////////////////////////// **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loginUi.setContentView(this)
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    override fun onResume() {
        super.onResume()
        /** auto login for testing **/
        //attemptLogin("testuser", "testuser")
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    /** Disable Back Key **/
    override fun onBackPressed() {}

    /** ////////////////////////////////////////////////////////////////////////////// **/
    /** サーバでログイン認証 **/
    fun attemptLogin(user: String, passwd: String, org: Int) {
        when {
            user.isBlank() ->
                toast(TOAST_LOGIN_ATTEMPT_UID).setGravity(Gravity.CENTER, 0, 0)
            !user.startsWith(NAME_START_TESTUSER) && passwd.isBlank() ->
                toast(TOAST_LOGIN_ATTEMPT_PASSWD).setGravity(Gravity.CENTER, 0, 0)
            user.contains('@') ->
                toast(TOAST_LOGIN_ATTEMPT_ATMARK).setGravity(Gravity.CENTER, 0, 0)
            else -> {
                /** sk2 サーバで認証してログイン **/
                val connector = Sk2Connector()

                var result: String? = null
                runBlocking(Dispatchers.IO) {
                    result = if (connector.connect()) {
                        connector.apply("$COMMAND_AUTH,$org,$user,$passwd")
                    } else {
                        REPLY_FAIL
                    }
                    connector.close()
                }

                if (result == REPLY_FAIL || result.isNullOrBlank()) {
                    toast(TOAST_CANT_CONNECT_SERVER).setGravity(Gravity.CENTER, 0, 0)
                } else {
                    login(user, result!!)
                }
            }
        }
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    /** ログインする **/
    private fun login(user: String, result: String) {
        if (result != REPLY_AUTH_FAIL) { // サーバからの返信が失敗でなければ
            /** サーバ返信を ',' で分割 **/
            val v: List<String> = result.split(",", limit = 4)
            val key = v[0]
            val name = v[1]
            /** ユーザ名の空白は全て半角スペース一つに圧縮 **/
            val nameJp = v[2].replace(Regex("\\s+"), " ")
            val json = v[3]
            //val time: Long = System.currentTimeMillis() // 現在時刻
            //Log.d("XXX", json.last().toString())

            /** Preferences 設定 **/
            setBoolean(Sk2Preferences.Key.ACCEPT_POLICY, true)
            setString(Sk2Preferences.Key.USER, user)
            setString(Sk2Preferences.Key.SK2KEY, key)
            setString(Sk2Preferences.Key.USER_NAME, name)
            setString(Sk2Preferences.Key.USER_NAME_JP, nameJp)
            setString(Sk2Preferences.Key.ROOM_JSON, json)
            setString(Sk2Preferences.Key.APP_VERSION, BuildConfig.VERSION_NAME)
            setInt(Sk2Preferences.Key.APP_CODE, BuildConfig.VERSION_CODE)

            /** Json で Ap-Room Mas を登録 **/
            Rooms.add(json)

            //Log.d("XXX", prefs.getString(Sk2Preferences.Key.ROOM_JSON))
            toast(TOAST_LOGIN_SUCCESS).setGravity(Gravity.CENTER, 0, 0)
            /** メイン画面へ **/
            startActivity<MainActivity>()
        } else {
            // in fail
            toast(TOAST_LOGIN_FAIL).setGravity(Gravity.CENTER, 0, 0)
        }
    }
}
