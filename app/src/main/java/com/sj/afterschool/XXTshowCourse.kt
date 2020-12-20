package com.sj.afterschool

import android.annotation.SuppressLint
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_show_course.*
import kotlinx.android.synthetic.main.coursenodes.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.math.BigInteger
import java.net.SocketTimeoutException
import java.security.MessageDigest
import java.util.regex.Pattern

/*
* 学习通点开进入课程内的activity
* */

class ShowCourse : AppCompatActivity() {
    private lateinit var nodesAdapter:NodesAdapter
    private lateinit var nodesRec:RecyclerView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_course)

        contentInit()
        tabInit()

    }

    private fun contentInit(){
        val name=intent.getStringExtra("name")
        val img=intent.getStringExtra("img")
        val href=intent.getStringExtra("href")
        val cookieA=intent.getParcelableExtra<CookieParcelable>("cookie")!!.recCookie

        show_course_refresher.isRefreshing=true
        if (show_course_refresher.isRefreshing){
            connectToChaoXing(name,img,href,cookieA)
        }
        show_course_refresher.setOnRefreshListener {
            connectToChaoXing(name,img,href,cookieA)
        }

    }

    private fun connectToChaoXing(name:String,img:String,href:String,cookieA: HashMap<String, String>){
        val nodesList=ArrayList<CourseNodeItems>()
        val handler= @SuppressLint("HandlerLeak")
        object :Handler(){
            override fun handleMessage(msg: Message) {
                when(msg.what){
                    1->{
                        nodesAdapter=
                            NodesAdapter(this@ShowCourse,nodesList)
                        nodesAdapter.setHasStableIds(true)
                        nodesAdapter.setOnItemClickListener(object :NodesAdapter.OnItemClickListener{
                            override fun onItemClick(view: View, position: Int) {
                                Thread{
                                    val jober=ChaoxingDoJob.getInstance(cookieA,nodesList,position,30)
                                    if (jober.checkProgressFinish()){
                                        jober.startJob()
                                        //todo handlemessage处理
                                    }
                                }.start()
                            }
                        })
                        nodesRec=nodesRC
                        nodesRec.setHasFixedSize(true)
                        nodesRec.layoutManager=LinearLayoutManager(this@ShowCourse)
                        nodesRec.adapter=nodesAdapter
                        show_course_refresher.isRefreshing=false
                    }
                    2->{
                        Toast.makeText(this@ShowCourse,"加载失败，请下拉刷新~",Toast.LENGTH_SHORT).show()
                        show_course_refresher.isRefreshing=false
                    }
                }
            }
        }

        /*
        * 获取课程节点
        * */
        Thread{
            val mes1=Message()
            cookieA["isfyportal"]="1"
            cookieA["amlbcookie"]="01"
            cookieA.remove("AMAuthCookie")
            try
            {
                val getNodes=Util.myJsoup(href,Connection.Method.GET,3000,cookieA)
                cookieA.putAll(getNodes.cookies())

                val list=getNodes.parse().getElementsByClass("units")
                for (i in list){
                    val text=i.select("h2").text()
                    val title=CourseNodeItems(text,0,0,"")
                    nodesList.add(title)
                    if (i.getElementsByClass("leveltwo")!=null){
                        for (u in i.getElementsByClass("leveltwo") ) {
                            val textLevel2=u.select("h3").first().text()
                            val hrefLevel2="http://mooc1.hlju.edu.cn"+u.select("a").attr("href").toString()
                            when(u.getElementsByClass("icon").select("em").attr("class").toString()){
                                ""->{
                                    val item=CourseNodeItems(textLevel2,1,0,hrefLevel2)
                                    nodesList.add(item)
                                }
                                "orange","blank"->{
                                    val item=CourseNodeItems(textLevel2,1,2,hrefLevel2)
                                    nodesList.add(item)
                                }
                                "openlock"->{
                                    val item=CourseNodeItems(textLevel2,1,1,hrefLevel2)
                                    nodesList.add(item)
                                }
                            }
                            if (u.getElementsByClass("levelthree")!=null){
                                for (v in u.getElementsByClass("levelthree").select("h3")){
                                    val textLevel3=v.select("h3").text()
                                    val hrefLevel3="http://mooc1.hlju.edu.cn"+v.select("a").attr("href").toString()
                                    when(u.getElementsByClass("icon").select("em").attr("class").toString()){
                                        ""->{
                                            val item=CourseNodeItems(textLevel3,1,0,hrefLevel3)
                                            nodesList.add(item)
                                        }
                                        "orange","blank"->{
                                            val item=CourseNodeItems(textLevel3,1,2,hrefLevel3)
                                            nodesList.add(item)
                                        }
                                        "openlock"->{
                                            val item=CourseNodeItems(textLevel3,1,1,hrefLevel3)
                                            nodesList.add(item)
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
                mes1.what=1
                handler.sendMessage(mes1)
            }catch (e:Exception){
                mes1.what=2
                handler.sendMessage(mes1)
            }
        }.start()
        courseopen_name.text=name
        Glide.with(this).load(img)
            .apply(RequestOptions.bitmapTransform(RoundedCorners(15)))
            .into(imagecourseopen)
    }



    private fun tabInit(){
        //初始化tablayout以及viewpager
        courseopen_vp.isUserInputEnabled=false
        courseopen_vp.adapter=object :FragmentStateAdapter(this){
            override fun getItemCount(): Int=2
            override fun createFragment(position: Int): Fragment {
                return when(position){
                    0->CourseNodes()
                    else->HomeWorkFragment()
                }
            }
        }
        TabLayoutMediator(tab_classitemopen,courseopen_vp
        ) { tab, position ->
            val myView = View.inflate(this, R.layout.tab_item, null)
            tab.customView = myView
            val textView = tab.customView!!.findViewById<TextView>(R.id.tab_text)
            when (position) {
                0 -> textView.text = "课程详情"
                else -> textView.text = "作业信息"
            }
        }.attach()
        courseopen_vp.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                val tabcount=tab_classitemopen.tabCount
                for (i in 0 until tabcount){
                    val tab=tab_classitemopen.getTabAt(i)
                    val textView=tab!!.customView!!.findViewById<TextView>(R.id.tab_text)
                    if (tab.position==position) {
                        textView.textSize = 18F
                        textView.typeface = Typeface.DEFAULT_BOLD
                    }else{
                        textView.textSize=18F
                        textView.typeface = Typeface.DEFAULT
                    }
                }
            }
        })
    }



}


/*
*展示课程内节点 tab
* */
class CourseNodes:Fragment(){
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.coursenodes,container,false)
    }


}

/*
* 课程公告 tab
* */
class HomeWorkFragment: Fragment(){

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.course_setting,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}
