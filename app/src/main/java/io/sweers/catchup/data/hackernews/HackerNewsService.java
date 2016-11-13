package io.sweers.catchup.data.hackernews;

import java.util.List;

import io.reactivex.Observable;
import io.sweers.catchup.data.hackernews.model.HackerNewsStory;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface HackerNewsService {

  String HOST = "hacker-news.firebaseio.com";
  String ENDPOINT = "https://" + HOST;

  @GET("/v0/item/{id}")
  Observable<HackerNewsStory> getItem(@Path("id") String id);

  @GET("/v0/topstories")
  Observable<List<String>> topStories();

}
