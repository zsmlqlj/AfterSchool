package com.sj.afterschool

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.youth.banner.adapter.BannerAdapter
import com.youth.banner.adapter.BannerImageAdapter
import com.youth.banner.holder.BannerImageHolder
import com.youth.banner.indicator.CircleIndicator
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_banner.*
import kotlinx.android.synthetic.main.class_item.*
import kotlinx.android.synthetic.main.home_fragment.*
import kotlinx.android.synthetic.main.xxt_fragment.*
import org.jsoup.Connection
import java.net.SocketTimeoutException
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


/*
* 首页fragment
* */

class HomeFragment: Fragment(){

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.home_fragment, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (activity!=null){
            val hdLoginCookie= activity!!.intent.getParcelableExtra<CookieParcelable>("callback1")!!.recCookie
            contentInit(this.activity!!, hdLoginCookie)
        }
    }

    private fun contentInit(context: Context, map: HashMap<String, String>){
        getCourse(context, map)
        bannerInit()
        pageInit()

    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    private fun pageInit(){
        if (activity!=null){
            this.context?.let {
                Glide.with(it).load(R.drawable.home_bcg)
                    .apply(RequestOptions.bitmapTransform(BlurTransformation(5, 5)))
                    .into(home_top_imageview)
            }
            /*
            * 设置button 图标
            * */
            val hdGradeButtonLogo=activity!!.getDrawable(R.drawable.grade_logo)
            hdGradeButtonLogo!!.setBounds(0, 0, 140, 140)
            hd_grade_button.setCompoundDrawables(null, hdGradeButtonLogo, null, null)
            val hdTestInfoLogo=activity!!.getDrawable(R.drawable.test_info_logo)
            hdTestInfoLogo!!.setBounds(0, 0, 140, 140)
            hd_testInfo_button.setCompoundDrawables(null, hdTestInfoLogo, null, null)
            val hdClassListButtonLogo=activity!!.getDrawable(R.drawable.class_list_logo)
            hdClassListButtonLogo!!.setBounds(0, 0, 140, 140)
            hd_classList_button.setCompoundDrawables(null, hdClassListButtonLogo, null, null)
            val hdPersonInformationButtonLogo=activity!!.getDrawable(R.drawable.person_information_logo)
            hdPersonInformationButtonLogo!!.setBounds(0, 0, 140, 140)
            hd_person_information_button.setCompoundDrawables(null, hdPersonInformationButtonLogo, null, null)


            /*
            *
            * */

        }
    }


    //todo 暂时写死，后续接入服务器
    private fun bannerInit(){
        val imageList=ArrayList<Int>()
        imageList.add(R.drawable.banner1)
        val adapter=HomeBannerAdapter(imageList)
        adapter.setOnImageClickListener(object :HomeBannerAdapter.OnImageClickListener{
            override fun onImageClick(view: View, position: Int) {
                //position zero~
                val intent=Intent(activity,BannerActivity::class.java)
                startActivity(intent)
            }
        })
        home_banner?.let {
            it.addBannerLifecycleObserver(this)
            it.indicator = CircleIndicator(activity)
            it.setBannerRound(20f)
            it.adapter = adapter
        }
    }


    @SuppressLint("ResourceAsColor")
    private fun getCourse(context: Context, map: HashMap<String, String>){
        val myCourseDao=MyCourseDatabase.getDatabase(context).mycourseDao()
        //首次进入刷新
        curriculum_refresh.isRefreshing=true
        if (curriculum_refresh.isRefreshing){
            connectToJWC(map, myCourseDao)
        }
        curriculum_refresh.setColorSchemeColors(R.color.colorPrimary)
        curriculum_refresh.setOnRefreshListener {
            connectToJWC(map, myCourseDao)
        }
    }




    private fun connectToJWC(map: HashMap<String, String>, myCourseDao: MyCourseDao){
        val messageOk= Message()
        val handler= @SuppressLint("HandlerLeak")
        object : Handler(){
            @SuppressLint("SetTextI18n")
            override fun handleMessage(msg: Message) {
                when(msg.what){
                    1 -> {
                        val userName=msg.obj as String
                        welcome_text_view.text= "欢迎回来，$userName"
                        //开始加载日历
                        val calendar = Calendar.getInstance()
                        calendar.time = Date()
                        val calendarAdapterList = ArrayList<CalendarAdapterList>()
                        for (after in 0..7) {
                            //查看一周课程
                            val calendarAdapterListItem =
                                CalendarAdapterList(
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH) + 1,
                                    calendar.get(Calendar.DAY_OF_MONTH),
                                    calendar.get(Calendar.DAY_OF_WEEK)
                                )
                            calendarAdapterList.add(calendarAdapterListItem)
                            calendar.add(Calendar.DATE, 1)
                        }
                        calendar_vp.orientation = ViewPager2.ORIENTATION_VERTICAL
                        calendar_vp.adapter =
                            CalendarAdapter(
                                this@HomeFragment.activity!!,
                                calendarAdapterList,
                                myCourseDao,
                                map
                            )
                        curriculum_refresh.isRefreshing = false
                    }
                    2 -> {
                        Toast.makeText(activity,"加载失败啦，下拉重新加载~",Toast.LENGTH_LONG).show()
                        welcome_text_view.text="加载名字失败"
                        curriculum_refresh.isRefreshing = false
                    }
                }
            }
        }

        Thread{
    /*verifyUrl这个请求得到的cookie是作为验证并取得个人数据的关键cookie，没有这个cookie将会重定向到未登录的【师生服务】页面
    *顺便一提，没有LoginHD的cookie将会直接重定向到黑大的正常登录页面，
    *这是因为没有正确的 cas签发的Ticket 导致的，具体可以查询cas单点登录相关内容了解
    * */
            try {
                val verifyUrl="https://webvpn.hlju.edu.cn/53d558b9/ssfw/j_spring_ids_security_check "
                val curriculumUrl="https://webvpn.hlju.edu.cn/53d558b9/ssfw/jwnavmenu.do?menuItemWid=D0B8115CED8537E3E04012ACDD216CBA " //课表url,目测是跳转到最新学期的课程表




                map["mapid"]="53d558b9"
                val verify=Util.myJsoup(verifyUrl, Connection.Method.GET, 5000, map)
                map.putAll(verify.cookies())

                val getTime=Util.myJsoup(curriculumUrl, Connection.Method.GET, 5000, map)
                val doc= getTime.parse()
                val term=doc.select("caption").first().text()
                val pattern=Pattern.compile("期 .* 同")
                val matcher=pattern.matcher(Matcher.quoteReplacement(term))
                var name=if (matcher.find()){matcher.group()}else{""}
                name=name.replace(" ","")
                name=name.replace("期","")
                name=name.replace("同","")



                myCourseDao.deleteAllMyCourse()
                queryCourse(myCourseDao, doc, term)
                messageOk.what=1
                messageOk.obj=name
                handler.sendMessage(messageOk)
            }catch (e: Exception){
                Log.w("eee",e.toString())
                messageOk.what=2
                handler.sendMessage(messageOk)
            }
        }.start()
    }

    private fun queryCourse(dao: MyCourseDao, doc: org.jsoup.nodes.Document, term: String){
        val dp=HashMap<Int, Int>()  //标记长于两节课的课程
        var trControl=0   //控制tr标签位置
        tr@for (tr in doc.select("tr")){
            if (trControl==0){
                trControl++
                continue@tr
            } else if (trControl%2!=0) {
                val tdAll = tr.select("td")
                var tdControl = 0  //控制td标签即数据库输入
                td@ for (td in tdAll) {
                    if (tdControl < 2) {
                        tdControl++
                        continue@td
                    }
                    if (tdAll.size == 9) {//正好都是两节课的td格式

                        //提取课程
                        var pattern= Pattern.compile(".*?节\\)")
                        var matcher=pattern.matcher(Matcher.quoteReplacement(td.text()))
                        var courseName = if (matcher.find()){ matcher.group() }else{ "" }

                        //提取课程地点
                        val place=td.text().replace(courseName,"")
                        pattern= Pattern.compile("(.){0,4}-([a-z0-9]){1,8}")
                        matcher=pattern.matcher(Matcher.quoteReplacement(place))
                        val coursePlace=if (matcher.find()){ matcher.group() }else{
                            pattern=Pattern.compile("([\u4E00-\u9FA5])*-.*([\u4E00-\u9FA5])*[0-9a-z]*")
                            matcher=pattern.matcher(Matcher.quoteReplacement(place))
                            if (matcher.find()){
                                matcher.group()
                            }else{""}
                        }

                        //提取课程时间
                        pattern= Pattern.compile("\\(第.*节\\)")
                        matcher=pattern.matcher(Matcher.quoteReplacement(courseName))
                        var courseTime=if (matcher.find()){matcher.group()}else{""}
                        courseName=courseName.replace(courseTime,"")
                        courseTime=courseTime.replace(")","")
                        courseTime=courseTime.replace("(","")


                        //插入
                        when (tdControl) {
                            8 -> {
                                val myCourse = MyCourse(term, 1, courseName,coursePlace,courseTime)
                                myCourse.id = dao.insertMyCourse(myCourse)
                            }
                            2, 3, 4, 5, 6, 7 -> {
                                val myCourse = MyCourse(term, tdControl,courseName,coursePlace,courseTime)
                                myCourse.id = dao.insertMyCourse(myCourse)
                            }
                        }
                        if(td.attr("rowspan").toInt()>2){  //如果行数超过2，则说明为多节课程，计入dp
                            dp[tdControl]=(td.attr("rowspan").toInt())/2-1
                        }
                        tdControl++


                    }else{ //若进入else则说明有两节以上课的td
                        if (dp[tdControl]!=null){
                            dp[tdControl]=dp[tdControl]!!-1
                            if (dp[tdControl]==0){
                                dp.remove(tdControl)
                            }
                            tdControl++    //tdControl++ 跳过此无内容的课表格子，提取下一个  保证了星期与课程的正确对应
                        }

                        //提取课程，同上，不封装了
                        var pattern= Pattern.compile(".*节\\)")
                        var matcher=pattern.matcher(Matcher.quoteReplacement(td.text()))
                        var courseName = if (matcher.find()){ matcher.group() }else{ "" }


                        //提取课程地点，梅开二度
                        val place=td.text().replace(courseName,"")
                        pattern= Pattern.compile("(.){0,4}-([a-z0-9]){0,8}")
                        matcher=pattern.matcher(Matcher.quoteReplacement(place))
                        val coursePlace=if (matcher.find()){ matcher.group() }else{
                            pattern=Pattern.compile("([\u4E00-\u9FA5])*-.*([\u4E00-\u9FA5])*[0-9a-z]*")
                            matcher=pattern.matcher(Matcher.quoteReplacement(place))
                            if (matcher.find()){
                                matcher.group()
                            }else{""}
                        }

                        //提取课程时间
                        pattern= Pattern.compile("\\(第.*节\\)")
                        matcher=pattern.matcher(Matcher.quoteReplacement(courseName))
                        var courseTime=if (matcher.find()){matcher.group()}else{""}
                        courseName=courseName.replace(courseTime,"")
                        courseTime=courseTime.replace(")","")
                        courseTime=courseTime.replace("(","")


                        val myCourse=MyCourse(term, tdControl, courseName,coursePlace,courseTime)
                        myCourse.id=dao.insertMyCourse(myCourse)
                        tdControl++
                    }
                }
            } else if(trControl==12){
                break
            }
            trControl++
        }
    }


}