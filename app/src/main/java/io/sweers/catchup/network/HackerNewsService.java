package io.sweers.catchup.network;

import java.util.List;

import io.sweers.catchup.model.HackerNewsStory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;

public interface HackerNewsService {

  @GET("topstories.json") Observable<List<String>> topStories();

  @GET("item/{id}.json") Observable<HackerNewsStory> getItem(@Path("id") String id);

}
