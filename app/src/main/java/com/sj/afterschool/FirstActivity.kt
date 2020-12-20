package com.sj.afterschool

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock.sleep
import android.view.WindowManager
import java.lang.Exception

class FirstActivity :AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_activity)
        Util.setTranslucent(this)

        Thread{
            try {
                sleep(1200)
                val intent=Intent(this,LoginHD::class.java)
                startActivity(intent)
                finish()
            }catch (e:Exception){
                e.printStackTrace()
            }
        }.start()
    }



}