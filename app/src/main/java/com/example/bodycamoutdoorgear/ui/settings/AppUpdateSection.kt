package com.example.bodycamoutdoorgear.ui.settings

import com.example.bodycamoutdoorgear.i18n.trf
import com.example.bodycamoutdoorgear.i18n.tr
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.launch
import com.google.android.play.core.ktx.requestAppUpdateInfo

private sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    object UpToDate : UpdateState
    data class Available(val info: AppUpdateInfo) : UpdateState
    object Failed : UpdateState
}

/**
 * "Check for updates" row for the Settings sheet. Asks Google Play whether a
 * newer version is published; if so, offers to start Play's own update flow.
 * Only works for builds installed from Google Play — a sideloaded build gets
 * the failed state, whose button opens the Play Store listing instead.
 */
@Composable
fun AppUpdateSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { AppUpdateManagerFactory.create(context) }
    var state by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }

    val versionName = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (_: Exception) { null }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }

    fun openPlayStore() {
        val pkg = context.packageName
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(tr("APP UPDATE"))
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (versionName != null) HelperText(trf("Installed version %s.", versionName))
            when (val s = state) {
                UpdateState.Idle, UpdateState.UpToDate, UpdateState.Failed, UpdateState.Checking -> {
                    SettingsRowButton(if (s == UpdateState.Checking) tr("CHECKING…") else tr("CHECK FOR UPDATES")) {
                        if (s == UpdateState.Checking) return@SettingsRowButton
                        state = UpdateState.Checking
                        scope.launch {
                            state = try {
                                val info = manager.requestAppUpdateInfo()
                                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                                    info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                                ) UpdateState.Available(info) else UpdateState.UpToDate
                            } catch (_: Exception) {
                                UpdateState.Failed
                            }
                        }
                    }
                    when (s) {
                        UpdateState.UpToDate -> HelperText(tr("You're on the newest version."))
                        UpdateState.Failed -> {
                            HelperText(tr("Couldn't check from here. This works for installs from Google Play; you can also open the Play Store page directly."))
                            SettingsRowButton(tr("OPEN IN PLAY STORE")) { openPlayStore() }
                        }
                        else -> {}
                    }
                }
                is UpdateState.Available -> {
                    HelperText(tr("A new version is available."))
                    SettingsRowButton(tr("UPDATE NOW")) {
                        try {
                            manager.startUpdateFlowForResult(
                                s.info,
                                launcher,
                                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                            )
                        } catch (_: Exception) {
                            state = UpdateState.Failed
                        }
                    }
                }
            }
        }
    }
}
