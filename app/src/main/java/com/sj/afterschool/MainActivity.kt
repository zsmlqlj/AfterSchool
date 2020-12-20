package com.sj.afterschool


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.FragmentTransaction
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.HashMap


/*
* 程序主框架，采用底部导航栏＋多Fragment构成
* */


class MainActivity : AppCompatActivity(){



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        myInit()

    }


    private fun myInit(){
        Util.setStatusBar(this)
        bottomBarInit()
    }

    //设置状态栏颜色
    private fun statusInit(){
        val test=window.decorView
        val int= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        test.systemUiVisibility=int
    }

    //底部导航栏
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun bottomBarInit(){
        replaceFragment(R.id.homerBtn)
        bottomRadiogroup.setOnCheckedChangeListener { group, checkedId ->
            replaceFragment(checkedId)
        }

        val homeBottomIcon=getDrawable(R.drawable.home_bottom_icon)
        homeBottomIcon!!.setBounds(0,0,100,100)
        homerBtn.setCompoundDrawables(null,homeBottomIcon,null,null)
        val flashClassIcon=getDrawable(R.drawable.flash_class_icon)
        flashClassIcon!!.setBounds(0,0,100,100)
        fastclassrBtn.setCompoundDrawables(null,flashClassIcon,null,null)
        val settingIcon=getDrawable(R.drawable.setting_bottom_icon)
        settingIcon!!.setBounds(0,0,100,100)
        myrBtn.setCompoundDrawables(null,settingIcon,null,null)

    }


    private fun replaceFragment(checkedId:Int){
        val fragmentManger=supportFragmentManager
        val transaction=fragmentManger.beginTransaction()
        with(transaction){
            hideAllFragment(this)

            when(checkedId){
                R.id.homerBtn->{
                    if(homeFragment.isAdded)
                        show(homeFragment)
                    else {
                        add(R.id.threeFragment, homeFragment)
                        show(homeFragment)
                    }
                }
                R.id.fastclassrBtn->{
                    if (xxtFragment.isAdded)
                        show(xxtFragment)
                    else{
                        add(R.id.threeFragment, xxtFragment)
                        show(xxtFragment)
                    }
                }
                R.id.myrBtn->{
                    if(myFragment.isAdded)
                        show(myFragment)
                    else {
                        add(R.id.threeFragment, myFragment)
                        show(myFragment)
                    }
                }
            }
            commit()
        }
    }

    private fun hideAllFragment(fragmentTransaction: FragmentTransaction) {
        with(fragmentTransaction) {
            hide(homeFragment)
            hide(xxtFragment)
            hide(myFragment)
        }
    }

/*
* 保存登录学习通cookie
* */
    private lateinit var xxtLoginCookie:HashMap<String,String>

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            1->if(resultCode== Activity.RESULT_OK){
                val callBack=data?.getParcelableExtra<CookieParcelable>("callback")
                if (callBack != null) {
                    xxtLoginCookie=callBack.recCookie
                }

            }
        }
    }

    fun getxxtLoginCookie(): HashMap<String, String> {
        return xxtLoginCookie
    }


}