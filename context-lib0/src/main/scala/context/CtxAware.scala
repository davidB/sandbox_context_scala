package context

/**
  * If a akka message implement
  */
trait CtxAware {
    def ctx: Option[Ctx]
}
