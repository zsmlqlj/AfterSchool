package com.sj.afterschool

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_login_h_d.*
import kotlinx.android.synthetic.main.activity_main.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.IOException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.*
import javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier
import javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory

/*
* 登录界面，
* 登录黑大教务系统前置Activity
* */


class LoginHD : AppCompatActivity() {

    lateinit var resultNumber:String
    lateinit var resultPassword:String
    lateinit var resultCookie:HashMap<String,String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_h_d)

        Util.setTranslucent(this)
        initEditText()

        SSLContextSecurity.createIgnoreVerifySSL("TLS")   //TLS1.0与SSL3.0没有大区别，这个函数是为了让安卓x可以处理http协议
        heidaLoginbtn.setOnClickListener {
            val handler= @SuppressLint("HandlerLeak")
            object :Handler(){
                override fun handleMessage(msg: Message) {
                    when(msg.what){
                        0->{
                            Toast.makeText(this@LoginHD,"可能教务处出了点问题，请稍后重新尝试~",Toast.LENGTH_LONG).show()
                            //发送至服务器
                        }
                        1->{
                            Toast.makeText(this@LoginHD,"登录成功~",Toast.LENGTH_SHORT).show()
                            resultCookie= msg.obj as HashMap<String, String>
                            val intent=Intent(this@LoginHD,MainActivity::class.java)
                            val send=CookieParcelable("HDlogin",
                                resultCookie
                            )
                            intent.putExtra("callback1",send)
                            intent.putExtra("callback2",resultNumber)
                            intent.putExtra("callback3",resultPassword)
                            startActivity(intent)
                            finish()
                        }
                        2->{
                            Toast.makeText(this@LoginHD,"学号或密码错误，检查后再重试吧~",Toast.LENGTH_LONG).show()
                        }
                        3->{
                            Toast.makeText(this@LoginHD,"好像与服务器没沟通明白，请稍后再试~",Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

        resultNumber=heidaNumberEtext.text.toString()
        resultPassword=heidaPasswordEtext.text.toString()

            Thread{
                val loginMes=Message()
                try {
                    val hdUrl="https://webvpn.hlju.edu.cn:444/authserver/login?service=https://webvpn.hlju.edu.cn/vpn/user/auth/cas"
                    val finCookies=HashMap<String,String>()

                    val firstResponse=Util.myJsoup(hdUrl,Connection.Method.GET,10000,finCookies)

                    //获取lt以及execution作为cas单点登录的ticket
                    val doc=firstResponse.parse()
                    val lt= doc.getElementsByAttributeValue("name","lt").attr("value").toString()
                    val execution=doc.getElementsByAttributeValue("name","execution").attr("value").toString()
                    val data= "username=$resultNumber&password=$resultPassword&btn=&lt=$lt&dllt=userNamePasswordLogin&execution=$execution&_eventId=submit&rmShown=1"

                    //POST
                    val secResponse=Util.myJsoup(hdUrl,Connection.Method.POST,10000,
                        firstResponse.cookies() as HashMap<String, String>,data)
                    finCookies.putAll(firstResponse.cookies())
                    finCookies.putAll(secResponse.cookies())
                    finCookies["mapid"]="4ee7de73"

                    if (secResponse.cookies().toString()=="{}"){
                        loginMes.what=2
                        handler.sendMessage(loginMes)
                    }else if (secResponse.statusCode()>=400){
                        loginMes.what=3
                        loginMes.obj=secResponse.statusCode()
                        handler.sendMessage(loginMes)
                    }else{
                        loginMes.obj=finCookies
                        loginMes.what=1
                        handler.sendMessage(loginMes)
                    }
                }catch (e:IOException){
                    loginMes.what=0
                    handler.sendMessage(loginMes)
                }
            }.start()
        }
    }



    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initEditText(){
        //设置文本输入框和登录按钮的logo图片
        val accountLogo=getDrawable(R.drawable.account)
        accountLogo!!.setBounds(0,0,100,100)
        heidaNumberEtext.setCompoundDrawablesRelative(accountLogo,null,null,null)
        heidaNumberEtext.compoundDrawablePadding=50
        val passwordLogo=getDrawable(R.drawable.passwordlogo)
        passwordLogo!!.setBounds(0,0,100,100)
        heidaPasswordEtext.setCompoundDrawablesRelative(passwordLogo,null,null,null)
        heidaPasswordEtext.compoundDrawablePadding=50
        val loginLogo=getDrawable(R.drawable.login_h_d_button_logo)
        loginLogo!!.setBounds(38,0,145,80)
        heidaLoginbtn.setCompoundDrawables(loginLogo,null,null,null)


    }


    /**
     * 信任所有SSL证书
     */
    object SSLContextSecurity {
        fun createIgnoreVerifySSL(sslVersion: String): SSLSocketFactory {
            val sc = SSLContext.getInstance(sslVersion);
            val trustAllCerts: Array<TrustManager> = arrayOf(object : X509TrustManager {
                @SuppressLint("TrustAllX509TrustManager")
                @Throws(CertificateException::class)
                override fun checkClientTrusted(
                    chain: Array<java.security.cert.X509Certificate>, authType: String) {
                }

                @SuppressLint("TrustAllX509TrustManager")
                @Throws(CertificateException::class)
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate?> {
                    return arrayOfNulls(0)
                }
            })

            sc!!.init(null, trustAllCerts, java.security.SecureRandom())
            // 创建一个信任所有host的verifier
            val allHostsValid = HostnameVerifier { _, _ -> true }
             //如果 hostname in certificate didn't match的话就给一个默认的主机验证
            setDefaultSSLSocketFactory(sc.socketFactory);
            setDefaultHostnameVerifier(allHostsValid);
            return sc.socketFactory;
        }

    }



}
