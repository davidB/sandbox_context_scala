package di

import javax.inject.Singleton

import brave.opentracing.BraveTracer
import com.google.inject.{AbstractModule, Provides}
import io.opentracing.Tracer
import zipkin.reporter.AsyncReporter
import zipkin.reporter.okhttp3.OkHttpSender

class BraveModule extends AbstractModule {
    def configure() = {
    }

    @Provides
    @Singleton
    def tracer(): Tracer= {
        // Configure a reporter, which controls how often spans are sent
        //   (the dependency is io.zipkin.reporter:zipkin-sender-okhttp3)
        val sender = OkHttpSender.create("http://127.0.0.1:9411/api/v1/spans")
        val reporter = AsyncReporter.builder(sender).build()

        // Now, create a Brave tracer with the service name you want to see in Zipkin.
        //   (the dependency is io.zipkin.brave:brave)
        val braveTracer = brave.Tracer.newBuilder()
                .localServiceName("my-service")
                .reporter(reporter)
                .build()

        // Finally, wrap this with the OpenTracing Api
        val tracer = BraveTracer.wrap(braveTracer)
        //GlobalTracer.register(tracer)

        // You can later unwrap the underlying Brave Api as needed
        //val braveTracer = tracer.unwrap();
        return tracer
    }
}

