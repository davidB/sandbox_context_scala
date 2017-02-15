package sandbox

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorSystem, Props}
import context.propagation.threadlocal.CurrentCtxLocalThread
import context._
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._
import org.slf4j.{LoggerFactory, MDC}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by davidb on 11/02/17.
  */
class PropapationSpec extends FlatSpec {
    CurrentCtx.instance = CurrentCtxLocalThread.instance
    val currentCtx = CurrentCtx
    val ctxFactory = new CtxFactory0()
    val ctxTools = new CtxTools(currentCtx, ctxFactory)
    val pcol = new PropagationCollector(currentCtx)

    "Implicit Propagation" should "work as explicit for sync (no future) chaining with sub ctx" in {
        val service = new FakeService(ctxTools, pcol)
        pcol.report(None)
        ctxTools.withCtx(ctxTools.newCtx("0")) { implicit ctx0 =>
            pcol.report(ctx0)
            service.doJobInNewCtx("job1")
            pcol.report(ctx0)
            service.doJobNoReturn()
            pcol.report(ctx0)
            service.doJobInNewCtx("job2")
            pcol.report(ctx0)
        }
        pcol.report(None)

        pcol.checkCollected()
    }

    it should "work as explicit for simple future chaining" in {
        val service = new FakeService(ctxTools, pcol)
        pcol.report(None)
        ctxTools.withCtx(ctxTools.newCtx("0")) { implicit ctx0 =>
            pcol.report(ctx0)

            val m0 = "m0"
            val chain = service.doJobAsync(m0).map(v => service.doJobInNewCtx(v)).flatMap(v => service.doJobAsyncInNewCtx(v))
            Await.ready(chain, 3.seconds)
        }
        pcol.report(None)
        pcol.checkCollected()
    }

    it should "work as explicit for simple actor" in {
        val system = ActorSystem("test")
        val actor = system.actorOf(Props(new FakeActor(ctxTools, pcol)))
        pcol.report(None)
        ctxTools.withCtx(ctxTools.newCtx("0")) { implicit ctx0 =>
            pcol.report(ctx0)
            actor ! MyMessage("m0", 3)
            Thread.sleep(2000)
        }
        pcol.report(None)
        pcol.checkCollected()
    }

//    "PropagationCollector.checkCollected" should "failed on invalid" in {
//        val pcol = new PropagationCollector(() => Some("ok"))
//        pcol.report("ok")
//        pcol.report("ko")
//        pcol.report("ok")
//        pcol.checkCollected()
//    }

    "PropagationCollector.checkCollected" should "pass on valid" in {
        val ctx = Some(ctxFactory.newCtx("ok"))
        val currentCtx = new CurrentCtxNoop {
            override def get(): Option[Ctx] = ctx
        }
        val pCol0 = new PropagationCollector(currentCtx)
        pCol0.report(ctx)
        pCol0.report(ctx)
        pCol0.report(ctx)
        pCol0.checkCollected()
    }
}

class PropagationCollector(currentCtx: CurrentCtx) {
    case class Entry(expl: Option[Ctx], impl: Option[Ctx], stackTraceElement: StackTraceElement)
    val collected = new java.util.concurrent.ConcurrentLinkedQueue[Entry]()
    def report(expl: Option[Ctx]): Unit = {
        val stack = Thread.currentThread().getStackTrace
        collected.add(Entry(expl, currentCtx.get(), stack(2)))
    }

    def checkCollected(): Unit = {
        var e = collected.poll()
        while (e != null) {
            //Console.println(e)
            assert(e.expl === e.impl, "| mismatch at: " + e.stackTraceElement)
            e = collected.poll()
        }
    }
}

case class Ctx0(id: String) extends Ctx
class CtxFactory0 extends CtxFactory {
    val count = new AtomicInteger(0)

    def newCtx(name: String)(implicit parent: Option[Ctx] = None): Ctx = {
        new Ctx0(parent.map(_ + "/").getOrElse("") + name + "[" + count.addAndGet(1) + "]")
    }

    def startCtx(ctx: Ctx): Ctx = {
        ctx
    }

    def finishCtx(ctx: Ctx): Ctx = {
        ctx
    }
}
        //----------------------------------------------------------------------------------------------------------------------
// Fake Application Fragment

class FakeService(ctxTools: CtxTools, pcol: PropagationCollector) {
    def doJobNoReturn()(implicit ctx: Option[Ctx] = None): Unit = {
        pcol.report(ctx)
    }

    def doJobIdentity[T](input: T)(implicit ctx: Option[Ctx] = None): T = {
        pcol.report(ctx)
        input
    }
    def doJobInNewCtx[T](input: T)(implicit ctx: Option[Ctx] = None): T = {
        ctxTools.withCtx(ctxTools.newCtx("doJobInNewCtx")) { implicit ctx =>
            doJobIdentity(input)
        }
    }
    def doJobAsync[T](input: T)(implicit ctx: Option[Ctx] = None): Future[String] = {
        pcol.report(ctx)
        Future{
            doJobIdentity("doJobAsync")
        }
    }
    def doJobAsyncInNewCtx[T](input: T)(implicit ctx: Option[Ctx] = None): Future[String] = {
        ctxTools.withCtx(ctxTools.newCtx("doJobAsyncInNewCtx")) { implicit ctx =>
            pcol.report(ctx)
            Future{
                doJobIdentity("doJobAsyncInNewCtx")
            }
        }
    }
    def doJobAsyncSameT[T](input: T)(implicit ctx: Option[Ctx] = None): Future[String] = {
        pcol.report(ctx)
        Future.successful(
            doJobIdentity("doJobAsyncSameT")
        )
    }
    def doJobAsyncSameTInNewCtx[T](input: T)(implicit ctx: Option[Ctx] = None): Future[String] = {
        ctxTools.withCtx(ctxTools.newCtx("doJobAsyncSameTInNewCtx")) { implicit ctx =>
            pcol.report(ctx)
            Future.successful("doJobAsyncInNewCtx")
        }
    }
    def doJobAsyncFailedSameT[T](input: T)(implicit ctx: Option[Ctx] = None): Future[T] = {
        pcol.report(ctx)
        Future.failed(new Exception("sample error"))
    }
    def doJobAsyncFailedSameTInNewCtx[T](input: T)(implicit ctx: Option[Ctx] = None): Future[T] = {
        ctxTools.withCtx(ctxTools.newCtx("doJobAsyncFailedSameTInNewCtx")) { implicit ctx =>
            pcol.report(ctx)
            Future.failed(new Exception("sample error"))
        }
    }
}

case class MyMessage(txt: String, cnt: Int)(implicit val ctx: Option[Ctx] = None)
class FakeActor(ctxTools: CtxTools, pcol: PropagationCollector) extends Actor {
    override def receive = {
        case m: MyMessage =>
            implicit val ctx = m.ctx
            pcol.report(m.ctx)
            if (m.cnt > 0) {
                if (m.cnt % 2 == 0) {
                    ctxTools.withCtx(ctxTools.newCtx("newMessage")) {implicit ctx =>
                        self ! MyMessage(m.txt + "x", m.cnt - 1)
                    }
                } else {
                    self ! MyMessage(m.txt + "x", m.cnt - 1)
                }
            }
    }

}


