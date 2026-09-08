package com.hufeng943.timetable.legacyprobe

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class LegacyProbeActivity : Activity() {
    private lateinit var output: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        root.addView(TextView(this).apply {
            text = "Build 9 · China Legacy Wear Probe"
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        root.addView(TextView(this).apply {
            text = "独立探针，不修改原 Timetable 数据。\n10.2.0 + GoogleApiClient + Wearable.*Api"
            textSize = 15f
            setPadding(0, dp(8), 0, dp(12))
        })

        fun addButton(label: String, action: () -> Unit) {
            root.addView(Button(this).apply {
                text = label
                isAllCaps = false
                setOnClickListener { action() }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(8)
            })
        }

        addButton("运行 Legacy 完整诊断") {
            runAsync { LegacyProbe.runDiagnostics(this, ROLE) }
        }
        addButton("测试 $ROLE_LABEL → 对端 Message") {
            runAsync { LegacyProbe.sendMessage(this, ROLE) }
        }
        addButton("测试 $ROLE_LABEL → 对端 DataItem") {
            runAsync { LegacyProbe.sendDataItem(this, ROLE) }
        }
        addButton("刷新最近接收事件") {
            output.text = "最近接收：\n${LegacyProbe.lastEvent(this)}"
        }

        output = TextView(this).apply {
            text = "点击“运行 Legacy 完整诊断”开始。"
            textSize = if (ROLE == "WATCH") 15f else 16f
            setTextIsSelectable(true)
            movementMethod = ScrollingMovementMethod()
            gravity = Gravity.START
            setPadding(0, dp(12), 0, dp(24))
        }
        root.addView(output, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun runAsync(block: () -> String) {
        output.text = "运行中…"
        Thread {
            val result = runCatching(block).getOrElse { "FAIL ${it.javaClass.simpleName}: ${it.message ?: "无详细信息"}" }
            runOnUiThread { output.text = result }
        }.start()
    }

    companion object {
        const val ROLE = "PHONE"
        const val ROLE_LABEL = "手机"
    }
}
