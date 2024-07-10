package com.github.kr328.clash

import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setUUID
import com.github.kr328.clash.common.util.uuid
import com.github.kr328.clash.design.PropertiesDesign
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.design.util.showExceptionToast
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class PropertiesActivity : BaseActivity<PropertiesDesign>() {
    private var canceled: Boolean = false

    override suspend fun main() {
        setResult(RESULT_CANCELED)

        val uuid = intent.uuid ?: return finish()
        val design = PropertiesDesign(this)

        val original = withProfile { queryByUUID(uuid) } ?: return finish()

        design.profile = original

        setContentDesign(design)

        defer {
            Log.i("in defer block")
            canceled = true

            withProfile { release(uuid) }
        }

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ActivityStop -> {
                            Log.i("ActivityStop")
                            val profile = design.profile

                            if (!canceled && profile != original) {
                                Log.i("ActivityStop profile diff")
                                withProfile {
                                    patch(profile.uuid, profile.name, profile.source, profile.interval)
                                }
                            }
                        }
                        Event.ServiceRecreated -> {
                            finish()
                        }
                        else -> Unit
                    }
                }
                design.requests.onReceive {
                    when (it) {
                        PropertiesDesign.Request.BrowseFiles -> {
                            startActivity(FilesActivity::class.intent.setUUID(uuid))
                        }
                        PropertiesDesign.Request.Commit -> {
                            design.verifyAndCommit()
                        }
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        design?.apply {
            launch {
                if (!progressing) {
                    if (requestExitWithoutSaving())
                        finish()
                }
            }
        } ?: return super.onBackPressed()
    }

    private suspend fun PropertiesDesign.verifyAndCommit() {
        when {
            profile.name.isBlank() -> {
                showToast(R.string.empty_name, ToastDuration.Long)
            }
            profile.type != Profile.Type.File && profile.source.isBlank() -> {
                showToast(R.string.invalid_url, ToastDuration.Long)
            }
            else -> {
                try {
                    Log.i("verifyAndCommit")
                    withProcessing { updateStatus ->
                        Log.i("withProcessing thread: " + Thread.currentThread().name)
                        withProfile {
                            Log.i("withProfile thread: " + Thread.currentThread().name)
                            patch(profile.uuid, profile.name, profile.source, profile.interval)

                            coroutineScope {
                                commit(profile.uuid) {
                                    launch {
                                        Log.i("coroutineScope thread: " + Thread.currentThread().name)
                                        Log.i("it: " + it)
                                        updateStatus(it)
                                    }
                                }
                            }
                        }
                    }

                    setResult(RESULT_OK)

                    finish()
                } catch (e: Exception) {
                    showExceptionToast(e)
                }
            }
        }
    }
}