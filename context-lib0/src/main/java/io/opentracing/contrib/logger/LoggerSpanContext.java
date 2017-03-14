package io.opentracing.contrib.logger;

import io.opentracing.SpanContext;

import java.util.Map;

class LoggerSpanContext implements SpanContext {
    private final SpanContext wrapped;
    public final String spanId;

    LoggerSpanContext(SpanContext wrapped, String spanId) {
        this.wrapped = wrapped;
        this.spanId = spanId;
    }

    @Override
    public Iterable<Map.Entry<String, String>> baggageItems() {
        return wrapped.baggageItems();
    }
}
