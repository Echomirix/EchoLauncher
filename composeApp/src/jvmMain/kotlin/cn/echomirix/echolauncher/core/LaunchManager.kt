package cn.echomirix.echolauncher.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

data class CrashReport(
    val id: String = UUID.randomUUID().toString(),
    val versionName: String,
    val exitCode: Int,
    val description: String?,
    val logFile: File?
)

object LaunchManager {
    private val _activeTasks = MutableStateFlow<List<LaunchTask>>(emptyList())
    val activeTasks: StateFlow<List<LaunchTask>> = _activeTasks.asStateFlow()

    private val _crashReports = MutableStateFlow<List<CrashReport>>(emptyList())
    val crashReports: StateFlow<List<CrashReport>> = _crashReports.asStateFlow()

    fun launch(context: LaunchContext) {
        val task = LaunchTask(context)
        _activeTasks.value += task

        // 交给全局生命周期的协程去跑
        CoroutineScope(Dispatchers.IO).launch {
            try {
                task.start()
            } finally {
                // 退出后从列表移除
                _activeTasks.value -= task
            }
        }
    }

    fun reportCrash(versionName: String, exitCode: Int, description: String?, logFile: File?) {
        val report =
            CrashReport(versionName = versionName, exitCode = exitCode, description = description, logFile = logFile)
        _crashReports.value += report
    }

    // 当玩家点击“我知道了”时，清除对应的崩溃记录
    fun dismissCrashReport(id: String) {
        _crashReports.value = _crashReports.value.filter { it.id != id }
    }
}