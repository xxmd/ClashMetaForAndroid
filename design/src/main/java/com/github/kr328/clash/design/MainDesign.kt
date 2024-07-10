package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.core.model.FetchStatus
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.core.util.trafficTotal
import com.github.kr328.clash.design.databinding.DesignAboutBinding
import com.github.kr328.clash.design.databinding.DesignMainBinding
import com.github.kr328.clash.design.dialog.ModelProgressBarConfigure
import com.github.kr328.clash.design.dialog.withModelProgressBar
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.resolveThemedColor
import com.github.kr328.clash.design.util.root
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainDesign(context: Context) : Design<MainDesign.Request>(context) {
    enum class Request {
        ToggleStatus,
        OpenProxy,
        OpenProfiles,
        OpenProviders,
        OpenLogs,
        OpenSettings,
        OpenHelp,
        OpenAbout,
    }

    private val binding = DesignMainBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    suspend fun setProfileName(name: String?) {
        withContext(Dispatchers.Main) {
            binding.profileName = name
        }
    }

    suspend fun setClashRunning(running: Boolean) {
        withContext(Dispatchers.Main) {
            binding.clashRunning = running
        }
    }

    suspend fun setForwarded(value: Long) {
        withContext(Dispatchers.Main) {
            binding.forwarded = value.trafficTotal()
        }
    }

    suspend fun setMode(mode: TunnelState.Mode) {
        withContext(Dispatchers.Main) {
            binding.mode = when (mode) {
                TunnelState.Mode.Direct -> context.getString(R.string.direct_mode)
                TunnelState.Mode.Global -> context.getString(R.string.global_mode)
                TunnelState.Mode.Rule -> context.getString(R.string.rule_mode)
                else -> context.getString(R.string.rule_mode)
            }
        }
    }

    suspend fun setHasProviders(has: Boolean) {
        withContext(Dispatchers.Main) {
            binding.hasProviders = has
        }
    }

    suspend fun showAbout(versionName: String) {
        withContext(Dispatchers.Main) {
            val binding = DesignAboutBinding.inflate(context.layoutInflater).apply {
                this.versionName = versionName
            }

            AlertDialog.Builder(context)
                .setView(binding.root)
                .show()
        }
    }

    init {
        binding.self = this

        binding.colorClashStarted = context.resolveThemedColor(R.attr.colorPrimary)
        binding.colorClashStopped = context.resolveThemedColor(R.attr.colorClashStopped)
    }

    fun request(request: Request) {
        requests.trySend(request)
    }

    suspend fun withProcessing(executeTask: suspend (suspend (FetchStatus) -> Unit) -> Unit) {
        try {
            context.withModelProgressBar {
                configure {
                    isIndeterminate = true
                    text = context.getString(R.string.initializing)
                }

                executeTask {
                    configure {
                        applyFrom(it)
                    }
                }
            }
        } finally {
        }
    }

    private fun ModelProgressBarConfigure.applyFrom(status: FetchStatus) {
        when (status.action) {
            FetchStatus.Action.FetchConfiguration -> {
                text = context.getString(R.string.format_fetching_configuration, status.args[0])
                isIndeterminate = true
            }
            FetchStatus.Action.FetchProviders -> {
                text = context.getString(R.string.format_fetching_provider, status.args[0])
                isIndeterminate = false
                max = status.max
                progress = status.progress
            }
            FetchStatus.Action.Verifying -> {
                text = context.getString(R.string.verifying)
                isIndeterminate = false
                max = status.max
                progress = status.progress
            }
        }
    }
}