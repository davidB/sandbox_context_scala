package io.opentracing.contrib.logger;

import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerTracer  implements Tracer {
    final Tracer wrapped;
    final Logger logger;

    /**
     * @param wrapped the backend tracer, if you don't want a wrapped tracer use NoopTracerFactory.create()
     * @param logger the logger to use (eg LoggerFactory.getLogger("tracer"))
     */
    public LoggerTracer(Tracer wrapped, Logger logger) {
        this.wrapped = wrapped;
        this.logger = logger;
        System.out.println("init");
        LoggerFactory.getLogger(this.getClass()).info("{tracer: 'init'}");
    }

    @Override
    public SpanBuilder buildSpan(String s) {
        return new LoggerSpanBuilder(wrapped.buildSpan(s), logger, s);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C c) {
        wrapped.inject(spanContext, format, c);
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C c) {
        return wrapped.extract(format, c);
    }
}
