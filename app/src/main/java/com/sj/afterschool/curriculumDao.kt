package com.sj.afterschool

import android.content.Context
import androidx.room.*


/*
*数据库代码
* 使用了Jetpack中Room数据库，具体用法可以查询 Android Room
* */

@Entity
data class MyCourse(var term: String, var week: Int, var course: String,var place:String,var time:String){
    @PrimaryKey(autoGenerate = true)
    var id:Long=0
}


class QueryCourseName{
    @ColumnInfo(name = "course")
    var courseName:String?=null
    @ColumnInfo(name = "place")
    var coursePlace:String?=null
    @ColumnInfo(name = "time")
    var courseTime:String?=null
}



class QueryTerm{
    @ColumnInfo(name = "term")
    var courseTerm:String?=null
}


@Dao
interface MyCourseDao{

    @Insert
    fun insertMyCourse(myCourse: MyCourse):Long

    @Update
    fun updateMyCourse(myCourse: MyCourse)

    @Delete
    fun deleteMyCourse(myCourse: MyCourse)

    @Query("DELETE FROM MyCourse")
    fun deleteAllMyCourse()

    @Query("select term from MyCourse where id=1")
    fun queryTerm():QueryTerm



    /*
    * 查询课程信息
    * */
    @Query("select course,place,time from MyCourse where week=1")
    fun queryCourseSunday():List<QueryCourseName>

    @Query("select course,place,time from MyCourse where week=2")
    fun queryCourseMonday():List<QueryCourseName>

    @Query("select course,place,time from MyCourse where week=3")
    fun queryCourseTuesday():List<QueryCourseName>

    @Query("select course,place,time from MyCourse where week=4")
    fun queryCourseWednesday():List<QueryCourseName>

    @Query("select course,place,time from MyCourse where week=5")
    fun queryCourseThursday():List<QueryCourseName>

    @Query("select course,place,time from MyCourse where week=6")
    fun queryCourseFriday():List<QueryCourseName>

    @Query("select course,place,time from MyCourse where week=7")
    fun queryCourseSaturday():List<QueryCourseName>




}

@Database(version = 1, entities = [MyCourse::class])
abstract class MyCourseDatabase: RoomDatabase(){
    abstract fun mycourseDao():MyCourseDao

    companion object{
        private var instance:MyCourseDatabase?=null

        @Synchronized
        fun getDatabase(context: Context):MyCourseDatabase{
            instance?.let {
                return it
            }
            return Room.databaseBuilder(
                context.applicationContext,
                MyCourseDatabase::class.java,
                "mycourse_database"
            )
                .build().apply {
                    instance=this
                }
        }
    }
}