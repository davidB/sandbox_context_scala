package context.propagation.threadlocal

import context.CurrentCtx
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.{Around, Aspect}

/**
  * Created by davidb on 14/02/17.
  */
@Aspect
class CurrentCtxInstrumentation {
    @Around("execution(* context.CurrentCtx.instance())")
    def aroundCurrentCtx(pjp: ProceedingJoinPoint): CurrentCtx = CurrentCtxLocalThread.instance
}
