package io.sweers.catchup.data.hackernews;

import java.util.List;

import io.sweers.catchup.data.hackernews.model.HackerNewsStory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;

public interface HackerNewsService {

  String ENDPOINT = "https://hacker-news.firebaseio.com/v0/";

  @GET("item/{id}.json")
  Observable<HackerNewsStory> getItem(@Path("id") String id);

  @GET("topstories.json")
  Observable<List<String>> topStories();

}
