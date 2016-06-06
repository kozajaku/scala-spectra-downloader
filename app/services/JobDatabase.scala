package services

import javax.inject.Singleton
import scala.collection.mutable
import utils.model.JobInfo
/**
  * Created by radiokoza on 5.6.16.
  */
@Singleton
class JobDatabase {
  private val jobs: mutable.ArrayBuffer[JobInfo] =  mutable.ArrayBuffer()

  def += (jobInfo: JobInfo): JobDatabase = {
    jobs += jobInfo
    this
  }

  def addNewJob(jobInfo: JobInfo): Int = {
    val id = jobs.length
    this += jobInfo
    id
  }

  def apply(index: Int): JobInfo = {
    jobs(index)
  }

  def toArray: Array[JobInfo] = jobs.toArray
}
