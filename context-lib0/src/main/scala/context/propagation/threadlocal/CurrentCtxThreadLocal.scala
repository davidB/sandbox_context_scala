package context.propagation.threadlocal

import context.{Ctx, CurrentCtx}
import org.slf4j.MDC

object CurrentCtxLocalThread {
    //val default: CurrentCtx = new CurrentCtxNoop()
    val instance: CurrentCtx = new CurrentCtxLocalThread()
}

class CurrentCtxLocalThread extends CurrentCtx {
    private val currentCtx = new ThreadLocal[Ctx]()
    def get() : Option[Ctx] =  Option(currentCtx.get())
    def set(ctx: Option[Ctx]): Unit = {
        ctx match {
            case Some(c) => {
                currentCtx.set(c)
                MDC.put("currentCtx", c.id)
            }
            case None => {
                currentCtx.remove()
                MDC.remove("currentCtx")
            }
        }
    }
    def withCurrentCtx[T](ctx: Option[Ctx])(f: Option[Ctx] => T): T = {
        val previousCtx = get()
        try {
            set(ctx)
            f(ctx)
        } finally {
            set(previousCtx)
        }
    }
}
