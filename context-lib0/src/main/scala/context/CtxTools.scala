package context

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.Future

/**
  * Created by davidb on 14/02/17.
  */
class CtxTools(currentCtx: CurrentCtx) {
    val count = new AtomicInteger(0)

    def newCtx(name: String)(implicit parent: Option[Ctx] = None): Ctx = {
        new Ctx(parent.map(_ + "/" ).getOrElse("") + name + "[" + count.addAndGet(1) + "]")
    }

    def startCtx(ctx: Ctx): Ctx = {
        ctx
    }
    def finishCtx(ctx: Ctx): Ctx = {
        ctx
    }

    /*
     * start + set current -> "schedule" f + finish on complete + restore previous current
     */
    def withAsyncCtx[T](ctx: Ctx)(f: Option[Ctx] => Future[T]): Future[T] = {
        val startedCtx = startCtx(ctx)
        currentCtx.withCurrentCtx(Option(startedCtx)) { implicit oStartedCtx =>
            val res = f(oStartedCtx)
            res.onComplete(_ => finishCtx(startedCtx))(SameThreadExecutionContext) //TODO use same Thread / ExecutionContext
            res
        }
    }
    def withCtx[T](ctx: Ctx)(f: Option[Ctx] => T): T = {
        val startedCtx = startCtx(ctx)
        currentCtx.withCurrentCtx(Option(startedCtx)) { implicit oStartedCtx =>
            try {
                f(oStartedCtx)
            } finally {
                finishCtx(startedCtx)
            }
        }
    }
}
