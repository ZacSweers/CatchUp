package io.sweers.catchup.network;

import java.util.List;

import io.sweers.catchup.model.reddit.RedditListing;
import io.sweers.catchup.model.reddit.RedditResponse;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

public interface RedditService {
  @GET("r/{subreddit}/comments/{id}.json") Observable<List<RedditResponse<RedditListing>>> comments(
      @Path("subreddit") String subreddit,
      @Path("id") String id
  );

  @GET("r/{subreddit}.json") Observable<RedditResponse<RedditListing>> subreddit(
      @Path("subreddit") String subreddit,
      @Query("after") String after,
      @Query("limit") int limit);

  @GET("top.json") Observable<RedditResponse<RedditListing>> top(
      @Query("after") String after,
      @Query("limit") int limit
  );
}
