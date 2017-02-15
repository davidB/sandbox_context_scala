package context

object CurrentCtx extends CurrentCtx {
    var instance: CurrentCtx = CurrentCtxNoop.instance

    def get() : Option[Ctx] = instance.get()
    def set(ctx: Option[Ctx]): Unit = instance.set(ctx)
    def withCurrentCtx[T](ctx: Option[Ctx])(f: Option[Ctx] => T): T = instance.withCurrentCtx(ctx)(f)
}

trait CurrentCtx {
    def get() : Option[Ctx]
    def set(ctx: Option[Ctx]): Unit
    def withCurrentCtx[T](ctx: Option[Ctx])(f: Option[Ctx] => T): T
}

object CurrentCtxNoop {
    val instance = new CurrentCtxNoop()
}

class CurrentCtxNoop extends CurrentCtx {
    def get() : Option[Ctx] = None
    def set(ctx: Option[Ctx]): Unit = {}
    def withCurrentCtx[T](ctx: Option[Ctx])(f: Option[Ctx] => T): T = f(ctx)
}


