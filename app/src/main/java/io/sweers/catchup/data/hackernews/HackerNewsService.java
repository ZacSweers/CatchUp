package io.sweers.catchup.data.hackernews;

import io.reactivex.Single;
import io.sweers.catchup.data.hackernews.model.HackerNewsStory;
import java.util.List;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface HackerNewsService {

  String HOST = "hacker-news.firebaseio.com";
  String ENDPOINT = "https://" + HOST;

  @GET("/v0/item/{id}") Single<HackerNewsStory> getItem(@Path("id") String id);

  @GET("/v0/topstories") Single<List<String>> topStories();
}
