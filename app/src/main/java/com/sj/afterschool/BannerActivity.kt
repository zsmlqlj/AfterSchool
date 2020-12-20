package com.sj.afterschool

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.activity_banner.*

class BannerActivity : AppCompatActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_banner)
        Util.setTranslucent(this)

        banner_webview.settings.javaScriptEnabled=true
        banner_webview.settings.domStorageEnabled=true
        banner_webview.webViewClient= WebViewClient()
        banner_webview.loadUrl("https://g.gd-share.cn/p/lihhh9jg")
    }
}