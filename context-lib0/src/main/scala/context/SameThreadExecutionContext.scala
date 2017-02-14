package context

import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

/**
  * For small code blocks that don't need to be run on a separate thread.
  */
object SameThreadExecutionContext extends ExecutionContext {
    val logger = LoggerFactory.getLogger(this.getClass)
    override def execute(runnable: Runnable): Unit = runnable.run
    override def reportFailure(t: Throwable): Unit = logger.error(t.getMessage, t)
}
