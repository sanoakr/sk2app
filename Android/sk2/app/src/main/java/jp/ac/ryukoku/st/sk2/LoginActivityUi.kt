package jp.ac.ryukoku.st.sk2

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType.*
import android.view.Gravity
import android.view.View
import android.view.ViewManager
import android.widget.ImageView
import android.widget.LinearLayout
import jp.ac.ryukoku.st.sk2.LoginActivity.Companion.TOAST_LOGIN_ATTEMPT_ATMARK
import jp.ac.ryukoku.st.sk2.LoginActivity.Companion.TOAST_LOGIN_ATTEMPT_PASSWD
import jp.ac.ryukoku.st.sk2.LoginActivity.Companion.TOAST_LOGIN_ATTEMPT_UID
import jp.ac.ryukoku.st.sk2.MainActivityUi.Companion.COLOR_BACKGROUND
import jp.ac.ryukoku.st.sk2.MainActivityUi.Companion.COLOR_MAIN
import jp.ac.ryukoku.st.sk2.MainActivityUi.Companion.TEXT_HUGE
import jp.ac.ryukoku.st.sk2.MainActivityUi.Companion.TEXT_LARGE
import jp.ac.ryukoku.st.sk2.MainActivityUi.Companion.TEXT_TINY
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.APP_NAME
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.APP_TITLE
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.COLOR_HINT
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.COLOR_NORMAL_LIGHT
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.NAME_START_TESTUSER
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.SERVER_ORGNIZATION
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
            }.lparams {
                centerHorizontally(); alignParentTop()
                topMargin = dip(100); bottomMargin = dip(50)
            }
            /** ////////////////////////////////////////////////////////////////////////////// **/
            textView("$HINT_UID / $HINT_PASSWD") {
                id = LABEL
                textSize = TEXT_LARGE
                textColor = Color.BLACK
            }.lparams {
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
                                        lparams(width = matchParent, height = matchParent)

                                        textView("sk2 は教室位置情報をバックグラウンドで取得します") {
                                            textSize = TEXT_HUGE
                                            textColor = Color.BLUE
                                            typeface = Typeface.DEFAULT_BOLD
                                        }.lparams {
                                            width = wrapContent
                                            height = wrapContent
                                            margin = dip(32)
                                        }

                                        imageView(R.drawable.explain1) {
                                            scaleType = ImageView.ScaleType.CENTER_INSIDE
                                        }.lparams {
                                            width = matchParent
                                            height = dip(150)
                                            padding = dip(0)
                                            margin = dip(16)
                                        }
                                        listedTextView("sk2アプリを一度起動しておけば、自動で出席記録が行われます。")
                                        listedTextView("教室移動などでビーコン信号が変化すると自動で出席情報が送信されます。")
                                        listedTextView("自動送信は、龍谷大学瀬田キャンパス内で、授業実施時間にのみ実行されます。")
                                        listedTextView("自動送信の記録頻度は最短でも10分に1回です。")
                                    }
                                }
                                positiveButton("次へ") { _ ->
                                    // 2つめの説明
                                    alert {
                                        customView {
                                            verticalLayout {
                                                padding = dip(32)
                                                lparams(width = matchParent, height = matchParent)

                                                textView("sk2 は手動送信でのみ、緯度経度を含むおおよその位置情報を取得します") {
                                                    textSize = TEXT_HUGE
                                                    textColor = Color.BLUE
                                                    typeface = Typeface.DEFAULT_BOLD
                                                }.lparams {
                                                    width = wrapContent
                                                    height = wrapContent
                                                    margin = dip(32)
                                                }

                                                imageView(R.drawable.explain2) {
                                                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                                                }.lparams {
                                                    width = matchParent
                                                    height = dip(150)
                                                    padding = dip(0)
                                                    margin = dip(16)
                                                }

                                                listedTextView("手動送信は「出席」ボタンで表示される「送信場所」を選択すると実行されます。")
                                                listedTextView("手動記録では、GPSや基地局情報などからおおよその緯度経度情報が送信されます。")
                                                listedTextView("手動記録は、オンライン授業での出席記録や災害時の安否確認などに利用されます。")
                                            }
                                        }
                                        positiveButton("次へ") { _ ->
                                            // 最後にプライバシーポリシーを承認
                                            alert {
                                                customView {
                                                    verticalLayout {
                                                        lparams(width = matchParent, height = wrapContent)
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

    /** ////////////////////////////////////////////////////////////////////////////// **/
    fun ViewManager.listedTextView(text: String) = linearLayout {
        padding = dip(16)
        lparams(width = matchParent, height = wrapContent)
        textView("● ") {
            textSize = TEXT_LARGE
            textColor = Color.BLUE
        }.lparams {
            width = wrapContent
            height = wrapContent
        }
        textView(text) {
            textSize = TEXT_LARGE
            textColor = Color.BLACK
        }.lparams {
            width = wrapContent
            height = wrapContent
        }
    }
}
