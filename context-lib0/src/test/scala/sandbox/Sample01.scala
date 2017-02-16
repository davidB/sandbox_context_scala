package sandbox

import java.util

import com.google.inject.Guice
import context.CtxTools
import context.di.CtxBasicModule
import di.{BraveModule, HawkularModule}
import io.opentracing.Tracer
import io.opentracing.propagation.{Format, TextMapInjectAdapter}
import io.opentracing.tag.Tags

/**
  * Created by davidb on 15/02/17.
  */
object Sample01 {
    def main(args: Array[String]): Unit = {
        //val injector = Guice.createInjector(new BraveModule(), new CtxBasicModule())
        val injector = Guice.createInjector(new HawkularModule(), new CtxBasicModule())
        System.out.print("\n only OpenTracing api\n")
        val tracer = injector.getInstance(classOf[Tracer])
        (0 until 5).foreach { _ =>
            run0(tracer)
        }
        System.out.print("\n use CtxTools api\n")
        val ctxTools = injector.getInstance(classOf[CtxTools])
        (0 until 5).foreach { _ =>
            run1(ctxTools)
        }
        System.out.print("\n waiting 2s to give time to reporter\n")
        Thread.sleep(2000)
        System.out.print("\n end\n")
    }

    def run0(tracer: Tracer): Unit = {
        // start a span
        val span0 = tracer.buildSpan("POST")
                .withTag("description", "top level initial span in the original process")
                .start()
        Tags.HTTP_URL.set(span0, "/orders") //span.setTag(Tags.HTTP_URL.getKey(), "/orders")
        Tags.HTTP_METHOD.set(span0, "POST")
        Tags.PEER_SERVICE.set(span0, "OrderManager")
        Tags.SPAN_KIND.set(span0, Tags.SPAN_KIND_SERVER)
        try {

            val span1 = tracer.buildSpan("span-1")
                    .asChildOf(span0)
                    .withTag("description", "the first inner span in the original process")
                    .start()
            try {

                // do something

                // start another span

                val span2 = tracer.buildSpan("span-2")
                        .asChildOf(span1)
                        .withTag("description", "the second inner span in the original process")
                        .start()
                        try {

                    // do something

                    // cross process boundary
                    val map = new util.HashMap[String, String]()
                    //tracer.inject(span2.context(), Format.Builtin.HTTP_HEADERS, new TextMapInjectAdapter(map))
                    System.out.print(".")
                    // request.addHeaders(map);
                    // request.doGet();
                } finally {span2.finish()}
            } finally{span1.finish()}
        } finally {span0.finish()}
    }

    def run1(ctxTools: CtxTools): Unit = {
        // start a span
        ctxTools.withCtx(ctxTools.newCtx("span-0")) { implicit  ctx =>
            ctx.foreach{ x =>
                x.span.setTag("description", "top level initial span in the original process")
                Tags.HTTP_URL.set(x.span, "/orders")
                Tags.HTTP_METHOD.set(x.span, "POST")
                Tags.PEER_SERVICE.set(x.span, "OrderManager")
                Tags.SPAN_KIND.set(x.span, Tags.SPAN_KIND_SERVER)
            }
            ctxTools.withCtx(ctxTools.newCtx("span-1")) {implicit  ctx =>
                ctx.foreach(_.span.setTag("description", "the first inner span in the original process"))
                // do something
                // start another span
                ctxTools.withCtx(ctxTools.newCtx("span-2")) { implicit ctx =>
                    ctx.foreach(_.span.setTag("description", "the second inner span in the original process"))
                    // do something
                    // cross process boundary
                    val map = new util.HashMap[String, String]()
                    //TODO tracer.inject(span2.context(), Format.Builtin.HTTP_HEADERS, new TextMapInjectAdapter(map))
                    // request.addHeaders(map);
                    // request.doGet();
                    System.out.print(".")
                }
            }
        }
    }
}
