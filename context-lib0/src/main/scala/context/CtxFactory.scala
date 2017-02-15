package context

trait CtxFactory {
    def newCtx(name: String)(implicit parent: Option[Ctx] = None): Ctx
    def startCtx(ctx: Ctx): Ctx
    def finishCtx(ctx: Ctx): Ctx
}
