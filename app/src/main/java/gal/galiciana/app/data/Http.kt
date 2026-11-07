package gal.galiciana.app.data

import okhttp3.OkHttpClient
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.TimeUnit

object Http {
    private val cookies = object : CookieJar {
        private val jar = mutableListOf<Cookie>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) = synchronized(jar) {
            cookies.forEach { incoming ->
                jar.removeAll { it.name == incoming.name && it.domain == incoming.domain && it.path == incoming.path }
                jar.add(incoming)
            }
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(jar) {
            jar.removeAll { it.expiresAt < System.currentTimeMillis() }
            jar.filter { it.matches(url) }
        }
    }
    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookies)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
}
