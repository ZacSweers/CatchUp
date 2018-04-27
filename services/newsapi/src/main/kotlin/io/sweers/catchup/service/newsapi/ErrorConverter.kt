package io.sweers.catchup.service.newsapi

import io.reactivex.SingleSource
import io.reactivex.functions.Function
import io.sweers.catchup.service.newsapi.model.NewsApiResponse

/**
 * Basic error converter for use with onErrorResumeNext.
 */
internal interface ErrorConverter : Function<Throwable, SingleSource<NewsApiResponse>>
