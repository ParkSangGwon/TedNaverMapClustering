package ted.gun0912.clustering.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

internal class SquareTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {
    private var mOffsetTop = 0
    private var mOffsetLeft = 0


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = measuredWidth
        val height = measuredHeight
        val dimension = Math.max(width, height)
        if (width > height) {
            mOffsetTop = width - height
            mOffsetLeft = 0
        } else {
            mOffsetTop = 0
            mOffsetLeft = height - width
        }
        setMeasuredDimension(dimension, dimension)
    }

    override fun draw(canvas: Canvas) {
        canvas.translate((mOffsetLeft / 2).toFloat(), (mOffsetTop / 2).toFloat())
        super.draw(canvas)
    }
}
