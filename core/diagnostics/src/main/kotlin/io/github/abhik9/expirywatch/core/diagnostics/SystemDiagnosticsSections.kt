package io.github.abhik9.expirywatch.core.diagnostics

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.content.pm.PackageInfoCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.expirywatch.core.common.AppInfo
import io.github.abhik9.expirywatch.core.common.diagnostics.DiagnosticsSection
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

internal class AppDiagnosticsSection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appInfo: AppInfo,
) : DiagnosticsSection {
    override val title = "App"

    override suspend fun describe(): List<String> {
        val info = context.packageInfo()
        return listOf(
            "Version: ${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)}), ${context.packageName}",
            "Barcode scanning: ${appInfo.barcodeEngine}, debuggable: ${context.isDebuggable}",
            "Installed: ${info.firstInstallTime.asDateTime()}, updated: ${info.lastUpdateTime.asDateTime()}",
        )
    }

    @Suppress("DEPRECATION")
    private fun Context.packageInfo(): PackageInfo = if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        packageManager.getPackageInfo(packageName, 0)
    }
}

internal class DeviceDiagnosticsSection @Inject constructor(
    @ApplicationContext private val context: Context,
) : DiagnosticsSection {
    override val title = "Device"

    override suspend fun describe(): List<String> = buildList {
        val power = context.getSystemService(PowerManager::class.java)
        add("Device: ${Build.MANUFACTURER} ${Build.MODEL}, build ${Build.DISPLAY}")
        add("Android: ${Build.VERSION.RELEASE} (API $SDK_INT)")
        add("Locale: ${Locale.getDefault().toLanguageTag()}, time zone: ${ZoneId.systemDefault()}")
        add("Power save mode: ${power.isPowerSaveMode}")
        add("Ignoring battery optimizations: ${power.isIgnoringBatteryOptimizations(context.packageName)}")
        if (SDK_INT >= Build.VERSION_CODES.P) {
            val activities = context.getSystemService(ActivityManager::class.java)
            add("Background restricted: ${activities.isBackgroundRestricted}")
            add("Standby bucket: ${standbyBucket(context.getSystemService(UsageStatsManager::class.java))}")
        }
    }

    // The restricted bucket is newer than the minimum SDK, but is only ever returned where it exists.
    @SuppressLint("InlinedApi")
    @RequiresApi(Build.VERSION_CODES.P)
    private fun standbyBucket(usageStats: UsageStatsManager): String {
        val bucket = usageStats.appStandbyBucket
        val name = when (bucket) {
            UsageStatsManager.STANDBY_BUCKET_ACTIVE -> "active"
            UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> "working set"
            UsageStatsManager.STANDBY_BUCKET_FREQUENT -> "frequent"
            UsageStatsManager.STANDBY_BUCKET_RARE -> "rare"
            UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> "restricted"
            else -> "other"
        }
        return "$name ($bucket)"
    }
}

/** Why Android ended the app's recent processes, e.g. a crash, low memory or the user. */
internal class ProcessExitsDiagnosticsSection @Inject constructor(
    @ApplicationContext private val context: Context,
) : DiagnosticsSection {
    override val title = "Process exits"

    override suspend fun describe(): List<String> = if (SDK_INT >= Build.VERSION_CODES.R) {
        exits().ifEmpty { listOf("None") }
    } else {
        listOf("Unavailable before Android 11")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun exits(): List<String> = context.getSystemService(ActivityManager::class.java)
        .getHistoricalProcessExitReasons(context.packageName, 0, MAX_EXITS)
        .map { exit ->
            val reason = EXIT_REASONS.getOrNull(exit.reason) ?: "reason ${exit.reason}"
            "${exit.timestamp.asDateTime()} $reason, status ${exit.status}, importance ${exit.importance}: " +
                exit.description.orEmpty()
        }

    private companion object {
        const val MAX_EXITS = 10

        /** ApplicationExitInfo reasons by value, since some constants are newer than the minimum SDK. */
        val EXIT_REASONS = listOf(
            "unknown", "exit self", "signaled", "low memory", "crash", "native crash", "ANR",
            "initialization failure", "permission change", "excessive resource usage", "user requested",
            "user stopped", "dependency died", "other", "freezer", "package state change", "package updated",
        )
    }
}

private fun Long.asDateTime(): OffsetDateTime =
    OffsetDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS)
