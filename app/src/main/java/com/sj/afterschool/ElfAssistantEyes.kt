package com.sj.afterschool

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import kotlin.math.sqrt

class ElfAssistantEyes: ElfAssistant {

    private lateinit var mClickListener: elfClickListener

    constructor(context: Context):super(context)
    constructor(context: Context,attributeSet: AttributeSet):super(context,attributeSet)
    constructor(context:Context,attributeSet: AttributeSet,int: Int):super(context,attributeSet,int)




    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        eyesInit(canvas)


    }

    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
        mClickListener.elfClick()
    }

    interface elfClickListener{
        fun elfClick(){}
    }

    fun setElfClickListener(clickListener: elfClickListener){
        this.mClickListener=clickListener
    }

    private fun eyesInit(canvas: Canvas?){
        val l =((width) / 2f)/4
        val h =(sqrt(3f) *l)
        val top = 5*height/8f
        canvas!!.drawLine((width-l)/2+l/4,top+h/4,(width-l)/2+l/4,top+h/2,mPurplePaint)
        canvas.drawLine((width-l)/2+l/4+l/2,top+h/4,(width-l)/2+l/4+l/2,top+h/2,mPurplePaint)


    }




}