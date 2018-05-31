package stocks

import akka.NotUsed
import akka.stream.ThrottleMode
import akka.stream.scaladsl.Source

import scala.concurrent.duration._

class Stock(val symbol: StockSymbol) {
  private val source: Source[StockQuote, NotUsed] = {
    Source.unfold(StockQuote(symbol, StockPrice(0.0))) { (last: StockQuote) =>
      Some(last, last)
    }
  }

  /**
    * Returns a source of stock history, containing a single element.
    */
  def history(n: Int): Source[StockHistory, NotUsed] = {
    source.grouped(n).map(sq => new StockHistory(symbol, sq.map(_.price))).take(1)
  }

  /**
    * Provides a source that returns a stock quote every 75 milliseconds.
    */
  def update: Source[StockUpdate, NotUsed] = {
    source
      .throttle(elements = 1, per = 75.millis, maximumBurst = 1, ThrottleMode.shaping)
      .map(sq => new StockUpdate(sq.symbol, sq.price))
  }

  override val toString: String = s"Stock($symbol)"
}


case class StockQuote(symbol: StockSymbol, price: StockPrice)

/** Value class for a stock symbol */
class StockSymbol private (val raw: String) extends AnyVal {
  override def toString: String = raw
}

object StockSymbol {
  import play.api.libs.json._ // Combinator syntax

  def apply(raw: String) = new StockSymbol(raw)

  implicit val stockSymbolReads: Reads[StockSymbol] = {
    JsPath.read[String].map(StockSymbol(_))
  }

  implicit val stockSymbolWrites: Writes[StockSymbol] = Writes {
    (symbol: StockSymbol) => JsString(symbol.raw)
  }
}

/** Value class for stock price */
class StockPrice private (val raw: Double) extends AnyVal {
  override def toString: String = raw.toString
}

object StockPrice {
  import play.api.libs.json._ // Combinator syntax

  def apply(raw: Double):StockPrice = new StockPrice(raw)

  implicit val stockPriceWrites: Writes[StockPrice] = Writes {
    (price: StockPrice) => JsNumber(price.raw)
  }
}

// Used for automatic JSON conversion
// https://www.playframework.com/documentation/2.6.x/ScalaJson

// JSON presentation class for stock history
case class StockHistory(symbol: StockSymbol, prices: Seq[StockPrice])

object StockHistory {
  import play.api.libs.json._ // Combinator syntax

  implicit val stockHistoryWrites: Writes[StockHistory] = new Writes[StockHistory] {
    override def writes(history: StockHistory): JsValue = Json.obj(
      "type" -> "stockhistory",
      "symbol" -> history.symbol,
      "history" -> history.prices
    )
  }
}

// JSON presentation class for stock update
case class StockUpdate(symbol: StockSymbol, price: StockPrice)

object StockUpdate {
  import play.api.libs.json._ // Combinator syntax

  implicit val stockUpdateWrites: Writes[StockUpdate] = new Writes[StockUpdate] {
    override def writes(update: StockUpdate): JsValue = Json.obj(
      "type" -> "stockupdate",
      "symbol" -> update.symbol,
      "price" -> update.price
    )
  }
}