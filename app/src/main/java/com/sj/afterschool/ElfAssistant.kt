package com.sj.afterschool

import android.animation.Animator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.RelativeLayout
import kotlin.math.sqrt


/** 本文件实现首页精灵
* 方法为重写viewgroup实现精灵3d效果+内部view透明button控制点击效果，如有更好的办法，欢迎issue
*
*
* 如果只重写自定义view，会导致这个view部分全局响应点击(重写点击范围可能可以解决？)，那如果直接将view
* 缩小到100dp左右呢？ 如果这么做，就会失去精灵的3d效果(除了100dp范围外不会响应点击，自然没有效果)
*
* */

open class ElfAssistant:RelativeLayout{

    private var mShakeAnim=ValueAnimator()
    val mPurplePaint=Paint(Paint.ANTI_ALIAS_FLAG)

    private var mCameraRotateX=0f
    private var mCameraRotateY=0f
    private val mMaxCameraRotate=15f
    val mCircleStrokeWidth=25f


    private val mMatrix=Matrix()
    private val mCamera=Camera()


    constructor(context: Context):super(context){
        setWillNotDraw(false)   //ViewGroup中onDraw方法默认是不调用的，而这里需要绘制elfAssistant
    }
    constructor(context: Context, attributeSet: AttributeSet):super(context, attributeSet){
        setWillNotDraw(false)
    }
    constructor(context: Context, attributeSet: AttributeSet, int: Int):super(
        context,
        attributeSet,
        int
    ){
        setWillNotDraw(false)
    }






    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        initPaint()
        if (canvas != null) {
            setCameraRotate(canvas)
        }
        drawElf(canvas)
        invalidate()

    }



    @SuppressLint("ResourceAsColor")
    fun initPaint(){
        with(mPurplePaint){
            isDither=true
            isAntiAlias=true
            style=Paint.Style.FILL_AND_STROKE
            strokeWidth=mCircleStrokeWidth
            strokeCap=Paint.Cap.ROUND
            color=R.color.purple
            alpha=230
        }
    }

    private fun drawElf(canvas: Canvas?){
        /**
         * 调整l,h,top的比例使助手主体部分按比例缩小，整个view覆盖在Imageview上
         * 达到让点击事件生效同时拥有动画效果
         * */
        val l =((width) / 2f)/4
        val h =(sqrt(3f) *l)
        val top = 5*height/8f

        canvas!!.drawLine((width-l)/2,top,width/2-l,top+h/2,mPurplePaint)
        canvas.drawLine(width/2-l,top+h/2,(width-l)/2,top+h,mPurplePaint)
        canvas.drawLine((width-l)/2,top+h,(width+l)/2,top+h,mPurplePaint)
        canvas.drawLine((width+l)/2,top+h,width/2+l,top+h/2,mPurplePaint)
        canvas.drawLine(width/2+l,top+h/2,(width+l)/2,top,mPurplePaint)
        canvas.drawLine((width+l)/2,top,(width-l)/2,top,mPurplePaint)

    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        when (ev!!.action) {
            MotionEvent.ACTION_DOWN -> {
                getCameraRotate(ev)

            }
            MotionEvent.ACTION_MOVE -> {
                getCameraRotate(ev)

            }
            MotionEvent.ACTION_UP->{
                startShakeAnim(ev)


            }
        }
        return super.dispatchTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return true
    }



    private fun setCameraRotate(mCanvas: Canvas) {

        mMatrix.reset()
        mCamera.save()
        mCamera.rotateX(mCameraRotateX)
        mCamera.rotateY(mCameraRotateY)
        mCamera.getMatrix(mMatrix)
        mCamera.restore()
        mMatrix.preTranslate(-width / 2.toFloat(), -5.5f*height / 8.toFloat())
        mMatrix.postTranslate(width / 2.toFloat(), 5.5f*height / 8.toFloat())
        mCanvas.concat(mMatrix)

    }


    private fun getCameraRotate(event: MotionEvent) {
        if (mShakeAnim.isRunning) {
            mShakeAnim.cancel()
        }
        val rotateX = -(event.y - height / 2)
        val rotateY = event.x - width / 2

        var percentX= rotateX / 200f
        var percentY= rotateY / 200f
        if (percentX > 1) {
            percentX = 1f
        } else if (percentX < -1) {
            percentX = -1f
        }
        if (percentY > 1) {
            percentY = 1f
        } else if (percentY < -1) {
            percentY = -1f
        }


        mCameraRotateX = percentX * mMaxCameraRotate
        mCameraRotateY = percentY * mMaxCameraRotate
    }


    private fun startShakeAnim(event: MotionEvent?) {

        val cameraRotateXName = "cameraRotateX"
        val cameraRotateYName = "cameraRotateY"
        val cameraRotateXHolder =
            PropertyValuesHolder.ofFloat(cameraRotateXName, mCameraRotateX, 0f)
        val cameraRotateYHolder =
            PropertyValuesHolder.ofFloat(cameraRotateYName, mCameraRotateY, 0f)
        mShakeAnim = ValueAnimator.ofPropertyValuesHolder(cameraRotateXHolder, cameraRotateYHolder)
        mShakeAnim.interpolator=OvershootInterpolator(6f)
        mShakeAnim.duration = 500
        mShakeAnim.addUpdateListener(AnimatorUpdateListener { animation ->
            mCameraRotateX = animation.getAnimatedValue(cameraRotateXName) as Float
            mCameraRotateY = animation.getAnimatedValue(cameraRotateYName) as Float
        })
        mShakeAnim.start()


    }




}


