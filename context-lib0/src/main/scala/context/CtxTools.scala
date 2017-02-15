package context

import scala.concurrent.Future

/**
  * Created by davidb on 14/02/17.
  */
class CtxTools(val current: CurrentCtx, val factory: CtxFactory) {

    def newCtx(name: String)(implicit parent: Option[Ctx] = None): Ctx = factory.newCtx(name)
    def startCtx(ctx: Ctx): Ctx = factory.startCtx(ctx)
    def finishCtx(ctx: Ctx): Ctx = factory.finishCtx(ctx)

    /*
     * start + set current -> "schedule" f + finish on complete + restore previous current
     */
    def withAsyncCtx[T](ctx: Ctx)(f: Option[Ctx] => Future[T]): Future[T] = {
        val startedCtx = startCtx(ctx)
        current.withCurrentCtx(Option(startedCtx)) { implicit oStartedCtx =>
            val res = f(oStartedCtx)
            res.onComplete(_ => finishCtx(startedCtx))(SameThreadExecutionContext) //TODO use same Thread / ExecutionContext
            res
        }
    }
    def withCtx[T](ctx: Ctx)(f: Option[Ctx] => T): T = {
        val startedCtx = startCtx(ctx)
        current.withCurrentCtx(Option(startedCtx)) { implicit oStartedCtx =>
            try {
                f(oStartedCtx)
            } finally {
                finishCtx(startedCtx)
            }
        }
    }
}
