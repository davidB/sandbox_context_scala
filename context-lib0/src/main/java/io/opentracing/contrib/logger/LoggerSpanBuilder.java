package io.opentracing.contrib.logger;

import io.opentracing.References;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class LoggerSpanBuilder implements Tracer.SpanBuilder {
    private static final String BAGGAGE_SPANID_KEY = "logger.spanId";
    private Tracer.SpanBuilder wrapped;
    private Logger logger;
    private final Map<String, Object> tags = new LinkedHashMap<>();
    private final Map<String, String> references = new LinkedHashMap<>();
    private String operationName;

    LoggerSpanBuilder(Tracer.SpanBuilder wrapped, Logger logger, String operationName){
        this.wrapped = wrapped;
        this.logger = logger;
        this.operationName = operationName;
    }

    String findSpanId(SpanContext context) {
        if (context instanceof LoggerSpanContext) {
            return ((LoggerSpanContext) context).spanId;
        }
        for (Map.Entry<String,?> kv: context.baggageItems()) {
            if (BAGGAGE_SPANID_KEY.equals(kv.getKey())) {
                return kv.getValue().toString();
            }
        }
        return "";
    }

    @Override
    public Tracer.SpanBuilder asChildOf(SpanContext spanContext) {
        wrapped = wrapped.asChildOf(spanContext);
        references.put(References.CHILD_OF, findSpanId(spanContext));
        return this;
    }

    @Override
    public Tracer.SpanBuilder asChildOf(Span span) {
        wrapped = wrapped.asChildOf(span);
        references.put(References.CHILD_OF, findSpanId(span.context()));
        return this;
    }

    @Override
    public Tracer.SpanBuilder addReference(String s, SpanContext spanContext) {
        wrapped = wrapped.addReference(s, spanContext);
        references.put(s, findSpanId(spanContext));
        return this;
    }

    @Override
    public Tracer.SpanBuilder withTag(String s, String s1) {
        wrapped = wrapped.withTag(s, s1);
        tags.put(s, s1);
        return this;
    }

    @Override
    public Tracer.SpanBuilder withTag(String s, boolean b) {
        wrapped = wrapped.withTag(s, b);
        tags.put(s, b);
        return this;
    }

    @Override
    public Tracer.SpanBuilder withTag(String s, Number number) {
        wrapped = wrapped.withTag(s, number);
        tags.put(s, number);
        return this;
    }

    @Override
    public Tracer.SpanBuilder withStartTimestamp(long l) {
        wrapped = wrapped.withStartTimestamp(l);
        return this;
    }

    @Override
    public Span start() {
        Span wspan = wrapped.start();
        String spanId = UUID.randomUUID().toString();
        wspan.setBaggageItem(BAGGAGE_SPANID_KEY, spanId);
        Span span = new LoggerSpan(wspan, logger, spanId, operationName, tags, references);
        return span;
    }

    @Override
    public Iterable<Map.Entry<String, String>> baggageItems() {
        return wrapped.baggageItems();
    }
}
