package com.sj.afterschool

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Message
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.xxt_classlist_fragment.*
import kotlinx.android.synthetic.main.xxt_fragment.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.io.IOException
import java.lang.Exception


/*
* 学习通fragment
* */

class XxtFragment:Fragment(){


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.xxt_fragment,container,false)
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //头像兼登录按钮
        head_photo.setOnClickListener {
            val intent=Intent(activity,LoginChaoXingActivity::class.java)
            intent.putExtra("account",activity!!.intent.getStringExtra("callback2"))
            intent.putExtra("password",activity!!.intent.getStringExtra("callback3"))
            activity?.startActivityForResult(intent,1)
        }


        val handler = @SuppressLint("HandlerLeak")
        object : android.os.Handler() {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    1 -> {
                        //登录成功，获取头像、重新加载登录后的fragment
                        nsv.isNestedScrollingEnabled=true
                        head_photo.visibility=View.INVISIBLE
                        Glide.with(this@XxtFragment).load(msg.obj)
                            .placeholder(R.drawable.headphotoloading)
                            .apply(RequestOptions.bitmapTransform(CircleCrop()))
                            .into(head_photo_image)
                        xxt_vp.adapter=object :FragmentStateAdapter(this@XxtFragment){
                            override fun getItemCount(): Int=2
                            override fun createFragment(position: Int): Fragment {
                                return when(position){
                                    0->XXTClasslistFragment()
                                    else-> AnnouncementFragment()
                                }
                            }
                        }
                    }
                    2->{
                        //加载失败占位图
                        
                    }
                }
            }
        }
        val mainActivity = activity as MainActivity
        Thread{
            //避免未初始化错误，确保已登录，且MainActivity获得cookie后再使用
            //开启新线程防止主线程被阻塞
            while (true){
                try {
                    val cookieA = mainActivity.getxxtLoginCookie()
                }catch (e:Exception){
                    continue
                }
                val cookieA = mainActivity.getxxtLoginCookie()
                val uid = cookieA["_uid"]
                val mes = Message()
                try {
                    val getUrl=Util.myJsoup("http://photo.hlju.edu.cn/p/" + uid + "_80",Connection.Method.GET,3000,cookieA)
                    mes.what = 1
                    mes.obj = getUrl.url()
                    handler.sendMessage(mes)
                }catch (e:IOException){
                    mes.what=2
                    handler.sendMessage(mes)
                }
                break
            }
        }.start()


        //初始化搜索框
        courseSearch_xxt.isIconifiedByDefault = false
        courseSearch_xxt.clearFocus()
        val searchtext=courseSearch_xxt.context.resources.getIdentifier("android:id/search_src_text",null,null)
        val textview=courseSearch_xxt.findViewById<TextView>(searchtext)
        textview.setTextSize(TypedValue.COMPLEX_UNIT_SP,12f)
        courseSearch_xxt.setOnQueryTextFocusChangeListener { v, hasFocus ->
        }


        //初始化tablayout：未登录状态
        nsv.isNestedScrollingEnabled=false
        xxt_vp.isUserInputEnabled=false
        xxt_vp.adapter= object : FragmentStateAdapter(this@XxtFragment) {
            override fun getItemCount()=2
            override fun createFragment(position: Int): Fragment {
                return when(position){
                    0->NoLoginFragment()
                    else->NoLoginFragment()
                }
            }
        }
        TabLayoutMediator(tab_xxt,xxt_vp
        ) { tab, position ->
            val myview = View.inflate(activity, R.layout.tab_item, null)
            tab.customView = myview
            val tabView = tab.customView!!.findViewById<TextView>(R.id.tab_text)
            when (position) {
                0 -> tabView.text = "我的课程"
                else -> tabView.text = "板块公告"
            }
        }.attach()
        xxt_vp.registerOnPageChangeCallback( object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                //可以来设置选中时tab的大小
                val tabCount: Int =  tab_xxt.tabCount
                for (i in 0 until tabCount) {
                    val tab = tab_xxt.getTabAt(i)
                    val tabView = tab!!.customView!!.findViewById<TextView>(R.id.tab_text)
                    if (tab.position == position) {
                        tabView.textSize = 18F
                        tabView.typeface = Typeface.DEFAULT_BOLD
                    } else {
                        tabView.textSize = 18F
                        tabView.typeface = Typeface.DEFAULT
                    }
                }
            }
        })

    }
}



/*
* 学习通fragment中的刷课设定tab
* */
class AnnouncementFragment:Fragment(){
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.announcement,container,false)
    }
}



/*
* 学习通fragment中的课程列表 viewpager
* */
class XXTClasslistFragment:Fragment(){
    private lateinit var recyclerView: RecyclerView
    private lateinit var courseAdapter:CourseAdapter
    private var courselist=ArrayList<CourseItems>()
    private lateinit var ml0List: Elements


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.xxt_classlist_fragment,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getAllCourses()

    }

    private fun getAllCourses(){
        val mainActivity = activity as MainActivity
        val cookieA = mainActivity.getxxtLoginCookie()
        val handler1=
            @SuppressLint("HandlerLeak")
            object : android.os.Handler() {
                override fun handleMessage(msg: Message) {
                    when(msg.what){
                        0-> Toast.makeText(this@XXTClasslistFragment.activity,"加载失败，下拉重新加载~", Toast.LENGTH_LONG).show()
                        1->{
                            ml0List=msg.obj as Elements
                            initCourse()
                        }
                    }
                }
            }
        Thread{
            val mes1= Message()
            try {
                val getCourse=Util.myJsoup("http://mooc.hlju.edu.cn/courselist/study?begin=0&end=0&showContent=全部课程",Connection.Method.GET,10000,cookieA)
                val doc=getCourse.parse()
                val ml0Lista=doc.getElementsByClass("zmy_item")
                mes1.what=1
                mes1.obj=ml0Lista
                handler1.sendMessage(mes1)
            }catch (e: IOException){
                mes1.what=0
            }
        }.start()
    }

    fun initCourse(){
        val mainActivity = activity as MainActivity
        val cookieA = mainActivity.getxxtLoginCookie()
        recyclerView= courselistRC
        //获取课程
        getData(ml0List)
        courseAdapter= context?.let { CourseAdapter(it,courselist) }!!
        courseAdapter.setHasStableIds(true)
        courseAdapter.setOnItemClickListener(object :CourseAdapter.OnItemClickListener{
            override fun onItemClick(view: View, position: Int) {
                val intent= Intent(activity,ShowCourse::class.java)
                val cookie=CookieParcelable("courselist", cookieA )
                intent.putExtra("name",courselist[position].courseName)
                intent.putExtra("img",courselist[position].courseImage)
                intent.putExtra("href",courselist[position].courseHerf)
                intent.putExtra("cookie",cookie)
                activity?.startActivity(intent)
            }
        })
        recyclerView.layoutManager= GridLayoutManager(activity,3)
        recyclerView.adapter=courseAdapter
        recyclerView.addOnScrollListener(object :EndlessRecyclerOnScrollListener() {
            override fun onLoadMore() {
                if (courseAdapter.getState()!=courseAdapter.loadingEnd){
                    val oldCourselist=courselist.clone() as ArrayList<CourseItems>
                    courseAdapter.setLoadState(courseAdapter.loading,oldCourselist,courselist,courseAdapter)
                    SystemClock.sleep(300)
                    //加载过快会没有UI，视觉效果较差，这里阻塞主线程300毫秒
                    if (getData(ml0List)) {
                        courseAdapter.setLoadState(courseAdapter.loadingComplete,oldCourselist,courselist,courseAdapter)
                    } else {
                        //加载完全
                        courseAdapter.setLoadState(courseAdapter.loadingEnd,oldCourselist,courselist,courseAdapter)
                    }
                }else{
                    return
                }

            }
        })
    }
    fun getData(listMl0: Elements): Boolean {
        var control=0      //每次加载12项课程，分页加载防止oom
        for (listElement in listMl0){
            val img=listElement.getElementsByTag("img").attr("src")
            val name=listElement.getElementsByAttributeValue("name","courseNameHtml").text()
            if (name==""){
                control++
                continue
            }
            val href=listElement.getElementsByClass("zmy_pic").attr("href")
            val course=CourseItems(name,img,href)
            courselist.add(course)
            listMl0.elementAt(control).empty()
            control++
            if (control==12){
                return true
            }
        }
//数据加载完成
        return false
    }




}