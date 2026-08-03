package com.example.englishtapreader

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class WordHighlightView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var words: List<WordInfo> = emptyList()
    private var scale = 1f; private var offX = 0f; private var offY = 0f
    private var selectedWord: WordInfo? = null
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAFFFF00"); style = Paint.Style.FILL
    }
    var onWordClickListener: ((WordInfo) -> Unit)? = null

    fun setWords(words: List<WordInfo>, scale: Float, offsetX: Float, offsetY: Float) {
        this.words = words; this.scale = scale; this.offX = offsetX; this.offY = offsetY
        selectedWord = null; invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (word in words) {
            val box = word.boundingBox ?: continue
            val rect = RectF(box.left*scale+offX, box.top*scale+offY, box.right*scale+offX, box.bottom*scale+offY)
            if (word == selectedWord) canvas.drawRect(rect, highlightPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return false
        val (x,y) = event.x to event.y
        for (word in words) {
            val box = word.boundingBox ?: continue
            if (x in (box.left*scale+offX)..(box.right*scale+offX) && y in (box.top*scale+offY)..(box.bottom*scale+offY)) {
                selectedWord = word; invalidate(); onWordClickListener?.invoke(word); return true
            }
        }
        selectedWord = null; invalidate(); return false
    }
}
