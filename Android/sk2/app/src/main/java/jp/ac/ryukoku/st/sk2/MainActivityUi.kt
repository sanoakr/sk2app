package jp.ac.ryukoku.st.sk2


import android.graphics.Color
import android.graphics.Typeface
import android.text.InputFilter
import android.text.InputType.TYPE_CLASS_TEXT
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import androidx.core.content.ContextCompat
import jp.ac.ryukoku.st.sk2.ApplicationContext.Companion.infoQueue
import jp.ac.ryukoku.st.sk2.MainActivityUi.Companion.COLOR_MAIN
import jp.ac.ryukoku.st.sk2.MainActivityUi.Companion.TEXT_LARGE
import jp.ac.ryukoku.st.sk2.MainActivityUi.Companion.TEXT_NORMAL
import jp.ac.ryukoku.st.sk2.MainActivityUi.Companion.TEXT_SMALL
import jp.ac.ryukoku.st.sk2.Sk2Preferences.getString
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZonedDateTime
import java.util.*

/** ////////////////////////////////////////////////////////////////////////////// **/
/*** UI構成 via Anko ***/
class MainActivityUi: AnkoComponent<MainActivity>, AnkoLogger {
    companion object {
        const val MENU = 10
        const val SEARCH = 11
        const val HELP = 12
        const val EXIT = 13
        const val LIST = 20
        const val BSET = 30

        var COLOR_BACKGROUND = Color.parseColor("#fbfbfb")
        var COLOR_MAIN = Color.parseColor("#2589cd")

        const val TEXT_TINY = 6f
        const val TEXT_SMALL = 8f
        const val TEXT_NORMAL = 10f
        const val TEXT_LARGE = 14f
        const val TEXT_HUGE = 16f
        const val TEXT_SHUGE = 26f
    }
    // ListView Adapter
    val recAdapter = RecordLocalAdapter()

    override fun createView(ui: AnkoContext<MainActivity>) = with(ui) {
        // Reference to MainActivity
        val main = ui.owner


        relativeLayout {
            padding = dip(4)
            backgroundColor = COLOR_BACKGROUND
            /** ////////////////////////////////////////////////////////////////////////////// **/
            linearLayout {
                id = MENU
                /** ////////////////////////////////////////////////////////////////////////////// **/
                button("about sk2") {
                    id = HELP
                    textColor = COLOR_MAIN
                    backgroundColor = COLOR_BACKGROUND
                    textSize = TEXT_LARGE
                    allCaps = false
                    onClick {
                        browse("https://sk2.st.ryukoku.ac.jp/")
                    }
                }.lparams {
                    gravity = Gravity.CENTER_HORIZONTAL
                    margin = dip(0)
                    weight = 1f
                }
/*                /** ////////////////////////////////////////////////////////////////////////////// **/
                textView() {
                    backgroundColor = COLOR_MAIN
                }.lparams {
                    margin = dip(0)
                    padding = dip(1)
                    width = dip(1)
                    //weight = 1f
                }*/
                /** ////////////////////////////////////////////////////////////////////////////// **/
                button("Logout") {
                    id = EXIT
                    textColor = COLOR_MAIN
                    backgroundColor = COLOR_BACKGROUND
                    textSize = TEXT_LARGE
                    allCaps = false
                    onClick {
                        alert("ログアウトしますか？", "確認") {
                            positiveButton("ログアウトします") { _ -> //sk2.readyTologout()
                                ui.owner.logout()
                            }
                            negativeButton("キャンセル") { _ -> }
                        }.show()
                    }
                }.lparams {
                    gravity = Gravity.CENTER_HORIZONTAL
                    margin = dip(0)
                    weight = 1f
                }
/*                /** /////////////////////////////////// **/
                verticalLayout {
                    imageButton {
                        id = SEARCH
                        imageResource = R.drawable.ic_outline_find_in_page_24
                        backgroundColor = Sk2Globals.COLOR_BACKGROUND
                        onClick { _ ->
                            startActivity<SearchActivity>()
                        }
                    }.lparams {
                        width = dip(75)
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                    textView("Search") {
                        labelFor = SEARCH
                    }.lparams {
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                }
                /** /////////////////////////////////// **/
                verticalLayout {
                    imageButton {
                        id = HELP
                        imageResource = R.drawable.ic_baseline_error_outline_24
                        backgroundColor = Sk2Globals.COLOR_BACKGROUND
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        onClick { _ ->
                            startActivity<HelpActivity>()
                        }
                    }.lparams {
                        width = dip(75)
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                    textView("Help") {
                        labelFor = HELP
                    }.lparams {
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                }
                /** /////////////////////////////////// **/
                verticalLayout {
                    imageButton {
                        id = EXIT
                        imageResource = R.drawable.ic_outline_exit_to_app_24
                        backgroundColor = Sk2Globals.COLOR_BACKGROUND
                        onClick {
                            alert("ログアウトしますか？", "確認") {
                                positiveButton("ログアウトします") { _ -> //sk2.readyTologout()
                                    ui.owner.logout()
                                    //ui.owner.startActivity(intentFor<LoginActivity>().clearTop())
                                }
                                negativeButton("キャンセル") { _ -> }
                            }.show()
                        }
                    }.lparams {
                        width = dip(75)
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                    textView("Logout") {
                        labelFor = EXIT
                    }.lparams {
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                }
 */
            }.lparams {
                width = matchParent
                centerHorizontally()
                alignParentTop()
            }
            /** ////////////////////////////////////////////////////////////////////////////// **/
            verticalLayout {
                id = LIST
                gravity = Gravity.TOP
                // Vertical Line
                view { backgroundColor = COLOR_MAIN
                }.lparams { width = matchParent; height = dip(1); verticalMargin = dip(8) }
                /** ////////////////////////////////////////////////////////////////////////////// **/
                /** データ表示用 ListView **/
                listView {
                    doAsync {
                        uiThread {
                            adapter = recAdapter
                            //if (recAdapter.list.isEmpty())
                            //    toast(TOAST_LOG_NO_LOCAL_RECORDS).setGravity(Gravity.CENTER, 0, 0)
                        }
                    }
                }
                // Vertical Line
                view { backgroundColor = COLOR_MAIN
                }.lparams { width = matchParent; height = dip(1); verticalMargin = dip(8) }

            }.lparams {
                below(MENU)
                above(BSET)
            }
            /** ////////////////////////////////////////////////////////////////////////////// **/
            verticalLayout {
                id = BSET
                // Vertical Line
                view { backgroundColor = COLOR_MAIN
                }.lparams { width = matchParent; height = dip(1); verticalMargin = dip(8) }
                /** ////////////////////////////////////////////////////////////////////////////// **/
                val userText = editText {
                    inputType = TYPE_CLASS_TEXT
                    filters = arrayOf(InputFilter.LengthFilter(16))
                    hint = "送信文字列（max16文字）"
                    textSize = TEXT_NORMAL
                    leftPadding = dip(8)
                    backgroundColor = Color.LTGRAY
                    hintTextColor = Color.DKGRAY
                }.lparams {
                    height = wrapContent
                    width = matchParent
                    verticalMargin = dip(4)
                }
                /** ////////////////////////////////////////////////////////////////////////////// **/
                button("出席") {
                    backgroundColor = COLOR_MAIN
                    textColor = Color.WHITE
                    textSize = TEXT_SHUGE
                    onClick {
                        var locations = mutableListOf<String>()
                        SType.values().forEach { locations.add(it.message()) }
                        locations.add("キャンセル")
                        //info(locations) // [, 自宅・学外から送信します, 龍大瀬田キャンパスから送信します, その他の龍大施設から送信します, キャンセル]
                        selector("手動送信を行いますか？", locations.subList(1,5)) { _, i ->
                            if (i != 3) { // subList(1,5) = [0,3]:[自宅・学外から送信します, 龍大瀬田キャンパスから送信します, その他の龍大施設から送信します, キャンセル]
                                main.sendAttend(null, userText.text.toString(), SType.fromInt(i + 1), true) // SType は Auto(0) があるので +1
                            }
                        }
                        //main.startMonitoring()
                    }
                }.lparams {
                    height = wrapContent
                    width = matchParent
                    verticalMargin = dip(4)
                }
                /** ////////////////////////////////////////////////////////////////////////////// **/
                linearLayout {
                    textView("${getString(Sk2Preferences.Key.USER)} / ${getString(Sk2Preferences.Key.USER_NAME_JP)}") {
                        textSize = TEXT_NORMAL
                        gravity = Gravity.START
                    }.lparams {
                        weight = 1f
                    }
                    textView("sk2 Version ${Sk2Preferences.getString(Sk2Preferences.Key.APP_VERSION)} (${Sk2Preferences.getInt(Sk2Preferences.Key.APP_CODE)})") {
                        textSize = TEXT_SMALL
                        gravity = Gravity.END
                    }.lparams {
                        weight = 1f
                    }
                }.lparams {
                    width = matchParent
                }
            }.lparams {
                horizontalMargin = dip(4)
                height = wrapContent; width = matchParent
                alignParentBottom(); centerHorizontally()
            }
        }
    }
}


/** ////////////////////////////////////////////////////////////////////////////// **/
/** UI **/
class RecordLocalAdapter: BaseAdapter(), AnkoLogger {
    private val list = infoQueue
    private val recordFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE HH:mm:ss", Locale.JAPAN)

    /** ////////////////////////////////////////////////////////////////////////////// **/
    override fun getView(i: Int, v: View?, parent: ViewGroup?): View {
        info(list)
        val success = getItem(i).Success
        val datetime = getItem(i).Datetime
        val displayDateTime = LocalDateTime.parse(datetime, recordFormatter).format(displayFormatter).toString()
        val stype = getItem(i).Stype
        val latitude = getItem(i).Latitude
        val longitude = getItem(i).Longitude
        val beacons: MutableList<Triple<Int?, Int?, String?>> = mutableListOf()
        beacons.add(Triple(getItem(i).Major1,getItem(i).Minor1,getItem(i).Room1))
        beacons.add(Triple(getItem(i).Major2,getItem(i).Minor2,getItem(i).Room2))
        beacons.add(Triple(getItem(i).Major3,getItem(i).Minor3,getItem(i).Room3))

        return with(parent!!.context) {
            /** ////////////////////////////////////////////////////////////////////////////// **/
            verticalLayout {
                /** ////////////////////////////////////////////////////////////////////////////// **/
                linearLayout {
                    padding = dip(4)
                    /** ////////////////////////////////////////////////////////////////////////////// **/
                    textView(displayDateTime) {
                        textSize = TEXT_NORMAL
                        backgroundColor = Color.WHITE // for Huwai's initAdditionalStyle default Error.
                        typeface = Typeface.DEFAULT_BOLD
                        textColor = if (success) COLOR_MAIN else Color.RED
                    }.lparams { horizontalGravity = left; weight = 1f
                    /** ////////////////////////////////////////////////////////////////////////////// **/
                    //textView("${stype} (${"%.2f".format(latitude)}, ${"%.2f".format(longitude)})") {
                    textView("${stype}") {
                            textSize = TEXT_NORMAL
                            textColor = Color.GREEN
                            backgroundColor = Color.WHITE // for Huwai's initAdditionalStyle default Error.
                            typeface = Typeface.DEFAULT_BOLD
                        }.lparams { horizontalGravity = left }
                    }
                }.lparams {
                    width = matchParent
                }
                /** ////////////////////////////////////////////////////////////////////////////// **/
                verticalLayout {
                    for (b in beacons) {
                        val (major, minor, notes) = b
                        if (major != null && minor != null) {
                            /** ////////////////////////////////////////////////////////////////////////////// **/
                            linearLayout {
                                ////////////////////////////////////////
                                textView(notes ?: "Unknown") {
                                    textSize = TEXT_SMALL
                                }.lparams {
                                    horizontalGravity = left
                                }
                                ////////////////////////////////////////
                                /*
                                textView("(${major}, ${minor})") {
                                    textSize = TEXT_SMALL
                                    textColor = Color.BLACK
                                }.lparams {
                                    horizontalGravity = left
                                    horizontalMargin = dip(8)
                                }
                                 */
                            }.lparams {
                                horizontalMargin = dip(3)
                            }
                        }
                    }
                }.lparams { bottomMargin = dip(4); horizontalMargin = dip(16) }
            }
        }
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    override fun getItem(position: Int): BeaconLog {
        return list.get(position)
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    override fun getCount(): Int {
        return list.count()
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}
