package context.di

import javax.inject.Singleton

import com.google.inject.{AbstractModule, Provides}
import context.propagation.threadlocal.CurrentCtxLocalThread
import context.{CtxFactory, CtxFactorySpanOnly, CurrentCtx}

class CtxBasicModule extends AbstractModule {
    def configure() = {
        bind(classOf[CtxFactory]).to(classOf[CtxFactorySpanOnly]).asEagerSingleton()
    }

    @Provides
    @Singleton
    def currentCtx(): CurrentCtx = {
        CurrentCtx.instance = CurrentCtxLocalThread.instance
        CurrentCtx.instance
    }

}
