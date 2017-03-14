package di

import javax.inject.{Named, Singleton}

import com.google.inject.{AbstractModule, Provides}
import io.opentracing.{NoopTracerFactory, Tracer}

class NoopTracerModule extends AbstractModule {
  def configure() = {
  }

  @Provides
  @Singleton
  @Named("backend")
  def tracer(): Tracer = {
    return NoopTracerFactory.create()
  }
}

