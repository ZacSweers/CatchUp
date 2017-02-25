package io.sweers.catchup.data.smmry.model;

import io.sweers.catchup.BuildConfig;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SmmryRequestBuilder {

  public static SmmryRequestBuilder forUrl(String url) {
    SmmryRequestBuilder builder = new SmmryRequestBuilder();
    try {
      builder.url = URLEncoder.encode(url, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Invalid url - " + url);
    }
    return builder;
  }

  public static SmmryRequestBuilder forText() {
    return new SmmryRequestBuilder();
  }

  // the webpage to summarize
  private String url;

  // the number of sentences returned, default is 7
  private long sentenceCount = -1;

  // how many of the top keywords to return
  private long keywordCount = -1;

  // Whether or not to include quotations
  private boolean avoidQuote = false;
  private boolean avoidQuoteWasSet = false;

  // summary will contain string [BREAK] between each sentence
  private boolean withBreak = false;
  private boolean withBreakWasSet = false;

  private SmmryRequestBuilder() {
  }

  public SmmryRequestBuilder sentenceCount(long sentenceCount) {
    this.sentenceCount = sentenceCount;
    return this;
  }

  public SmmryRequestBuilder keywordCount(long keywordCount) {
    this.keywordCount = keywordCount;
    return this;
  }

  public SmmryRequestBuilder avoidQuote(boolean avoidQuote) {
    this.avoidQuote = avoidQuote;
    avoidQuoteWasSet = true;
    return this;
  }

  public SmmryRequestBuilder withBreak(boolean withBreak) {
    this.withBreak = withBreak;
    withBreakWasSet = true;
    return this;
  }

  public Map<String, Object> build() {
    Map<String, Object> map = new LinkedHashMap<>(6);
    map.put("SM_API_KEY", BuildConfig.SMMRY_API_KEY);
    if (sentenceCount != -1) {
      map.put("SM_LENGTH", sentenceCount);
    }
    if (keywordCount != -1) {
      map.put("SM_KEYWORD_COUNT", keywordCount);
    }
    if (avoidQuoteWasSet) {
      map.put("SM_QUOTE_AVOID", avoidQuote);
    }
    if (withBreakWasSet) {
      map.put("SM_WITH_BREAK", withBreak);
    }

    // This has to be last!
    if (url != null) {
      map.put("SM_URL", url);
    }
    return map;
  }
}
