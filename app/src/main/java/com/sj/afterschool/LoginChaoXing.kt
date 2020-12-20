package com.sj.afterschool

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.DropBoxManager
import android.os.Handler
import android.os.Message
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Toast
import com.bumptech.glide.Glide
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Object
import kotlinx.android.synthetic.main.activity_xxt_login.*
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.Serializable
import java.lang.StringBuilder
import java.net.CookieHandler
import java.net.PasswordAuthentication
import java.net.SocketException
import java.util.regex.Pattern
import kotlin.math.log

class LoginChaoXingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_xxt_login)

        val loginStatusOk=Message() //线程mes
        val loginStatusFailNet=Message()
        val loginStatusFail=Message()
        var result = HashMap<String, String>()    //保存登录返回的Cookie



//加载等待UI






//登录逻辑
        xxtloginbtn.setOnClickListener {
            val handler = @SuppressLint("HandlerLeak")
            object : Handler() {
                override fun handleMessage(msg: Message) {
                    when (msg.what) {
                        0 -> {
                            Toast.makeText(
                                this@LoginChaoXingActivity,
                                "可能学习通开小差了，请稍后重新尝试~",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                            //错误信息发送至服务器

                        }
                        1 -> {
                            result = msg.obj as HashMap<String, String>
                            val intent = Intent(this@LoginChaoXingActivity, MainActivity::class.java)
                            val res = CookieParcelable("xxt_normal", result)
                            intent.putExtra("callback", res)
                            setResult(Activity.RESULT_OK, intent)
                            Toast.makeText(this@LoginChaoXingActivity, "登录成功~", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        2 -> {
                            Toast.makeText(this@LoginChaoXingActivity, "与服务器没沟通明白，请稍后再试~", Toast.LENGTH_LONG).show()
                            finish()
                            //网络状态码发送至服务器


                        }
                    }
                }
            }
            Thread{
                val account=intent!!.getStringExtra("account")
                val password=intent!!.getStringExtra("password")
                val urlhlju="http://mooc.hlju.edu.cn/portal"
                val urlhdlogin="http://mooc.hlju.edu.cn/sso/hljussouserlogin"
                try{
/*
*   hd自主学习平台采用的是多次请求逐次获取全部cookie的登录方式，包括获取时间戳来确定Post的URL等方法，响应头中含有Location的会自动跳转
*   其余需要手动发送请求，目测需要登录学习通后再用学号登录的学校应该都类似
*   经过分析，发现不需要POST验证码也可以对后台进行操作，所以这里不对验证码操作
*
* */
                    val firstResponse=Util.myJsoup(urlhlju,Connection.Method.GET,5000,result)
                    result.putAll(firstResponse.cookies())

                    val jsonData="uname=$account&password=$password&type=1&fid=18738&backurl=http%3A%2F%2Fmooc.hlju.edu.cn%2Fsso%2Fhljusso"
                    val secResponse=Util.myJsoup(urlhdlogin,Connection.Method.POST,5000,result,jsonData)
                    val secPause= secResponse.parse()
                    val enc=secPause.getElementsByAttributeValue("name","enc").attr("value") //获取enc和time
                    val time=secPause.getElementsByAttributeValue("name","time").attr("value")
                    result.putAll(secResponse.cookies())

                    val jsonData3="fid=18738&uname=$account&enc=$enc&refer=http%3A%2F%2Fmooc.hlju.edu.cn%2Flogin%2Fauth&authurl=http%3A%2F%2Fmooc.hlju.edu.cn%2Ftologin%3Fstatus%3D2&authrul=http%3A%2F%2Fmooc.hlju.edu.cn%2Ftologin%3Fstatus%3D2&time=$time&expires="
                    val urlfinPost="http://passport2.hlju.edu.cn/loginfanya?_t=$time"  //时间戳
                    val finResponse=Util.myJsoup(urlfinPost,Connection.Method.POST,10000,result,jsonData3)
                    result.putAll(finResponse.cookies())

                    if (result.toString()==="{}"){
                        loginStatusFail.what=2
                        handler.sendMessage(loginStatusFail)
                    }else{
                        loginStatusOk.what=1
                        loginStatusOk.obj=result
                        handler.sendMessage(loginStatusOk)
                    }
                }catch (e:IOException){
                    loginStatusFailNet.what=0
                    handler.sendMessage(loginStatusFailNet)
                }
            }.start()
        }
    }
}


