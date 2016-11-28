package io.sweers.catchup.data.github;

import io.reactivex.Single;
import io.sweers.catchup.data.github.model.Order;
import io.sweers.catchup.data.github.model.SearchQuery;
import io.sweers.catchup.data.github.model.SearchRepositoriesResult;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GitHubService {
  String HOST = "api.github.com";
  String ENDPOINT = "https://" + HOST;

  @GET("/search/repositories")
  Single<SearchRepositoriesResult> searchRepositories(
      @Query("q") SearchQuery query,
      @Query("sort") String sort,
      @Query("order") Order order
  );

}
