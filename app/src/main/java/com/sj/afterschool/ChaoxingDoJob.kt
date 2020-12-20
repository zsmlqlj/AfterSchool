package com.sj.afterschool


import android.util.Log
import org.jsoup.Connection
import java.io.IOException
import java.math.BigInteger
import java.net.SocketTimeoutException
import java.security.MessageDigest
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.log


/*
* 实现类,需在线程中初始化和使用
*
* cookieA:获取学习通章节信息的cookie
* hrefList:提取当前课程所有章节的list
* currentPosition:记录开始的章节位置
* time:控制刷课时间
* */
data class ChaoxingDoJob(val cookieA: HashMap<String, String>,val hrefList:ArrayList<CourseNodeItems>,var currentPosition:Int, val time: Int){
    private var userid: String
    private var progressControl:Int=1

    init {
        val parse=Util.myJsoup(hrefList[currentPosition].href, Connection.Method.GET, 5000, cookieA).parse().toString()
        val mixUserid=Util.myMatcher("userId : '[0-9]*'", parse, 1, 1)!!
        userid=mixUserid
        cookieA["isfyportal"]="1"
        cookieA.remove("amlbcookie")
    }

    companion object{
        @Volatile
        private var chaoxingInstance:ChaoxingDoJob?=null
        fun getInstance(cookieA: HashMap<String, String>,hrefList: ArrayList<CourseNodeItems>,currentPosition: Int,time: Int):ChaoxingDoJob{
            return synchronized(this){
                val mchaoxingInstance= chaoxingInstance
                if (mchaoxingInstance!=null){
                    mchaoxingInstance
                }else{
                    val chaoxingInstanceCreater=ChaoxingDoJob(cookieA, hrefList, currentPosition, time)
                    chaoxingInstance=chaoxingInstanceCreater
                    chaoxingInstanceCreater
                }
            }
        }
    }

    private fun getParse():String{
        try {
            Log.w("eee",hrefList[currentPosition].href)
            val parseResult=Util.myJsoup(hrefList[currentPosition].href, Connection.Method.GET, 5000, cookieA)
            cookieA.putAll(parseResult.cookies())
            return parseResult.parse().toString()
        }catch (e: IOException){
            throw IOException("ChaoxingDoJob parse error")
        }
    }

    //提取knowledgeCards
    private fun getKnowledgeCard(chapterHref: String):String{
        val courseId=Util.myMatcher("courseId=[0-9]*", chapterHref, 1, 1)!!
        val clazzId=Util.myMatcher("classId=[0-9]*", chapterHref, 1, 1)!!
        val chapterId=Util.myMatcher("chapterId=[0-9]*", chapterHref, 1, 1)!!
        val lastUrl=Util.myMatcher("&ut=.*[0-9]", chapterHref, 1)
        return "http://mooc1.hlju.edu.cn/knowledge/cards?clazzid=$clazzId&courseid=$courseId&" +
                "knowledgeid=$chapterId&num=0"+lastUrl
    }

    //提取一个章节中所有任务
    private fun getTask(knowledgeCards: String):Queue<HashMap<String, String>>{
        val taskQueue:Queue<HashMap<String, String>> = LinkedList<HashMap<String, String>>()
            try {
                val patternCards=Pattern.compile("num=0")
                val matcherCards=patternCards.matcher(knowledgeCards)
                var card=0
                while (true){
                    //卡片循环
                    var knowledgeCardsUrl=""
                    if(matcherCards.find()){
                        knowledgeCardsUrl=matcherCards.replaceAll("num=$card")
                        matcherCards.reset()
                    }
                    val mArgResponse= Util.myJsoup(knowledgeCardsUrl, Connection.Method.GET, 5000, cookieA)
                    val mArg=mArgResponse.parse()
                    Log.w("eee",knowledgeCardsUrl)
                    /*
                    * 由于学习通的学习卡片接口在 "num=任何数" 时都可以生成能进入的网址，为防止发出无用请求导致进入爬虫检测，这里做出判断
                    *
                    * 如果title.text为空且找不到mArg中的任何内容，则说明num已经超额了，直接结束循环
                    * 如果没匹配到$mArg，则说明含有内容，初步判断可能有刷课内容，尝试提取相关内容
                    * 如果匹配到了$mArg，则说明无内容，一般这种情况是不需要刷课的课程
                    * */
                    if (mArg.select("title").text()=="" &&  Util.myMatcher("\"jobid\":\"[0-9]*\"", mArg.toString(), 1, 1)=="none"){
                        break
                    }
                    else if (Util.myMatcher("mArg=.*mArg", mArg.toString().replace(" ",""), 0)=="none"){
                        try {
                            var node=1
                            while (true){//任务点循环
                                val jobid=Util.myMatcher("\"jobid\":[\"]?[0-9]*[\"]?", mArg.toString(), 2 * node, 1)!! //同一个jobid有两个，用2*node提取第二个
                                if (jobid=="none"){
                                    break   //找不到jobid，则遍历任务点结束
                                }else{
                                    val otherInfo=Util.myMatcher(
                                        "nodeId_[0-9]*-cpi_[0-9]*",
                                        mArg.toString(),
                                        node
                                    )!!
                                    val objectId=Util.myMatcher(
                                        "\"objectid\":\"[0-9a-z]*\"",
                                        mArg.toString(),
                                        node,
                                        2
                                    )!!
                                    val clazzId=Util.myMatcher("\"clazzId\":[0-9]*", mArg.toString(), 1, 1)!! //clazzId只有一个
                                    val dtype=Util.myMatcher("\"type\":\"\\.*[a-z]*[0-9]*\"", mArg.toString(), node, 2)!!
                                    val reportUrl=Util.myMatcher(
                                        "\"reportUrl\":\"http://mooc1.hlju.edu.cn/multimedia/log/a/[0-9]*\"",
                                        mArg.toString(),
                                        1,
                                        3
                                    )!!
                                    val reportTimeInterval=Util.myMatcher(
                                        "\"reportTimeInterval\":[0-9]*",
                                        mArg.toString(),
                                        1,
                                        1
                                    )!!
                                    val anans=Util.myJsoup(
                                        "http://mooc1.hlju.edu.cn/ananas/status/$objectId?k=&flag=normal&_dc=${Date().time}",
                                        Connection.Method.GET, 5000, cookieA
                                    ).parse().toString()
                                    val duration=Util.myMatcher("\"duration\":[0-9]*", anans, 1, 1)!!
                                    val dtoken=Util.myMatcher(
                                        "\"dtoken\":\"[0-9a-z]*\"",
                                        anans,
                                        1,
                                        2
                                    )!!


                                    val task=HashMap<String, String>()
                                    task["otherInfo"]=otherInfo
                                    task["objectId"]=objectId
                                    task["clazzId"]=clazzId
                                    task["duration"]=duration
                                    task["clipTime"]="0_$duration"
                                    task["jobid"]=jobid
                                    task["userid"]=userid
                                    task["dtype"]=dtype
                                    task["dtoken"]=dtoken
                                    task["reportUrl"]=reportUrl
                                    task["reportTimeInterval"]=reportTimeInterval

                                    taskQueue.add(task)
                                    node++
                                }
                            }
                        }catch (e: IOException){
                            throw IOException("getTask Node IOException")
                        }catch (e: SocketTimeoutException){
                            throw SocketTimeoutException("getTask Node socketTimeout")
                        }
                        card++
                    }else{
                        //todo 卡片内无任务
                        //todo 处理测试题类型任务
                        card++
                        continue
                    }
                }
            }catch (e: Exception){
            }catch (e: SocketTimeoutException){
                throw SocketTimeoutException("getTask socketTimeout")
            }
        return taskQueue
    }

    private fun doTask(queue: Queue<HashMap<String, String>>){
        while (when(queue.size){
                0 -> {
                    false
                } else->{true} }){
            val mapElement=queue.poll()!!
            var logTime=0
            val cookieB=HashMap<String,String>()  //首次访问刷课接口时，不带cookie并获取返回的cookie，接下来带着返回的cookie刷本章节

            Thread.sleep(2000)
            val result=Util.myJsoupToChaoXing(buildUrl(mapElement, logTime), Connection.Method.GET, 5000, cookieB)
            val resultText=result.parse().toString()
            cookieB.putAll(result.cookies())
            val firstCheck=Util.myMatcher(
                "false",
                resultText,
                1
            )!!
            if (firstCheck=="none"){
                Thread.sleep(2000)
                continue  //如果请求返回的是true则check结果为none，表示已经完成此任务点，continue到下一个
            }else{
                while (true){
                    Thread.sleep(60000)
                    logTime+=60
                    val thisDuration=mapElement["duration"]!!.toInt()
                    if (logTime>thisDuration) logTime=thisDuration
                    val responseCheck=Util.myMatcher(
                        "\"isPassed\":false",
                        Util.myJsoupToChaoXing(buildUrl(mapElement, logTime), Connection.Method.GET, 5000, cookieB)
                            .parse().toString(),
                        1
                    )
                    Log.w("eee", "log一次")
                    if (responseCheck=="none") break
                }
            }
        }
    }

    private fun refreshHref():Boolean{  //更新链接为下一个章节
        return if(currentPosition==hrefList.size)
            false
        else {
            currentPosition++
            true
        }
    }

    //学习通enc加密编码方式为md5
    private fun enc(
        clazzId: String, userid: String, jobId: String, objectId: String, currentTimeSec: Int,
        duration: String, clipTime: String
    ):String{
        val md5="[$clazzId][$userid][$jobId][$objectId][${currentTimeSec*1000}][d_yHJ!\$pdA~5][${duration.toInt()*1000}][$clipTime]"
        val md=MessageDigest.getInstance("MD5")
        val charArray: CharArray = md5.toCharArray()
        val byteArray = ByteArray(charArray.size)
        for (i in charArray.indices) byteArray[i] = charArray[i].toByte()
        val md5Bytes: ByteArray = md.digest(byteArray)
        val hexValue = StringBuffer()
        for (i in md5Bytes.indices) {
            val transToInt = md5Bytes[i].toInt() and 0xff
            if (transToInt < 16) hexValue.append("0")
            hexValue.append(Integer.toHexString(transToInt))
        }
        return hexValue.toString().toLowerCase(Locale.ROOT)
    }

    //组装url
    private fun buildUrl(queueElement: HashMap<String, String>, timeSec: Int):String{
        val preUrl=queueElement["reportUrl"]
        val encode=enc(
            queueElement["clazzId"]!!,
            queueElement["userid"]!!,
            queueElement["jobid"]!!,
            queueElement["objectId"]!!,
            timeSec,
            queueElement["duration"]!!,
            "0_${queueElement["duration"]}"
        )
        val midUrl="${queueElement["dtoken"]}?clazzId=${queueElement["clazzId"]}&playingTime=$timeSec&duration=${queueElement["duration"]}&" +
                "clipTime=0_${queueElement["duration"]}&objectId=${queueElement["objectId"]}&otherInfo=${queueElement["otherInfo"]}&" +
                "jobid=${queueElement["jobid"]}&userid=${queueElement["userid"]}&isdrag=0&view=pc&enc=$encode&rt=0.9&" +
                "dtype=${queueElement["dtype"]}&_t=${Date().time}"
        return "$preUrl/$midUrl"
    }





    /*对外接口
    * checkProgressFinish检查是否有任务存在
    * startJob负责开启任务
    * */
    fun checkProgressFinish():Boolean{
        return progressControl==1
    }

    fun startJob(){
        progressControl=0
        var jobControl=true
        while (jobControl){
            //获得卡片url
            val cards=getKnowledgeCard(getParse())
            doTask(getTask(cards))
            jobControl=refreshHref()
        }
        progressControl=1
    }









}