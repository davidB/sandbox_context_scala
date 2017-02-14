package context.propagation.threadlocal

import akka.actor.ActorRef
import akka.dispatch.Envelope
import context.{Ctx, CtxAware, CurrentCtx}
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation._

@Aspect
class ActorEnvelopeInstrumentation {

    @Around("call(akka.dispatch.Envelope.new(java.lang.Object, akka.actor.ActorRef)) && args(msg, sender)")
    def aroundNewEnvelop(pjp: ProceedingJoinPoint, msg: Object, sender: ActorRef): Envelope = {
        //Console.println("new envelope: " + pjp)
        val msg2 = msg match {
            case msg: CtxAware =>
                msg
            case msg: AnyRef =>
                EnvelopeCtxAware(msg, CurrentCtx.get())
        }
        pjp.proceed(Array(msg2.asInstanceOf[AnyRef], sender)).asInstanceOf[Envelope]
    }

    @Around("execution (* akka.dispatch.Envelope.message())")
    def aroundExecutionContextExecute(pjp: ProceedingJoinPoint): Any = {
        //val ctx = CurrentCtx.default.get()
        val m = pjp.proceed()
        //System.out.println("message()" + m)
        m match {
            case EnvelopeCtxAware(msg, ctx) =>
                CurrentCtx.set(ctx)
                msg
            case msg: CtxAware =>
                CurrentCtx.set(msg.ctx)
                msg
            case msg => msg
        }
    }
}



private case class EnvelopeCtxAware(msg: Any, ctx: Option[Ctx]) extends CtxAware

