package catchup.service.api

import catchup.service.api.Mark.MarkType
import catchup.service.db.CatchUpDbItem

fun CatchUpItem.toCatchUpDbItem(): CatchUpDbItem {
  return CatchUpDbItem(
    id = id,
    title = title,
    description = description,
    timestamp = timestamp,
    scoreChar = score?.first,
    score = score?.second,
    tag = tag,
    tagHintColor = tagHintColor,
    author = author,
    source = source,
    itemClickUrl = itemClickUrl,
    detailKey = detailKey,
    serviceId = serviceId,
    indexInResponse = indexInResponse,
    contentType = contentType?.name,
    imageUrl = imageInfo?.url,
    imageDetailUrl = imageInfo?.detailUrl,
    imageAnimatable = imageInfo?.animatable,
    imageSourceUrl = imageInfo?.sourceUrl,
    imageBestSizeX = imageInfo?.bestSize?.first,
    imageBestSizeY = imageInfo?.bestSize?.second,
    imageAspectRatio = imageInfo?.aspectRatio,
    imageImageId = imageInfo?.imageId,
    imageColor = imageInfo?.color,
    imageBlurHash = imageInfo?.blurHash,
    markText = mark?.text,
    markTextPrefix = mark?.textPrefix,
    markType = mark?.markType?.name,
    markClickUrl = mark?._markClickUrl,
    markIconTintColor = mark?.iconTintColor,
    markFormatTextAsCount = mark?.formatTextAsCount,
  )
}

fun CatchUpDbItem.toCatchUpItem(): CatchUpItem {
  return CatchUpItem(
    id = id,
    title = title,
    description = description,
    timestamp = timestamp,
    score = scoreChar?.let { Pair(it, score ?: 0) },
    tag = tag,
    tagHintColor = tagHintColor,
    author = author,
    source = source,
    itemClickUrl = itemClickUrl,
    detailKey = detailKey,
    serviceId = serviceId,
    indexInResponse = indexInResponse,
    contentType = contentType?.let { ContentType.valueOf(it) },
    imageInfo =
      imageUrl?.let {
        ImageInfo(
          url = it,
          detailUrl = checkNotNull(imageDetailUrl) { "imageDetailUrl was null" },
          animatable = checkNotNull(imageAnimatable) { "imageAnimatable was null" },
          sourceUrl = checkNotNull(imageSourceUrl) { "imageSourceUrl was null" },
          bestSize = imageBestSizeX?.let { Pair(it, imageBestSizeY ?: 0) },
          aspectRatio = checkNotNull(imageAspectRatio) { "imageAspectRatio was null" },
          imageId = checkNotNull(imageImageId) { "imageImageId was null" },
          color = imageColor,
          blurHash = imageBlurHash,
        )
      },
    mark =
      markType?.let {
        Mark(
          text = markText,
          textPrefix = markTextPrefix,
          markType = MarkType.valueOf(it),
          _markClickUrl = markClickUrl,
          iconTintColor = markIconTintColor,
          formatTextAsCount =
            checkNotNull(markFormatTextAsCount) { "markFormatTextAsCount was null" },
        )
      },
  )
}
