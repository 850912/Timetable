package com.hufeng943.timetable.probe

import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WearProbeActivity : AppCompatActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var output: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "中国区 Wear 通信探针"

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(32))
        }
        column.addView(TextView(this).apply {
            text = "Build 8 · China Wear Probe"
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
        })
        column.addView(TextView(this).apply {
            text = "只做诊断，不修改课表数据。请先确保手机和手表已正常配对，并且两端安装同签名版本。"
            textSize = 15f
            setPadding(0, dp(8), 0, dp(16))
        })
        column.addView(button("运行完整诊断") { runDiagnostic() })
        column.addView(button("测试手机 → 手表 Message") { runAction { WearProbe.sendMessage(this) } })
        column.addView(button("测试手机 → 手表 DataItem") { runAction { WearProbe.sendDataItem(this) } })
        column.addView(button("刷新最近接收事件") { render("最近接收：\n${WearProbe.lastEvent(this)}") })
        output = TextView(this).apply {
            text = "点击“运行完整诊断”开始。"
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setPadding(0, dp(18), 0, 0)
        }
        column.addView(output)
        setContentView(ScrollView(this).apply { addView(column) })
    }

    private fun runDiagnostic() = runAction { WearProbe.runDiagnostics(this) }

    private fun runAction(block: () -> String) {
        output.text = "检测中…"
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching(block).getOrElse { "FAIL ${it.javaClass.simpleName}: ${it.message ?: "无详细信息"}" }
            }
            render(text)
        }
    }

    private fun render(text: String) { output.text = text }

    private fun button(label: String, action: () -> Unit) = MaterialButton(this).apply {
        text = label
        isAllCaps = false
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(8)
        }
        setOnClickListener { action() }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
