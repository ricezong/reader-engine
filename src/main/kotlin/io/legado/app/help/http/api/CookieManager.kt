package io.legado.app.help.http.api

interface CookieManager {
    fun setCookie(url: String, cookie: String?)
    fun replaceCookie(url: String, cookie: String)
    fun getCookie(url: String): String
    fun removeCookie(url: String)
    fun cookieToMap(cookie: String): MutableMap<String, String>
    fun mapToCookie(cookieMap: Map<String, String>?): String?
}
