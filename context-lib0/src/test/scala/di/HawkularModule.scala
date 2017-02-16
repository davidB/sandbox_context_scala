package di

import javax.inject.Singleton

import com.google.inject.{AbstractModule, Provides}
import io.opentracing.Tracer
import org.hawkular.apm.api.utils.PropertyUtil
import org.hawkular.apm.client.api.recorder.BatchTraceRecorder.BatchTraceRecorderBuilder
import org.hawkular.apm.client.api.recorder.TraceRecorder
import org.hawkular.apm.client.api.recorder.BatchTraceRecorder
import org.hawkular.apm.trace.publisher.rest.client.TracePublisherRESTClient

class HawkularModule extends AbstractModule {
    def configure() = {
    }

    //Hawkular seems to failed to load TracePublisher via ServiceLoader, so Made a explicit
    @Provides
    @Singleton
    def traceRecorder(): TraceRecorder = {
        val publisher = new TracePublisherRESTClient(
            PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_USERNAME, "jdoe"),
            PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_PASSWORD, "password"),
            PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_URI, "http://localhost:8080")
        )
        val builder = new BatchTraceRecorderBuilder()
        builder.withTracePublisher(publisher)
        Option(PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_COLLECTOR_BATCHSIZE)).foreach{ batchSize =>
            builder.withBatchSize(Integer.parseInt(batchSize))
        }
        Option(PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_COLLECTOR_BATCHTIME)).foreach{ batchTime =>
            builder.withBatchTime(Integer.parseInt(batchTime))
        }
        Option(PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_COLLECTOR_BATCHTHREADS)).foreach{ threadPoolSize =>
            builder.withBatchPoolSize(Integer.parseInt(threadPoolSize))
        }
        builder.withTenantId(PropertyUtil.getProperty("HAWKULAR_APM_TENANTID"))
        new BatchTraceRecorder(builder)
    }


    @Provides
    @Singleton
    def tracer(traceRecorder: TraceRecorder): Tracer = {
        val tracer0 = new org.hawkular.apm.client.opentracing.APMTracer(traceRecorder)
        //GlobalTracer.register(tracer0)
        //GlobalTracer.get()
        tracer0
    }

}
