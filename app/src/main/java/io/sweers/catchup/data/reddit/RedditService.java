package io.sweers.catchup.data.reddit;

import io.reactivex.Single;
import io.sweers.catchup.data.reddit.model.RedditResponse;
import java.util.List;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

public interface RedditService {
  String HOST = "www.reddit.com";
  String ENDPOINT = "https://" + HOST;

  @GET("/r/{subreddit}/comments/{id}")
  Observable<List<RedditResponse>> comments(
      @Path("subreddit") String subreddit,
      @Path("id") String id
  );

  @GET("/")
  Single<RedditResponse> frontPage(
      @Query("limit") int limit
  );

  @GET("/r/{subreddit}")
  Observable<RedditResponse> subreddit(
      @Path("subreddit") String subreddit,
      @Query("after") String after,
      @Query("limit") int limit);

  @GET("/top")
  Observable<RedditResponse> top(
      @Query("after") String after,
      @Query("limit") int limit
  );
}
