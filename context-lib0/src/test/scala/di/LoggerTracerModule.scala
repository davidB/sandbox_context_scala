package di

import javax.inject.{Named, Singleton}

import com.google.inject.{AbstractModule, Provides}
import io.opentracing.Tracer
import io.opentracing.contrib.logger.LoggerTracer
import org.slf4j.LoggerFactory

class LoggerTracerModule extends AbstractModule {
  def configure() = {
  }

  @Provides
  @Singleton
  def tracer(@Named("backend") tracer: Tracer): Tracer = {
    return new LoggerTracer(tracer, LoggerFactory.getLogger("tracer"))
  }
}

