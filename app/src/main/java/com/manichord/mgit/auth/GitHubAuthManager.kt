package com.manichord.mgit.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.manichord.mgit.models.Account
import com.manichord.mgit.models.AccountType
import org.json.JSONObject
import timber.log.Timber
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

class GitHubAuthManager(private val context: Context, private val accountManager: AccountManager) {

    companion object {
        private const val CLIENT_ID = "YOUR_CLIENT_ID" // User will need to replace this or we use a placeholder
        private const val CLIENT_SECRET = "YOUR_CLIENT_SECRET"
        private const val REDIRECT_URI = "mgit://github-auth"
        private const val AUTH_URL = "https://github.com/login/oauth/authorize"
        private const val TOKEN_URL = "https://github.com/login/oauth/access_token"
        private const val API_URL = "https://api.github.com"
    }

    fun launchAuth(context: Context) {
        if (CLIENT_ID == "YOUR_CLIENT_ID" || CLIENT_SECRET == "YOUR_CLIENT_SECRET") {
            android.widget.Toast.makeText(
                context,
                "GitHub OAuth is not configured. Set CLIENT_ID/CLIENT_SECRET in GitHubAuthManager.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        val uri = Uri.parse(AUTH_URL)
            .buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
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
                        val account = Account(
                            id = System.currentTimeMillis(),
                            name = "GitHub (${user.getString("login")})",
                            username = user.getString("login"),
                            token = token,
                            type = AccountType.GITHUB
                        )
                        accountManager.addAccount(account)
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
        val url = URL(TOKEN_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Accept", "application/json")
        conn.doOutput = true

        val body = "client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET&code=$code&redirect_uri=$REDIRECT_URI"
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
        conn.setRequestProperty("Authorization", "token $token")
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
                conn.setRequestProperty("Authorization", "token $token")
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
