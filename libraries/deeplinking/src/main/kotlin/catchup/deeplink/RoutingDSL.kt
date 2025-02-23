package catchup.deeplink

import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableMap

@DslMarker annotation class RoutingDSLMarker

class RouteRequest(
  val path: String,
  val pathParams: ImmutableMap<String, String>,
  val queryParams: ImmutableMap<String, List<String?>>,
)

@RoutingDSLMarker
interface RoutBuilder {
  fun route(path: String = "", block: RoutBuilder.(RouteRequest) -> Screen): Screen

  fun route(path: Regex, block: RoutBuilder.(RouteRequest) -> Screen): Screen
}

@RoutingDSLMarker
class RoutingBuilder : RoutBuilder {
  class RoutBuilderImpl : RoutBuilder {
    override fun route(path: String, block: RoutBuilder.(RouteRequest) -> Screen): Screen {
      TODO()
    }

    override fun route(path: Regex, block: RoutBuilder.(RouteRequest) -> Screen): Screen {
      TODO("Not yet implemented")
    }
  }

  override fun route(path: String, block: RoutBuilder.(RouteRequest) -> Screen): Screen {
    TODO()
  }

  override fun route(path: Regex, block: RoutBuilder.(RouteRequest) -> Screen): Screen {
    TODO("Not yet implemented")
  }

  fun build(): Router {
    TODO()
  }
}

class RouterImplV2 {
  // TODO path params
  //  check PATH_TRAVERSAL from Retrofit
  //  /hello
  //  /order/shipment
  //  /user/{login}
  //  /user/*
  //  /user/{login?}
  //  /user/{...}
  //  /user/{param...}
  //  Regex("/.+/hello")
  //  (?<name>pattern)
  //  Regex("""(?<id>\d+)/hello""")
}

fun routing(block: RoutingBuilder.() -> Unit): Router {
  // TODO
  val builder = RoutingBuilder()
  block(builder)
  return builder.build()
}

fun example() {
  routing {
    route("/home") { request -> TODO() }
    route("/users/{id}") { request ->
      val id = request.pathParams["id"]
      TODO()
    }
  }
}
