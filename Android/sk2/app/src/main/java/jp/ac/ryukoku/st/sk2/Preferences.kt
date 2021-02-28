package jp.ac.ryukoku.st.sk2

import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object Sk2Preferences {
    private var prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationContext.applicationContext())
    //private const val SHARED_PREFERENCES_NAME = "st.ryukoku.sk3"

    enum class Key(value: Any) {
        SK2KEY(""),
        ORGANIZATION(0),
        USER(""),
        USER_NAME(""),
        USER_NAME_JP(""),
        ACCEPT_POLICY(false),
        ROOM_JSON(""),
        APP_VERSION(""),
        APP_CODE(0),
        LOGS(""),
        ;
        enum class Type {
            BOOLEAN,
            INT,
            STRING,
        }
        val type: Type
        val defaultValue: Any = value

        init {
            type = when (value) {
                is Boolean -> Type.BOOLEAN
                is Int -> Type.INT
                is String -> Type.STRING
                else -> throw IllegalArgumentException()
            }
        }
        internal fun isBooleanKey(): Boolean { return type === Type.BOOLEAN }
        internal fun isIntKey(): Boolean { return type === Type.INT }
        internal fun isStringKey(): Boolean { return type === Type.STRING }
    }

    fun clear(key: Key) {
        when (key.type) {
            Key.Type.BOOLEAN ->
                prefs.edit().putBoolean(key.name, key.defaultValue as Boolean).apply()
            Key.Type.INT ->
                prefs.edit().putInt(key.name, key.defaultValue as Int).apply()
            Key.Type.STRING ->
                prefs.edit().putString(key.name, key.defaultValue as String).apply()
        }
    }
    fun setBoolean(key: Key, value: Boolean) {
        if (!key.isBooleanKey()) throw IllegalArgumentException(key.name + " is not key for Boolean")
        prefs.edit().putBoolean(key.name, value).apply()
    }
    fun setInt(key: Key, value: Int) {
        if (!key.isIntKey()) throw IllegalArgumentException(key.name + " is not key for Int")
        prefs.edit().putInt(key.name, value).apply()
    }
    fun setString(key: Key, value: String) {
        if (!key.isStringKey()) throw IllegalArgumentException(key.name + " is not key for String")
        prefs.edit().putString(key.name, value).apply()
    }

    fun getBoolean(key: Key): Boolean {
        if (!key.isBooleanKey()) throw IllegalArgumentException(key.name + " is not key for Boolean")
        return prefs.getBoolean(key.name, key.defaultValue as Boolean)
    }
    fun getInt(key: Key): Int {
        if (!key.isIntKey()) throw IllegalArgumentException(key.name + " is not key for Int")
        return prefs.getInt(key.name, key.defaultValue as Int)
    }
    fun getString(key: Key): String {
        if (!key.isStringKey()) throw IllegalArgumentException(key.name + " is not key for String")
        return prefs.getString(key.name, key.defaultValue as String)!!
    }
}