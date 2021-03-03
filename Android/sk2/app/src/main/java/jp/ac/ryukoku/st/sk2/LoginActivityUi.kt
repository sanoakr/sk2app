package jp.ac.ryukoku.st.sk2

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType.*
import android.text.Spannable
import android.text.style.BulletSpan
import android.text.style.LeadingMarginSpan
import android.view.Gravity
import androidx.core.view.marginBottom
import jp.ac.ryukoku.st.sk2.LoginActivity.Companion.TOAST_LOGIN_ATTEMPT_ATMARK
import jp.ac.ryukoku.st.sk2.LoginActivity.Companion.TOAST_LOGIN_ATTEMPT_PASSWD
import jp.ac.ryukoku.st.sk2.LoginActivity.Companion.TOAST_LOGIN_ATTEMPT_UID
import jp.ac.ryukoku.st.sk2.MainActivityUi.Companion.COLOR_BACKGROUND
import jp.ac.ryukoku.st.sk2.MainActivityUi.Companion.COLOR_MAIN
import jp.ac.ryukoku.st.sk2.MainActivityUi.Companion.TEXT_HUGE
import jp.ac.ryukoku.st.sk2.MainActivityUi.Companion.TEXT_LARGE
import jp.ac.ryukoku.st.sk2.MainActivityUi.Companion.TEXT_NORMAL
import jp.ac.ryukoku.st.sk2.MainActivityUi.Companion.TEXT_TINY
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.APP_NAME
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.APP_TITLE
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.COLOR_HINT
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.COLOR_NORMAL_LIGHT
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.NAME_START_TESTUSER
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.SERVER_ORGNIZATION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick

//@Suppress("EXPERIMENTAL_FEATURE_WARNING")
/** ////////////////////////////////////////////////////////////////////////////// **/
/** UI構成 via Anko **/
class LoginActivityUi: AnkoComponent<LoginActivity> {
    companion object {
        const val TITLE = 1
        const val LABEL = 2
        const val USER = 3
        const val PASS = 4
        const val LOGIN = 5

        const val PRIVACY_URI = "https://sk2.st.ryukoku.ac.jp/privacy.html"
        const val HINT_UID = "全学統合認証ID"
        const val HINT_PASSWD = "パスワード"
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    @SuppressLint("SetJavaScriptEnabled")
    override fun createView(ui: AnkoContext<LoginActivity>) = with(ui) {
        relativeLayout {
            padding = dip(16)
            backgroundColor = COLOR_BACKGROUND
            /** ////////////////////////////////////////////////////////////////////////////// **/
            textView("$APP_TITLE $APP_NAME") {
                id = TITLE
                textSize = TEXT_HUGE
                textColor = COLOR_MAIN
            }.lparams{
                centerHorizontally(); alignParentTop()
                topMargin = dip(100); bottomMargin = dip(50)
            }
            /** ////////////////////////////////////////////////////////////////////////////// **/
            textView("$HINT_UID / $HINT_PASSWD") {
                id = LABEL
                textSize = TEXT_LARGE
                textColor = Color.BLACK
            }.lparams{
                below(TITLE); leftMargin = dip(4)
            }
            /** ////////////////////////////////////////////////////////////////////////////// **/
            val user = editText {
                id = USER
                textSize = TEXT_HUGE
                hint = HINT_UID
                hintTextColor = COLOR_HINT
                inputType = TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }.lparams { below(LABEL); width = matchParent; height = dip(50) }
            /** ////////////////////////////////////////////////////////////////////////////// **/
            val passwd = editText {
                id = PASS
                textSize = TEXT_HUGE
                hint = HINT_PASSWD
                hintTextColor = COLOR_HINT
                inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
            }.lparams { below(USER); width = matchParent; height = dip(50) }
            /** ////////////////////////////////////////////////////////////////////////////// **/
            button("ログイン") {
                id = LOGIN
                textColor = Color.WHITE
                textSize = TEXT_HUGE
                padding = dip(0)
                backgroundColor = COLOR_NORMAL_LIGHT
                //backgroundColor = COLOR_NORMAL
                onClick {
                    when {
                        //　ユーザー名が空
                        user.text.toString().isBlank() ->
                            toast(TOAST_LOGIN_ATTEMPT_UID).setGravity(Gravity.CENTER, 0, 0)
                        // テストユーザーではなく、パスワードが空
                        !user.text.toString().startsWith(NAME_START_TESTUSER)
                                && passwd.text.toString().isBlank() ->
                            toast(TOAST_LOGIN_ATTEMPT_PASSWD).setGravity(
                                    Gravity.CENTER,
                                    0,
                                    0
                            )
                        // ユーザー名がメールアドレス（@ が含まれている）
                        user.text.toString().contains('@') ->
                            toast(TOAST_LOGIN_ATTEMPT_ATMARK).setGravity(
                                    Gravity.CENTER,
                                    0,
                                    0
                            )
                        else ->
                            // 1つめの説明
                            alert {
                                customView {
                                    verticalLayout {
                                        padding = dip(32)
                                        textView("sk2 は大学キャンパス内でのみ、\nバックグラウンドの位置情報を取得します") {
                                            textSize = TEXT_LARGE
                                            textColor = Color.BLUE
                                            typeface = Typeface.DEFAULT_BOLD
                                        }.lparams { weight = 1f }
                                        textView("あいうえお") {
                                            textSize = TEXT_LARGE
                                            textColor = Color.BLACK
                                        }.lparams { weight = 1f }
                                        imageView(R.drawable.explain1) {

                                        }.lparams { weight = 1f }
                                        textView("かきくけこ") {
                                            textSize = TEXT_NORMAL
                                            textColor = Color.BLACK
                                        }.lparams { weight = 1f }
                                    }
                                }
                                positiveButton("次へ") { _ ->

                                    // 2つめの説明
                                    alert {
                                        customView {
                                            padding = dip(8)
                                            verticalLayout {
                                                textView("sk2 は手動送信でのみ、\n緯度経度を含むおおよその位置情報を取得します") {
                                                    textSize = TEXT_LARGE
                                                    textColor = Color.BLUE
                                                    typeface = Typeface.DEFAULT_BOLD
                                                }
                                                imageView(R.drawable.explain2)
                                            }
                                        }
                                        positiveButton("次へ") { _ ->

                                            // 最後にプライバシーポリシーを承認
                                            alert {
                                                customView {
                                                    verticalLayout {
                                                        textView("sk2 プライバシーポリシーの承認") {
                                                            textSize = TEXT_HUGE
                                                            textColor = Color.WHITE
                                                            backgroundColor = COLOR_NORMAL_LIGHT
                                                            padding = dip(8)
                                                        }.lparams {
                                                            width = matchParent
                                                        }
                                                        webView {
                                                            settings.javaScriptEnabled = true
                                                            //clearCache(true)
                                                            loadUrl(PRIVACY_URI)
                                                        }
                                                    }
                                                }
                                                positiveButton("同意します") { _ ->
                                                    //runBlocking(Dispatchers.IO) {
                                                    ui.owner.attemptLogin(
                                                            user.text.toString().trim(),
                                                            passwd.text.toString(),
                                                            SERVER_ORGNIZATION
                                                    )
                                                    //}
                                                }
                                                negativeButton("同意しません") { }
                                            }.show()
                                        }
                                    }.show()
                                }
                            }.show()
                    }
                }
            }.lparams {
                below(PASS); centerHorizontally(); width = matchParent
                topMargin = dip(30)
            }
            /** ////////////////////////////////////////////////////////////////////////////// **/
            textView("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})") {
                //id = MainActivityUi.VERSION
                textColor = Color.BLACK
                textSize = TEXT_TINY
            }.lparams {
                alignParentBottom(); alignParentEnd()
            }
        }
    }
}
