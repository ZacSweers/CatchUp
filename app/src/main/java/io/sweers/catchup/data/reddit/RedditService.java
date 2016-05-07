package io.sweers.catchup.data.reddit;

import java.util.List;

import io.sweers.catchup.data.reddit.model.RedditResponse;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

public interface RedditService {
  String ENDPOINT = "https://www.reddit.com/";

  @GET("r/{subreddit}/comments/{id}.json") Observable<List<RedditResponse>> comments(
      @Path("subreddit") String subreddit,
      @Path("id") String id
  );

  @GET(".json") Observable<RedditResponse> frontPage(
      @Query("limit") int limit
  );

  @GET("r/{subreddit}.json") Observable<RedditResponse> subreddit(
      @Path("subreddit") String subreddit,
      @Query("after") String after,
      @Query("limit") int limit);

  @GET("top.json") Observable<RedditResponse> top(
      @Query("after") String after,
      @Query("limit") int limit
  );
}
