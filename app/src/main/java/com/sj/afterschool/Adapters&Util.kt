package com.sj.afterschool


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Message
import android.os.Parcelable
import android.service.autofill.FieldClassification
import android.util.Log
import android.view.*

import android.widget.*

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.youth.banner.adapter.BannerAdapter
import com.youth.banner.adapter.BannerImageAdapter
import com.youth.banner.util.BannerUtils
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.calerdar_item.view.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


//各activity间传输cookie用接口
@Parcelize
data class CookieParcelable(val from:String,val recCookie:HashMap<String,String>):Parcelable

data class CourseItems(val courseName:String, var courseImage:String, val courseHerf:String)

data class CourseNodeItems(val nodename:String,val nodeType:Int,val nodestatus:Int,val href:String)
//日期list
data class CalendarAdapterList(val year:Int,val month:Int,val day:Int,val week:Int)

object Util{
    //状态栏透明
    fun setStatusBar(activity:Activity){
        val window: Window = activity.window
        window.setFormat(PixelFormat.TRANSLUCENT)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
    }
    //状态栏隐藏
    fun setTranslucent(activity: Activity){
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }
    //封装Jsoup,无requestBody
    fun myJsoup(url:String,method:Connection.Method,timeout:Int,cookie:HashMap<String,String>): Connection.Response {
        return Jsoup.connect(url)
                .header("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:80.0) Gecko/20100101 Firefox/80.0")
                .header("Connection","keep-alive")
                .followRedirects(true).ignoreContentType(true)
                .method(method)
                .timeout(timeout)
                .maxBodySize(0)
                .cookies(cookie)
                .execute()
    }
    //有requestBody
    fun myJsoup(url:String,method:Connection.Method,timeout:Int,cookie:HashMap<String,String>,requestBody:String): Connection.Response {
        return Jsoup.connect(url)
            .header("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:80.0) Gecko/20100101 Firefox/80.0")
            .header("Connection","keep-alive")
            .followRedirects(true).ignoreContentType(true)
            .method(method)
            .timeout(timeout)
            .maxBodySize(0)
            .cookies(cookie)
            .requestBody(requestBody)
            .execute()
    }
    //反防爬虫
    fun myJsoupToChaoXing(url:String,method:Connection.Method,timeout:Int,cookie:HashMap<String,String>): Connection.Response {
        return Jsoup.connect(url)
            .header("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.183 Safari/537.36 Edg/86.0.622.63")
            .header("Connection","keep-alive")
            .header("Cache-Control","max-age=0, no-cache")
            .header("Accept-Encoding","gzip,deflate")
            .header("Pragma","no-cache")
            .header("Upgrade-Insecure-Requests","1")
            .header("Accept-Language","zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2")
            .followRedirects(true)
            .method(method)
            .ignoreContentType(true)
            .timeout(timeout)
            .maxBodySize(0)
            .cookies(cookie)
            .execute()
    }
    //封装正则表达式
    fun myMatcher(compile:String,needText:String,groupNumber:Int):String?{
        val pattern= Pattern.compile(compile)
        val matcher=pattern.matcher(Matcher.quoteReplacement(needText))
        var counter=1
        while (matcher.find()){
            if (counter==groupNumber){
                return matcher.group()
            }
            counter++
        }

        return "none"
    }
    //二次匹配回调的js数据中需要的内容(jobid,info等)
    fun myMatcher(compile: String,needText: String,groupNumber: Int,mode: Int):String?{
        val data=myMatcher(compile,needText,groupNumber).toString()
        val matcher:Matcher=when(mode){
            1->{
                val pattern=Pattern.compile("[0-9]*[0-9]")  //[0-9].*重复匹配多次
                pattern.matcher(Matcher.quoteReplacement(data))
            }
            2->{
                var filter:String=""
                val filterPattern=Pattern.compile("\"[a-z].*\":\"") //过滤不需要字符串，最终只留下所需数据
                val filterMatcher=filterPattern.matcher(data)
                if (filterMatcher.find()){
                    filter=filterMatcher.replaceAll("")
                }
                val pattern=Pattern.compile("[0-9a-z]*[0-9a-z]")
                pattern.matcher(Matcher.quoteReplacement(filter))
            }
            else -> {
                val pattern=Pattern.compile("http://.*[0-9]")
                pattern.matcher(Matcher.quoteReplacement(data))
            }
        }
        return if(matcher.find()){
            matcher.group()
        }else{
            "none"
        }
    }
}



//课程RecyclerView Adapter，实现上拉异步加载
class CourseAdapter(
    private val context: Context,
    private var courselist: ArrayList<CourseItems>):RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //普通布局
    private val typeItem=1
    //脚布局
    private val typeFooter=2
    //加载状态
    private var loadState=2
    val loading=1
    val loadingComplete=2
    val loadingEnd=3

    private lateinit var mOnItemClickListener: OnItemClickListener


    override fun getItemViewType(position: Int): Int =if (position+1==itemCount){typeFooter}else{typeItem}


    class ItemViewHodler(view: View):RecyclerView.ViewHolder(view){
        val image:ImageView=view.findViewById(R.id.courseimage)
        val name:TextView=view.findViewById(R.id.coursename)
        val imageicon:ImageView=view.findViewById(R.id.courseimageicon)
    }

    class FooterViewHodler(view: View):RecyclerView.ViewHolder(view){
        val pbLoading:ProgressBar=view.findViewById(R.id.pb_loading)
        val tvLoading:TextView=view.findViewById(R.id.tv_loading)
        val llEnd:LinearLayout=view.findViewById(R.id.ll_end)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):RecyclerView.ViewHolder {
        return when (viewType) {
            typeItem -> {
                val view = LayoutInflater.from(context).inflate(R.layout.class_item, parent, false)
                ItemViewHodler(view)
            }
            else -> {
                val view =
                    LayoutInflater.from(context).inflate(R.layout.class_item_footer, parent, false)
                FooterViewHodler(view)
            }
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHodler){
            holder.name.text= courselist[position].courseName
            if (courselist[position].courseImage=="/space/courselist/images/defaultcover.jpg")
                courselist[position].courseImage="http://mooc.hlju.edu.cn/space/courselist/images/defaultcover.jpg"
            Glide.with(context).load(courselist[position].courseImage)
                .apply(RequestOptions.bitmapTransform(BlurTransformation(15,2)))
                .into(holder.image)
            Glide.with(context).load(courselist[position].courseImage)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(15)))
                .into(holder.imageicon)
            holder.itemView.setOnClickListener{
                mOnItemClickListener.onItemClick(holder.itemView,position)
            }
//            holder.itemView.setOnLongClickListener{}

        }else if (holder is FooterViewHodler){
            when(loadState){
                loading->{
                    holder.pbLoading.visibility=View.VISIBLE
                    holder.tvLoading.visibility=View.VISIBLE
                    holder.llEnd.visibility=View.GONE
                }
                loadingComplete->{
                    holder.pbLoading.visibility=View.INVISIBLE
                    holder.tvLoading.visibility=View.INVISIBLE   //不显示控件，但保留控件占有空间
                    holder.llEnd.visibility=View.GONE           //不显示控件，这里需要不保留控件占有空间
                }
                loadingEnd->{
                    holder.pbLoading.visibility=View.GONE
                    holder.tvLoading.visibility=View.GONE
                    holder.llEnd.visibility=View.VISIBLE
                }
            }

        }
    }


    interface OnItemClickListener{ //recyclerveiw 点击事件自定义接口
        fun onItemClick(view:View,position:Int)
//      fun onItemLongClick(view:View,position:Int)
    }


    fun setOnItemClickListener(mOnItemClickListener: OnItemClickListener){
        this.mOnItemClickListener=mOnItemClickListener
    }


    override fun getItemCount()=courselist.size+1

    fun getState():Int=this.loadState

    fun setLoadState(loadState:Int,
                     oldCourseList:ArrayList<CourseItems>,
                     newCourseList:ArrayList<CourseItems>,
                     rAdapter:RecyclerView.Adapter<RecyclerView.ViewHolder>){
        this.loadState=loadState
//使用DiffUitl进行增量更新，优化了notifyDataSetChanged造成的卡顿，数据不多，暂时不开启单独线程计算
//当开启线程计算diffutil.result时会出现滑动过快时，因为计算结果未结束，在主线程强制更新导致程序崩溃
        val diffUtil=DiffUtil.calculateDiff(CourseCallBack(oldCourseList,newCourseList),true)
        diffUtil.dispatchUpdatesTo(rAdapter)
    }

//判断网格布局，使footer布局横跨所有网格
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val manger=recyclerView.layoutManager
        if(manger is GridLayoutManager){
            manger.spanSizeLookup=object :GridLayoutManager.SpanSizeLookup(){
                override fun getSpanSize(position: Int): Int {
                    return if (getItemViewType(position) == typeFooter) manger.spanCount else 1
                }
            }
        }
    }

}


//courseRecyclerView 滑动监听
abstract class EndlessRecyclerOnScrollListener: RecyclerView.OnScrollListener() {
    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        val manger=recyclerView.layoutManager as GridLayoutManager
        if (newState==RecyclerView.SCROLL_STATE_IDLE){
            if (manger.findLastVisibleItemPosition()==recyclerView.adapter!!.itemCount-1){
                onLoadMore()
            }
        }
    }

//    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//        super.onScrolled(recyclerView, dx, dy)
//    }

    abstract fun onLoadMore()
}

//DiffUtil
class CourseCallBack() :DiffUtil.Callback(){
    private var oldCourse=ArrayList<CourseItems>()
    private var newCourse=ArrayList<CourseItems>()
    constructor(oldList: ArrayList<CourseItems>, newList: ArrayList<CourseItems>):this(){
        this.newCourse=newList
        this.oldCourse=oldList
    }

    override fun getOldListSize():  Int=oldCourse.size

    override fun getNewListSize():  Int=newCourse.size


//判断名字是否相同
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldCourse[oldItemPosition].courseName == newCourse[newItemPosition].courseName
    }
//可扩展判断内容是否相同
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val courseOld=oldCourse[oldItemPosition]
        val courseNew=newCourse[newItemPosition]
//        if (!courseOld)
        return true

    }
}

//进入课程时加载课程节点的recyclerviewAdapter
class NodesAdapter(val context: Context,val nodesList:ArrayList<CourseNodeItems>):RecyclerView.Adapter<RecyclerView.ViewHolder>()
{
    private val typeTitle=0
    private val typeItem=1
    private lateinit var mOnItemClickListener: OnItemClickListener

    class NodeTitle(view: View):RecyclerView.ViewHolder(view){
        val chapterNumber:TextView=view.findViewById(R.id.chapterNumberTV)

    }
    class NodeItem(view: View):RecyclerView.ViewHolder(view){
        val chapterNumber:TextView=view.findViewById(R.id.chapterNumberItemTV)
        val chapterStatus:ImageView=view.findViewById(R.id.chapterstatus)
    }

    override fun getItemCount(): Int=nodesList.size

    override fun getItemViewType(position: Int): Int {
        return when(nodesList[position].nodeType){
            0->{
               typeTitle
            }
            else->{
                typeItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            typeTitle->{
                val view=LayoutInflater.from(context).inflate(R.layout.node_title,parent,false)
                NodeTitle(view)
            }
            else->{
                val view=LayoutInflater.from(context).inflate(R.layout.node_item,parent,false)
                NodeItem(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NodeTitle){
            holder.chapterNumber.text=nodesList[position].nodename
        }else if (holder is NodeItem){
            holder.chapterNumber.text=nodesList[position].nodename
            when(nodesList[position].nodestatus){
                1->{
                    Glide.with(context).load(R.drawable.smile).into(holder.chapterStatus)
                }
                2->{
                    Glide.with(context).load(R.drawable.sad).into(holder.chapterStatus)
                }
                0->{
                    Glide.with(context).load(R.drawable.lock).into(holder.chapterStatus)
                }
            }
            holder.itemView.setOnClickListener{
                mOnItemClickListener.onItemClick(holder.itemView,position)
            }
        }
    }

    interface OnItemClickListener  {
        fun onItemClick(view: View,position: Int)
//      fun onItemLongClick(view:View,position:Int)
    }

    fun setOnItemClickListener(mOnItemClickListener: OnItemClickListener){
        this.mOnItemClickListener=mOnItemClickListener
    }

}





class CalendarAdapter(val context: Context,val list:ArrayList<CalendarAdapterList>,val dao:MyCourseDao,val map: HashMap<String,String>):RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    class CalendarItem(view: View):RecyclerView.ViewHolder(view){
        val weekTextView:TextView=view.findViewById(R.id.calendar_myweek)
        val dayTextView:TextView=view.findViewById(R.id.calendar_myday)
        val yearMonthTextView:TextView=view.findViewById(R.id.calendar_myyear)
        val curriculumRecyclerView:RecyclerView=view.findViewById(R.id.curriculumRecyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view=LayoutInflater.from(context).inflate(R.layout.calerdar_item,parent,false)
        return CalendarItem(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.itemView.calendar_myweek.tag = position
        if(holder is CalendarItem){
            holder.dayTextView.text= list[position].day.toString()
                when(list[position].week) {
                    1 -> {
                        holder.weekTextView.text="周日"
                        holder.curriculumRecyclerView.layoutManager=
                            LinearLayoutManager(context)
                    }
                    2 -> {
                        holder.weekTextView.text="周一"
                        holder.curriculumRecyclerView.layoutManager=
                            LinearLayoutManager(context)
                    }
                    3 -> {
                        holder.weekTextView.text="周二"
                        holder.curriculumRecyclerView.layoutManager=
                            LinearLayoutManager(context)
                    }
                    4 -> {
                        holder.weekTextView.text="周三"
                        holder.curriculumRecyclerView.layoutManager=
                            LinearLayoutManager(context)
                    }
                    5 -> {
                        holder.weekTextView.text="周四"
                        holder.curriculumRecyclerView.layoutManager=
                            LinearLayoutManager(context)
                    }
                    6 -> {
                        holder.weekTextView.text="周五"
                        holder.curriculumRecyclerView.layoutManager=
                            LinearLayoutManager(context)
                    }
                    else -> {
                        holder.weekTextView.text="周六"
                        holder.curriculumRecyclerView.layoutManager=
                            LinearLayoutManager(context)
                    }
                }
                val year=list[position].year.toString()
                val month:Int=list[position].month
                holder.yearMonthTextView.text=
                    year+"年"+month.toString()+"月"
                curriculumRecyclerView(list[position].week,holder)
            }
        }

    override fun getItemCount(): Int=list.size

    private fun curriculumRecyclerView(week: Int,holder: RecyclerView.ViewHolder){
        holder as CalendarItem
        val curriculumHandler= @SuppressLint("HandlerLeak")
        object :Handler(){
            override fun handleMessage(msg: Message) {
                when(msg.what){
                    1->{
                        val resultCourse=msg.obj as List<QueryCourseName>

                        val layoutManager=LinearLayoutManager(context)
                        holder.curriculumRecyclerView.layoutManager=layoutManager
                        holder.curriculumRecyclerView.adapter=CurriculumAdapter(context,resultCourse)
                    }
                }
            }
        }

        /*
* 先进行官网当前学期与数据库学期进行比对，如果一致则直接加载到recyclerview中
* 如果不一致则需要更新数据库，再加载到recyclerView中
*
* 下面进行网络请求加载课程、读取课程数据库，在它起作用之前，LoginHD，也就是登录页面，必须顺利运行过至少一次来保证持有相应的cookie通过cas单点登录系统
**/
        Thread{
            val courseMessage=Message()
            courseMessage.what=1
            when(week){
                1 -> {
                    courseMessage.obj=dao.queryCourseSunday()
                }
                2 -> {
                    courseMessage.obj=dao.queryCourseMonday()

                }
                3 -> {
                    courseMessage.obj=dao.queryCourseTuesday()

                }
                4 -> {
                    courseMessage.obj=dao.queryCourseWednesday()

                }
                5 -> {
                    courseMessage.obj=dao.queryCourseThursday()

                }
                6 -> {
                    courseMessage.obj=dao.queryCourseFriday()

                }
                else -> {
                    courseMessage.obj=dao.queryCourseSaturday()

                }
            }
            curriculumHandler.sendMessage(courseMessage)
            }.start()
        }
    }







class CurriculumAdapter(val context: Context, val list: List<QueryCourseName>):RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    class CurriculumItem(view: View):RecyclerView.ViewHolder(view){
        val itemCourseTextView:TextView=view.findViewById(R.id.curriculum_recyclerview_coursename_textview)
        val itemCoursePlaceTextView:TextView=view.findViewById(R.id.curriculum_recyclerview_courseplace_textview)
        val itemCourseTimeTextView:TextView=view.findViewById(R.id.curriculum_recyclerview_coursetime_textview)
        val itemIndicator:View=view.findViewById(R.id.text_indicator)

        val itemCoursePlaceImageView:ImageView=view.findViewById(R.id.curriculum_recyclerview_courseplace_imageview)
        val itemCourseTimeImageView:ImageView=view.findViewById(R.id.curriculum_recyclerview_coursetime_imageview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view=LayoutInflater.from(context).inflate(R.layout.curriculum_recyclerview_item,parent,false)
        return CurriculumItem(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CurriculumItem){
            if (list[position].courseName!=""&&list[position].coursePlace!=""){
                holder.itemCourseTextView.visibility=View.VISIBLE
                holder.itemCoursePlaceTextView.visibility=View.VISIBLE
                holder.itemCourseTimeTextView.visibility=View.VISIBLE
                holder.itemIndicator.visibility=View.VISIBLE
                holder.itemCoursePlaceImageView.visibility=View.VISIBLE
                holder.itemCourseTimeImageView.visibility=View.VISIBLE


                holder.itemCourseTextView.text=list[position].courseName
                holder.itemCoursePlaceTextView.text=list[position].coursePlace
                holder.itemCourseTimeTextView.text=list[position].courseTime
            }
        }
    }

    override fun getItemCount(): Int=list.size
}







/*
* banner Adapter
* */
class HomeBannerAdapter(bannerImageList:List<Int>):BannerAdapter<Int, HomeBannerAdapter.ImageHolder>(bannerImageList) {
    private lateinit var mOnImageClickListener:OnImageClickListener

    class ImageHolder(view: View) : RecyclerView.ViewHolder(view) {
        var imageView: ImageView = view as ImageView
    }

    override fun onCreateHolder(parent: ViewGroup?, viewType: Int): ImageHolder {
        val imageView=ImageView(parent!!.context)
        val params=ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)
        imageView.layoutParams=params
        imageView.scaleType=ImageView.ScaleType.FIT_XY
        BannerUtils.setBannerRound(imageView,20f)
        return ImageHolder(imageView)
    }

    override fun onBindView(holder: ImageHolder?, data: Int?, position: Int, size: Int) {
        Glide.with(holder!!.itemView).load(data).into(holder.imageView)
        holder.itemView.setOnClickListener {
            mOnImageClickListener.onImageClick(holder.itemView,position)
        }
    }

    interface OnImageClickListener{
        fun onImageClick(view: View,position: Int)
    }

    fun setOnImageClickListener(mOnImageClickListener: OnImageClickListener){
        this.mOnImageClickListener=mOnImageClickListener
    }

}





