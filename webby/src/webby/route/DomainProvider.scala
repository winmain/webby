package webby.route

trait DomainProvider[A] {
  def toDomain(info: A): Option[String]
  def fromDomain(domain: String): Option[A]
  override def toString: String = getClass.getSimpleName
}

object EmptyDomainProvider extends DomainProvider[Any] {
  override def toDomain(info: Any): Option[String] = None
  override def fromDomain(domain: String): Option[Any] = Some(())
}

/**
 * DomainProvider, работающий с одним конкретным доменом
 */
class OneDomainProvider(domain: String) extends DomainProvider[Any] {
  override def toDomain(info: Any) = Some(domain)
  override def fromDomain(d: String): Option[Any] = if (domain == d) Some(()) else None
  override def toString: String = "OneDomainProvider: " + domain
}
