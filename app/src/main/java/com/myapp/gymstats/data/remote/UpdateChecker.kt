package com.myapp.gymstats.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL

@Serializable
data class GithubRelease(
    val tag_name: String,
    val name: String,
    val body: String,
    val assets: List<GithubAsset>
)

@Serializable
data class GithubAsset(
    val name: String,
    val browser_download_url: String
)

data class UpdateInfo(
    val latestVersion: String,
    val versionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val hasUpdate: Boolean
)

object UpdateChecker {
    private const val RELEASES_URL =
        "https://api.github.com/repos/miquelroju/gym_stats/releases/latest"

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val response = URL(RELEASES_URL).readText()
            val release = json.decodeFromString<GithubRelease>(response)

            val remoteCode = release.tag_name
                .removePrefix("v")
                .replace(".", "")
                .toIntOrNull() ?: return@runCatching null

            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                ?: return@runCatching null

            UpdateInfo(
                latestVersion = release.tag_name,
                versionCode = remoteCode,
                downloadUrl = apkAsset.browser_download_url,
                releaseNotes = release.body,
                hasUpdate = remoteCode > currentVersionCode
            )
        }.getOrNull()
    }

    suspend fun downloadAndInstall(context: Context, downloadUrl: String) = withContext(Dispatchers.IO) {
        val apkFile = File(context.cacheDir, "update.apk")
        URL(downloadUrl).openStream().use { input ->
            apkFile.outputStream().use { output -> input.copyTo(output) }
        }
        withContext(Dispatchers.Main) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}