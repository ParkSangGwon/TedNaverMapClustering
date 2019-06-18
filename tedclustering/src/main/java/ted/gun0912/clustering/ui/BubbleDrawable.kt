/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ted.gun0912.clustering.ui

import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import ted.gun0912.clustering.R

/**
 * Draws a bubble with a shadow, filled with any color.
 */
internal class BubbleDrawable(res: Resources) : Drawable() {

    private val mShadow: Drawable
    private val mMask: Drawable
    private var mColor = Color.WHITE

    init {
        mMask = res.getDrawable(R.drawable.amu_bubble_mask)
        mShadow = res.getDrawable(R.drawable.amu_bubble_shadow)
    }

    fun setColor(color: Int) {
        mColor = color
    }

    override fun draw(canvas: Canvas) {
        mMask.draw(canvas)
        canvas.drawColor(mColor, PorterDuff.Mode.SRC_IN)
        mShadow.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        throw UnsupportedOperationException()
    }

    override fun setColorFilter(cf: ColorFilter?) {
        throw UnsupportedOperationException()
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        mMask.setBounds(left, top, right, bottom)
        mShadow.setBounds(left, top, right, bottom)
    }

    override fun setBounds(bounds: Rect) {
        mMask.bounds = bounds
        mShadow.bounds = bounds
    }

    override fun getPadding(padding: Rect): Boolean {
        return mMask.getPadding(padding)
    }
}
