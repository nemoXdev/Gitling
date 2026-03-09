package com.manichord.mgit.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.manichord.mgit.models.Account
import com.manichord.mgit.models.AccountType
import me.sheimi.sgit.preference.PreferenceHelper
import org.json.JSONObject
import timber.log.Timber
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Scanner

class GitHubAuthManager(private val context: Context, private val accountManager: AccountManager) {

    companion object {
        private const val REDIRECT_URI = "mgit://github-auth"
        private const val AUTH_URL = "https://github.com/login/oauth/authorize"
        private const val TOKEN_URL = "https://github.com/login/oauth/access_token"
        private const val API_URL = "https://api.github.com"
    }

    private val prefs = PreferenceHelper(context)

    fun launchAuth(context: Context) {
        val clientId = prefs.getGitHubOAuthClientId()
        val clientSecret = prefs.getGitHubOAuthClientSecret()

        if (clientId.isBlank() || clientSecret.isBlank()) {
            android.widget.Toast.makeText(
                context,
                "GitHub OAuth is not configured. Please set Client ID and Secret in Settings.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        val uri = Uri.parse(AUTH_URL)
            .buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", "repo,user")
            .build()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }

    fun handleAuthCallback(uri: Uri, onComplete: (Boolean) -> Unit) {
        // OAuth error returned by GitHub (e.g. user cancelled)
        uri.getQueryParameter("error")?.let { error ->
            android.widget.Toast.makeText(
                context,
                "GitHub authorization failed: $error",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return onComplete(false)
        }

        val code = uri.getQueryParameter("code") ?: run {
            android.widget.Toast.makeText(
                context,
                "GitHub authorization failed: missing code",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return onComplete(false)
        }

        Thread {
            try {
                val token = fetchToken(code)
                if (token != null) {
                    val user = fetchUser(token)
                    if (user != null) {
                        val username = user.getString("login")
                        val existing = accountManager.getAccounts()
                            .find { it.username == username && it.type == AccountType.GITHUB }
                        if (existing != null) {
                            accountManager.updateAccount(existing.copy(token = token))
                        } else {
                            accountManager.addAccount(
                                Account(
                                    name = "GitHub ($username)",
                                    username = username,
                                    token = token,
                                    type = AccountType.GITHUB
                                )
                            )
                        }
                        onComplete(true)
                    } else {
                        onComplete(false)
                    }
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                Timber.e(e, "GitHub Auth error")
                onComplete(false)
            }
        }.start()
    }

    private fun fetchToken(code: String): String? {
        val clientId = prefs.getGitHubOAuthClientId()
        val clientSecret = prefs.getGitHubOAuthClientSecret()

        val url = URL(TOKEN_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.doOutput = true

        fun encode(s: String) = URLEncoder.encode(s, "UTF-8")
        val body = "client_id=${encode(clientId)}&client_secret=${encode(clientSecret)}" +
            "&code=${encode(code)}&redirect_uri=${encode(REDIRECT_URI)}"
        OutputStreamWriter(conn.outputStream).use { it.write(body) }

        if (conn.responseCode == 200) {
            val response = Scanner(conn.inputStream).useDelimiter("\\A").next()
            return JSONObject(response).optString("access_token")
        }
        return null
    }

    private fun fetchUser(token: String): JSONObject? {
        val url = URL("$API_URL/user")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Accept", "application/json")

        if (conn.responseCode == 200) {
            val response = Scanner(conn.inputStream).useDelimiter("\\A").next()
            return JSONObject(response)
        }
        return null
    }

    fun fetchRepos(token: String, onResult: (List<JSONObject>) -> Unit) {
        Thread {
            try {
                val url = URL("$API_URL/user/repos?sort=updated&per_page=100")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Accept", "application/json")

                if (conn.responseCode == 200) {
                    val response = Scanner(conn.inputStream).useDelimiter("\\A").next()
                    val reposJson = org.json.JSONArray(response)
                    val repos = mutableListOf<JSONObject>()
                    for (i in 0 until reposJson.length()) {
                        repos.add(reposJson.getJSONObject(i))
                    }
                    onResult(repos)
                } else {
                    onResult(emptyList())
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching repos")
                onResult(emptyList())
            }
        }.start()
    }
}
