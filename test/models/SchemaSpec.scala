package models

import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import slick.driver.SQLiteDriver.api._

class SchemaSpec  extends PlaySpec with OneAppPerTest {
  "Accounts" should {
    "print the table schema" in {
      Accounts.schema.createStatements.foreach(println)
    }
  }
}
