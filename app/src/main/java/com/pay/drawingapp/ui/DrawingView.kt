package com.pay.drawingapp.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private lateinit var mpath: CustomPath
    private lateinit var mCanvasBitmap: Bitmap
    private lateinit var mDarwpaint: Paint
    private lateinit var mCanvasPaint: Paint
    private var mBrushSize: Float = 0.toFloat()
    private var color = Color.BLACK
    private var colorVal: Int = 0
    private lateinit var canvas: Canvas
    private val mPaths: ArrayList<CustomPath> = ArrayList()
    private val mUndoPaths: ArrayList<CustomPath> = ArrayList()

    init {
        setupDrawingUI()
    }

    private fun setupDrawingUI() {
        mDarwpaint = Paint()
        mpath = CustomPath(color, mBrushSize)
        mDarwpaint.color = color
        mDarwpaint.style = Paint.Style.STROKE
        mDarwpaint.strokeJoin = Paint.Join.ROUND
        mDarwpaint.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
        //we removed this because we set the brush size in the main activity through setBrushSize
        //mBriSize = 20.toFloat()
    }

    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path() {

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        //here we set the path... how thick it is...
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                mpath.color = color
                mpath.brushThickness = mBrushSize
                mpath.reset()
                touchY?.let {
                    touchX?.let { it1 ->
                        mpath.moveTo(it1, it)
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                touchY?.let {
                    touchX?.let { it1 ->
                        mpath.lineTo(it1, it)
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                mPaths.add(mpath)
                mpath = CustomPath(color, mBrushSize)
            }

            else -> {
                return false
            }
        }
        invalidate()
        return true;
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawBitmap(mCanvasBitmap, 0f, 0f, mCanvasPaint)

        for (path in mPaths) {
            mDarwpaint.strokeWidth = path.brushThickness
            mDarwpaint.color = path.color
            canvas?.drawPath(path, mDarwpaint)
        }

        //here we say how thick the paint should be...
        if (!mpath.isEmpty) {
            mDarwpaint.strokeWidth = mpath.brushThickness
            mDarwpaint.color = mpath.color
            canvas?.drawPath(mpath, mDarwpaint)
        }
    }

    fun setBrushSize(newSize: Float) {
        mBrushSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, newSize,
            resources.displayMetrics
        )
        mDarwpaint.strokeWidth = mBrushSize
    }

    fun setBrushColor(brushColor: String) {
        color = Color.parseColor(brushColor)
        mDarwpaint.color = color
    }

    fun setBrushColor() {
        mDarwpaint.color = Color.RED
    }

    fun undoDrawing() {
        if (mPaths.size > 0) {
            mUndoPaths.add(mPaths.removeAt(mPaths.size - 1))
            invalidate()
        }
    }
}