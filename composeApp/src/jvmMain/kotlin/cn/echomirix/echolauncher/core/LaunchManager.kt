package cn.echomirix.echolauncher.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object LaunchManager {
    private val _activeTasks = MutableStateFlow<List<LaunchTask>>(emptyList())
    val activeTasks: StateFlow<List<LaunchTask>> = _activeTasks.asStateFlow()

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
}