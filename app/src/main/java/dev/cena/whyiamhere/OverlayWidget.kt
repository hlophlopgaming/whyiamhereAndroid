package dev.cena.whyiamhere

import android.content.Context
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import dev.cena.whyiamhere.data.AppSettings
import dev.cena.whyiamhere.data.Session
import kotlin.math.abs

// Нативный performClick вызывается в обработчике касаний и доступен TalkBack.
@SuppressLint("ClickableViewAccessibility")
class OverlayWidget(
    private val context: Context,
    private val onFinish: (Session) -> Unit,
    private val onPosition: (Int, Int) -> Unit,
    private val onCollapsed: (Boolean) -> Unit,
) {
    private val manager = requireNotNull(context.getSystemService(WindowManager::class.java))
    private val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(8), dp(12), dp(8))
        elevation = dp(8).toFloat()
    }
    private val goal = TextView(context).apply {
        textSize = 15f
        setTextColor(Color.WHITE)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
    }
    private val timer = TextView(context).apply {
        textSize = 21f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER_VERTICAL
        minHeight = dp(48)
        isClickable = true
        isFocusable = true
        setOnClickListener { toggle() }
    }
    private val controls = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private val collapse = Button(context).apply {
        text = context.getString(R.string.collapse)
        isAllCaps = false
        minHeight = dp(48)
        setOnClickListener { toggle() }
    }
    private val finish = Button(context).apply {
        text = context.getString(R.string.finish_session)
        isAllCaps = false
        minHeight = dp(48)
        setOnClickListener { session?.let(onFinish) }
    }
    private val params = WindowManager.LayoutParams(
        dp(270), WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT,
    ).apply {
        // Координаты перетаскивания абсолютные, включая телефоны с RTL-интерфейсом.
        gravity = Gravity.TOP or Gravity.LEFT
    }
    private var attached = false
    private var collapsed = false
    private var session: Session? = null

    init {
        root.addView(goal)
        root.addView(timer)
        controls.addView(collapse, LinearLayout.LayoutParams(0, -2, 1f))
        controls.addView(finish, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(controls)
        val drag = dragListener()
        goal.setOnTouchListener(drag)
        timer.setOnTouchListener(drag)
    }

    fun show(value: Session, settings: AppSettings) {
        session = value
        collapse.setText(R.string.collapse)
        finish.setText(R.string.finish_session)
        goal.text = value.goal
        collapsed = settings.collapsed
        params.x = settings.widgetX
        params.y = settings.widgetY
        applySize()
        clampPosition()
        if (!attached) {
            manager.addView(root, params)
            attached = true
        } else manager.updateViewLayout(root, params)
        root.post {
            if (attached) {
                clampPosition()
                manager.updateViewLayout(root, params)
            }
        }
    }

    fun update(remaining: Long, warning: Boolean) {
        timer.text = formatRemaining(remaining)
        timer.contentDescription = context.getString(
            if (collapsed) R.string.widget_description_collapsed else R.string.widget_description_expanded,
            session?.goal.orEmpty(), timer.text,
        )
        root.background = GradientDrawable().apply {
            cornerRadius = dp(20).toFloat()
            setColor(Color.parseColor(if (warning) "#A54112" else "#244B3B"))
        }
    }

    fun hide() {
        if (attached) {
            try { manager.removeViewImmediate(root) } catch (_: IllegalArgumentException) { }
            attached = false
        }
    }

    private fun toggle() {
        collapsed = !collapsed
        applySize()
        if (attached) {
            manager.updateViewLayout(root, params)
            root.post {
                if (attached) {
                    clampPosition()
                    manager.updateViewLayout(root, params)
                    onPosition(params.x, params.y)
                }
            }
        }
        onCollapsed(collapsed)
    }

    private fun applySize() {
        goal.visibility = if (collapsed) View.GONE else View.VISIBLE
        controls.visibility = if (collapsed) View.GONE else View.VISIBLE
        params.width = dp(if (collapsed) 120 else 270)
    }

    private fun clampPosition() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        manager.defaultDisplay.getMetrics(metrics)
        params.width = params.width.coerceAtMost(metrics.widthPixels)
        params.x = params.x.coerceIn(0, (metrics.widthPixels - params.width).coerceAtLeast(0))
        params.y = params.y.coerceIn(0, (metrics.heightPixels - root.height.coerceAtLeast(dp(64))).coerceAtLeast(0))
    }

    private fun dragListener(): View.OnTouchListener {
        var downX = 0f
        var downY = 0f
        var originalX = 0
        var originalY = 0
        var dragged = false
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        return View.OnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    originalX = params.x; originalY = params.y
                    dragged = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > slop || abs(dy) > slop) dragged = true
                    if (dragged && attached) {
                        params.x = originalX + dx.toInt()
                        params.y = originalY + dy.toInt()
                        clampPosition()
                        manager.updateViewLayout(root, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (dragged) onPosition(params.x, params.y)
                    else if (event.actionMasked == MotionEvent.ACTION_UP) view.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
}

fun formatRemaining(millis: Long): String {
    val value = millis.coerceAtLeast(0)
    val seconds = value / 1000 + if (value % 1000 > 0) 1 else 0
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
