package context

import java.util.UUID

import io.opentracing.Span

trait Ctx {
    def id: String
    def span: Span
}

case class CtxSpanOnly(span: Span, id: String = UUID.randomUUID().toString) extends Ctx {
    span.setBaggageItem("ctx.id", id)
}
