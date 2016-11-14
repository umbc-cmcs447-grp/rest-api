package models

import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import slick.driver.SQLiteDriver.api._

class SchemaSpec  extends PlaySpec with OneAppPerTest {
  "Accounts" should {
    "print the table schema" in {
      Accounts.schema.createStatements.foreach(println)
    }
  }

  "Users" should {
    "print the table schema" in {
      Users.schema.createStatements.foreach(println)
    }
  }

  "Posts" should {
    "print the table schema" in {
      Posts.schema.createStatements.foreach(println)
    }
  }
}
