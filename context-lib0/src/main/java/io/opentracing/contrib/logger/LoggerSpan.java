package io.opentracing.contrib.logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import org.slf4j.Logger;

import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

public class LoggerSpan implements Span {
    private Span wrapped;
    private final Logger logger;
    public final String spanId;
    public String operationName;
    public long startAt;
    public final Map<String, Object> tags;
    public final Map<String, String> references;
    final JsonFactory f = new JsonFactory();

    public LoggerSpan(Span wrapped, Logger logger, String spanId, String operationName, Map<String,Object> tags, Map<String, String> references) {
        this.logger = logger;
        this.spanId = spanId;
        this.wrapped = wrapped;
        this.operationName = operationName;
        this.tags = tags;
        this.references = references;
        startAt = now();
        if (logger.isTraceEnabled()) {
            logger.trace(toStructuredMessage("start", startAt, null));
        }
    }

    protected long now(){
        return System.nanoTime() / 1000l;
    }

    protected String toStructuredMessage(String action, long timestampMicroseconds, Map<String,?> logEvent){
        //return "" + (timestampMicroseconds - startAt);

        try {
            StringWriter w = new StringWriter();
            JsonGenerator g = f.createGenerator(w);

            g.writeStartObject();
            g.writeNumberField("ts", now());
            g.writeNumberField("elapsed", timestampMicroseconds - startAt);
            g.writeStringField("spanId", spanId);
            g.writeStringField("action", action);
            g.writeStringField("operation", operationName);
            g.writeObjectFieldStart("tags");
            for(Map.Entry<String,Object> kv : tags.entrySet()){
                Object v = kv.getValue();
                if (v instanceof String) {
                    g.writeStringField(kv.getKey(), (String)v);
                } else if (v instanceof Number) {
                    g.writeNumberField(kv.getKey(), ((Number) v).doubleValue());
                } else if (v instanceof Boolean) {
                    g.writeBooleanField(kv.getKey(), (Boolean) v);
                }
            }
            g.writeEndObject();
            if (logEvent != null && !logEvent.isEmpty()){
                g.writeObjectFieldStart("event");
                for(Map.Entry<String,?> kv : logEvent.entrySet()){
                    Object v = kv.getValue();
                    if (v instanceof String) {
                        g.writeStringField(kv.getKey(), (String)v);
                    } else if (v instanceof Number) {
                        g.writeNumberField(kv.getKey(), ((Number) v).doubleValue());
                    } else if (v instanceof Boolean) {
                        g.writeBooleanField(kv.getKey(), (Boolean) v);
                    }
                }
                g.writeEndObject();
            } else {
                g.writeObjectFieldStart("baggage");
                for(Map.Entry<String,String> kv : context().baggageItems()){
                    g.writeStringField(kv.getKey(), kv.getValue());
                }
                g.writeEndObject();
                g.writeObjectFieldStart("references");
                for(Map.Entry<String,String> kv : references.entrySet()){
                    g.writeStringField(kv.getKey(), kv.getValue());
                }
                g.writeEndObject();
            }

            g.writeEndObject();
            g.close();
            w.close();
            return w.toString();
        } catch(Exception exc) {
            exc.printStackTrace();
        }
        return "";
    }

    protected void toLogger(long l, Map<String, ?> map) {
        LogLevel level = LogLevel.INFO;
        try {
            level = (LogLevel) map.get("logLevel");
        } catch (Exception ignore) {
        }
        switch (level) {
            case TRACE:
                if (logger.isTraceEnabled()) {
                    logger.trace(toStructuredMessage("log", l, map));
                }
                break;
            case DEBUG:
                if (logger.isDebugEnabled()) {
                    logger.debug(toStructuredMessage("log", l, map));
                }
                break;
            case WARN:
                if (logger.isWarnEnabled()) {
                    logger.warn(toStructuredMessage("log", l, map));
                }
                break;
            case ERROR:
                if (logger.isErrorEnabled()) {
                    logger.error(toStructuredMessage("log", l, map));
                }
                break;
            default:
                if (logger.isInfoEnabled()) {
                    logger.info(toStructuredMessage("log", l, map));
                }
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    // Span

    @Override
    public SpanContext context() {
        return new LoggerSpanContext(wrapped.context(), spanId);
    }

    @Override
    public void finish() {
        wrapped.finish();
        if (logger.isTraceEnabled()) {
            logger.trace(toStructuredMessage("finish", now(), null));
        }
    }

    @Override
    public void finish(long l) {
        wrapped.finish(l);
        if (logger.isTraceEnabled()) {
            logger.trace(toStructuredMessage("finish", l, null));
        }
    }

    @Override
    public void close() {
        wrapped.close();
    }

    @Override
    public Span setTag(String s, String s1) {
        wrapped = wrapped.setTag(s, s1);
        tags.put(s, s1);
        return this;
    }

    @Override
    public Span setTag(String s, boolean b) {
        wrapped = wrapped.setTag(s, b);
        tags.put(s, b);
        return this;
    }

    @Override
    public Span setTag(String s, Number number) {
        wrapped = wrapped.setTag(s, number);
        tags.put(s, number);
        return this;
    }

    @Override
    public Span log(Map<String, ?> map) {
        wrapped = wrapped.log(map);
        toLogger(now(), map);
        return this;
    }

    @Override
    public Span log(long l, Map<String, ?> map) {
        wrapped = wrapped.log(l, map);
        toLogger(l, map);
        return this;
    }

    @Override
    public Span log(String event) {
        wrapped = wrapped.log(event);
        if (logger.isInfoEnabled()) {
            logger.info(toStructuredMessage("log", now(), Collections.singletonMap("event", event)));
        }
        return this;
    }

    @Override
    public Span log(long l, String event) {
        wrapped.log(l, event);
        toLogger(l, Collections.singletonMap("event", event));
        return this;
    }

    @Override
    public Span setBaggageItem(String s, String s1) {
        wrapped = wrapped.setBaggageItem(s, s1);
        return this;
    }

    @Override
    public String getBaggageItem(String s) {
        return wrapped.getBaggageItem(s);
    }

    @Override
    public Span setOperationName(String s) {
        wrapped = wrapped.setOperationName(s);
        operationName = s;
        return this;
    }

    @Override
    public Span log(String eventName, Object payload) {
        wrapped = wrapped.log(eventName, payload);
        toLogger(now(), Collections.singletonMap(eventName, payload));
        return this;
    }

    @Override
    public Span log(long l, String eventName, Object payload) {
        wrapped = wrapped.log(l, eventName, payload);
        toLogger(l, Collections.singletonMap(eventName, payload));
        return this;
    }
}
