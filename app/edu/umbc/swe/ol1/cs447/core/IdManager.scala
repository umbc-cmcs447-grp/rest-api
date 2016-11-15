package edu.umbc.swe.ol1.cs447.core

import java.security.SecureRandom
import java.util.Base64
import javax.inject.{Inject, Singleton}

import akka.agent.Agent
import com.google.common.hash.{BloomFilter, Funnels}
import models.Posts
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits._
import slick.driver.JdbcProfile

import scala.concurrent.{Future, Promise}

@Singleton
class IdManager @Inject()(dbConfProvider: DatabaseConfigProvider) {
  private val dbConf = dbConfProvider.get[JdbcProfile]
  private val db = dbConf.db
  private val rand = new SecureRandom
  private val bloomFilterAgentFuture = bloomFilterOfIds map Agent[BloomFilter[Array[Byte]]]

  import dbConf.driver.api._

  private def bloomFilterOfIds: Future[BloomFilter[Array[Byte]]] = {
    val bloomFilter = BloomFilter.create[Array[Byte]](Funnels.byteArrayFunnel(), 100000)
    for {
      seq <- db.run(Posts.map(_.postId).result)
      _ = seq.toStream
        .map(Base64.getUrlDecoder.decode)
        .foreach(bloomFilter.put)
    } yield bloomFilter
  }

  def newId: Future[String] = {
    for {
      agent <- bloomFilterAgentFuture
      p = Promise[String]()
      _ = agent.send(bloomFilter => {
        val idBytes = genNewId(bloomFilter)
        p.success(Base64.getUrlEncoder.encodeToString(idBytes))
        bloomFilter.put(idBytes)
        bloomFilter
      })
      id <- p.future
    } yield id
  }

  private def genNewId(bloomFilter: BloomFilter[Array[Byte]]): Array[Byte] = {
    Stream continually {
      val bytes = Array.ofDim[Byte](Posts.postIdLengthBytes)
      rand.nextBytes(bytes)
      bytes
    } dropWhile bloomFilter.mightContain
  }.head
}
