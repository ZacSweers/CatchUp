package io.sweers.catchup.model.reddit;

public class RedditResponse<T> {
  RedditResponse(T data) {
    this.data = data;
  }

  T data;

  public T getData() {
    return data;
  }
}
