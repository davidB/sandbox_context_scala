package context

import javax.inject.Inject

import io.opentracing.{References, Tracer}

trait CtxFactory {
    def newCtx(name: String)(implicit from: Option[Ctx] = None, fromKind: String = References.CHILD_OF): Ctx
    def startCtx(ctx: Ctx): Ctx
    def finishCtx(ctx: Ctx): Ctx
}


class CtxFactorySpanOnly @Inject()(tracer: Tracer) extends CtxFactory {
    def newCtx(name: String)(implicit from: Option[Ctx] = None, fromKind: String = References.CHILD_OF): Ctx = {
        val bs = tracer.buildSpan(name)
        from.foreach { fromCtx =>
            bs.addReference(fromKind, fromCtx.span.context())
        }
        val span = bs.start()
        CtxSpanOnly(span)
    }
    def startCtx(ctx: Ctx): Ctx = {
        ctx
    }
    def finishCtx(ctx: Ctx): Ctx = {
        ctx.span.finish()
        ctx
    }
}
