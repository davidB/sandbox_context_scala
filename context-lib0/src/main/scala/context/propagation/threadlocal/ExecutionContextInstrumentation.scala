package context.propagation.threadlocal

import context.CurrentCtx
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.{Around, Aspect}

/**
  * Created by davidb on 14/02/17.
  */
@Aspect
class ExecutionContextInstrumentation {
    @Around("execution (* scala.concurrent.ExecutionContext.execute(Runnable)) && args(runnable)")
    def aroundExecutionContextExecute(pjp: ProceedingJoinPoint, runnable: Runnable): Unit = {
        val ctx = CurrentCtx.get()
        val nRunnable = new Runnable {
            def run(): Unit ={
                CurrentCtx.withCurrentCtx(ctx){ _ =>
                    runnable.run()
                }
            }
        }
        pjp.proceed(Array(nRunnable))
    }
//    @Around("execution (* scala.concurrent.ExecutionContext.prepare())")
//    def aroundExecutionContextPrepare(pjp: ProceedingJoinPoint): ExecutionContext = {
//        val ctx = CurrentCtx.default.get()
//        //Console.println("aroundExecutionContextPrepare: " + pjp + ".." + ctx)
//        val ec = pjp.proceed().asInstanceOf[ExecutionContext]
//        return new WrappedExecutionContext(ec)(ctx)
//    }
}


//class WrappedExecutionContext(delegate: ExecutionContext)(ctx0: Option[Ctx]) extends ExecutionContext {
//    override def execute(runnable: Runnable): Unit = delegate.execute(new Runnable {
//        def run(): Unit = {
//            //System.out.println(("use ctx " + ctx0))
//            CurrentCtx.default.withCurrentCtx(ctx0){ _ =>
//                runnable.run()
//            }
//        }
//    })
//    override def reportFailure(t: Throwable): Unit = delegate.reportFailure(t)
//
//    override def prepare(): ExecutionContext = this
//}
