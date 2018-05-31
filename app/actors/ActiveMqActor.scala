package actors


import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import akka.stream.alpakka.jms.JmsProducerSettings
import akka.stream.alpakka.jms.scaladsl.JmsProducer
import akka.stream.scaladsl.{Flow, Keep, MergeHub, Sink, Source}
import akka.stream.{Materializer, ThrottleMode}
import akka.{Done, NotUsed}
import javax.inject.Inject
import javax.jms.ConnectionFactory
import org.apache.activemq.ActiveMQConnectionFactory
import play.api.Configuration
import stocks.{Stock, StockSymbol}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ActiveMqActor @Inject()(configuration : Configuration)
                             (implicit  mat: Materializer,
                              ec: ExecutionContext)
  extends Actor with ActorLogging{
  import actors.Messages._

  private val stocksMap: mutable.Map[StockSymbol, Stock] = mutable.HashMap()


  val connectionFactory: ConnectionFactory = new ActiveMQConnectionFactory(configuration.get[String]("active_mq.url"))
  val activeMqSubscribeSink: Sink[String, Future[Done]] = JmsProducer.textSink(
    JmsProducerSettings(connectionFactory).withQueue("subscribe")
  )

  val (hubSink, _) = MergeHub.source[String](perProducerBufferSize = 16).toMat(activeMqSubscribeSink)(Keep.both).run()
  val decoratedSink = Flow.fromFunction[String, String](a ⇒ {
    log.info(a)
    println(a)
    a
  }).to(hubSink)

  val source: Source[String, NotUsed] = {
    Source.unfold("a") { last ⇒
      Some("a", "b")
    }
  }

  val update: Source[String, NotUsed] = {
    source.throttle(elements = 2, per = 2.seconds, maximumBurst = 1, ThrottleMode.shaping)
  }

  override def receive: Receive = LoggingReceive {
    case WatchStocks(symbols) ⇒
      sender() ! websocketFow
  }

  private lazy val websocketFow: Flow[String, String, NotUsed] = {
    Flow.fromSinkAndSourceCoupled(decoratedSink, update)
  }
}
