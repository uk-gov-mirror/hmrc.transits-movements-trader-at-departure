package controllers.actions

import models.request.{AuthenticatedClientRequest}
import play.api.mvc.{ActionBuilder, AnyContent, DefaultActionBuilder}

import javax.inject.Inject

trait AuthenticatedClientIdActionProvider {
  def apply(): ActionBuilder[AuthenticatedClientRequest, AnyContent]
}

class AuthenticateClientIdActionProviderImpl @Inject()(
                                                authenticate: AuthenticateAction,
                                                authenticatedClientId: AuthenticatedClientIdAction,
                                                buildDefault: DefaultActionBuilder
                                              ) extends AuthenticatedClientIdActionProvider {

  override def apply(): ActionBuilder[AuthenticatedClientRequest, AnyContent] =
    buildDefault andThen authenticate andThen authenticatedClientId
}