package io.sweers.catchup.data;

import android.content.Context;
import android.support.v4.util.ArrayMap;

import java.io.IOException;

import io.sweers.catchup.P;
import io.sweers.catchup.data.designernews.DesignerNewsService;
import io.sweers.catchup.data.dribbble.DribbbleService;
import io.sweers.catchup.data.github.GitHubService;
import io.sweers.catchup.data.hackernews.HackerNewsService;
import io.sweers.catchup.data.medium.MediumService;
import io.sweers.catchup.data.model.ServiceData;
import io.sweers.catchup.data.producthunt.ProductHuntService;
import io.sweers.catchup.data.reddit.RedditService;
import io.sweers.catchup.data.slashdot.SlashdotService;
import io.sweers.catchup.injection.qualifiers.ApplicationContext;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Okio;

/**
 * An interceptor that rewrites the response with mocked data instead.
 */
public final class MockDataInterceptor implements Interceptor {

  // TODO Generate this?
  private static final ArrayMap<String, ServiceData> SUPPORTED_ENDPOINTS
      = new ArrayMap<String, ServiceData>() {{
    put(HackerNewsService.HOST,
        new ServiceData.Builder("hn")
            .addEndpoint("/v0/topstories")
            .addEndpoint("/v0/item/11665804")
            .build());
    put(RedditService.HOST,
        new ServiceData.Builder("r")
            .addEndpoint("/")
            .build());
    put(MediumService.HOST,
        new ServiceData.Builder("m")
            .addEndpoint("/browse/top")
            .build());
    put(ProductHuntService.HOST,
        new ServiceData.Builder("ph")
            .addEndpoint("/v1/posts")
            .build());
    put(SlashdotService.HOST,
        new ServiceData.Builder("sd")
            .addEndpoint("/Slashdot/slashdotMainatom")
            .fileType("xml")
            .build());
    put(DesignerNewsService.HOST,
        new ServiceData.Builder("dn")
            .addEndpoint("/api/v1/stories")
            .build());
    put(DribbbleService.HOST,
        new ServiceData.Builder("dr")
            .addEndpoint("/v1/shots")
            .build());
    put(GitHubService.HOST,
        new ServiceData.Builder("g")
            .addEndpoint("/search/repositories")
            .build());
  }};

  @ApplicationContext private Context context;

  public MockDataInterceptor(@ApplicationContext Context context) {
    this.context = context;
  }

  private static String formatUrl(ServiceData service, HttpUrl url) {
    String lastSegment = url.pathSegments().get(url.pathSize() - 1);
    if ("".equals(lastSegment)) {
      lastSegment = "nopath";
    }
    return service.assetsPrefix
        + "/"
        + lastSegment
        + "."
        + service.fileType;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    HttpUrl url = request.url();
    String host = url.host();
    String path = url.encodedPath();
    ServiceData serviceData = SUPPORTED_ENDPOINTS.get(host);
    if (P.debugMockModeEnabled.get()
        && serviceData != null
        && serviceData.supports(path)) {
      return new Response.Builder()
          .request(request)
          .body(ResponseBody.create(MediaType.parse("application/json"),
              Okio.buffer(
                  Okio.source(
                      context.getAssets().open(formatUrl(serviceData, url))))
                  .readUtf8()))
          .code(200)
          .protocol(Protocol.HTTP_1_1)
          .build();
    } else {
      return chain.proceed(request);
    }
  }
}
