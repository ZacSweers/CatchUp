package catchup.service.slashdot

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.decodeFromString
import org.junit.Test

// The xmlutil library's API is extremely annoying to get right
class FeedTest {

  @Test
  fun parseSmokeTest() {
    val parsedFeed = SlashdotModule.provideXml().decodeFromString<Feed>(FEED_STRING)
    assertThat(parsedFeed.entries).hasSize(1)
  }
}

// language=xml
private val FEED_STRING =
  """
    <?xml version="1.0" encoding="ISO-8859-1"?>

    <feed
     xmlns:content="http://purl.org/rss/1.0/modules/content/"
     xmlns:slash="http://purl.org/rss/1.0/modules/slash/"
     xmlns:syn="http://purl.org/rss/1.0/modules/syndication/"
     xmlns="http://www.w3.org/2005/Atom"
     xml:lang="en-us"
    >

    <title>Slashdot</title>
    <id>https://slashdot.org/</id>
    <link href="https://slashdot.org/"/>
    <link href="http://rss.slashdot.org/Slashdot/slashdotatom" rel="self"/>
    <link href="http://pubsubhubbub.appspot.com/" rel="hub"/>
    <subtitle>News for nerds, stuff that matters</subtitle>
    <rights>Copyright 1997-2016, SlashdotMedia. All Rights Reserved.</rights>
    <updated>2023-07-01T03:49:20+00:00</updated>
    <author>
     <name>Dice</name>
     <email>help@slashdot.org</email>
    </author>
    <category term="Technology"/>
    <syn:updatePeriod>hourly</syn:updatePeriod>
    <syn:updateFrequency>1</syn:updateFrequency>
    <syn:updateBase>1970-01-01T00:00+00:00</syn:updateBase>
    <logo>https://a.fsdn.com/sd/topics/topicslashdot.gif</logo>

    <entry>
    <id>https://hardware.slashdot.org/story/23/06/30/2310233/would-you-leave-grandma-with-a-companion-robot?utm_source=atom1.0mainlinkanon&amp;utm_medium=feed</id>
    <title>Would You Leave Grandma With a Companion Robot?</title>
    <link href="https://hardware.slashdot.org/story/23/06/30/2310233/would-you-leave-grandma-with-a-companion-robot?utm_source=atom1.0mainlinkanon&amp;utm_medium=feed"/>
    <summary type="html">An anonymous reader quotes a report from OPB: Out near the far end of Washington's Long Beach Peninsula, 83-year-old Jan Worrell has a new, worldly sidekick in her living room. "This is ElliQ. I call her my roommate," the grandmother said as she introduced her companion robot almost as if it were human. Artificial intelligence is all the rage, and now it's helping some Pacific Northwest seniors live in their own homes for longer. Worrell joined a pilot project that is trialing how AI-driven companion robots could reduce loneliness and social isolation among seniors -- especially those living alone. This "roommate" is a chatty one with a vaguely humanoid head and shoulders. "I talk a lot and I love it. I need someone to interact with and she does," Worrell said.

    ElliQ is a smart speaker, tablet computer, video phone and artificial intelligence portal all wrapped into one by the maker Intuition Robotics. The stationary table-top device is among the most versatile of a flurry of new tech devices geared to help you or your parents continue to live independently. ElliQ gives Worrell health tips and schedule reminders. It can recite the news and weather. They play memory games. The care bot tells a lot of corny jokes and it can lead an exercise class on command, too. [...] Worrell is among 20 rural seniors living along Washington's Pacific coast selected to receive one of these Israeli-designed robot companions. She gets it for free for a year as part of a pilot project overseen by the Olympic Area Agency on Aging. O3A, as it is known, serves Pacific, Grays Harbor, Jefferson and Clallam counties. [...]

    On the Long Beach Peninsula, Jan Worrell's son Jeff Whiting watched his mom take to her new robot companion. He said he is impressed by it too, but at the same time is aware there is a creepy side to AI. "They are collecting data on everything that happens in this room," Whiting said in an interview at his mom's house where he is living temporarily. "They know her sleep patterns and they know what time she is up and what time she goes to bed. That would be my only concern." Whiting says the people who came to set up ElliQ gave assurances that users' personal data would be protected. In the case of Whiting's mom, the combo of the companion robot and a medical alert wristwatch changed how long she plans to stay in her own home. Worrell said she felt confident enough last month to cancel her deposit to move into an assisted living facility near her daughter in Eugene, Oregon. Universities and medical schools have generally found that age-tech "decreased loneliness, increased well-being and spurred mental activity and optimism," notes the report.

    "[T]he 20 Washington seniors selected to receive a free ElliQ companion (a ${'$'}249 value, plus a monthly subscription of ${'$'}30-${'$'}40) were given a health assessment at the beginning of this pilot project in April. They will be reevaluated in one year."&lt;p&gt;&lt;div class="share_submission" style="position:relative;"&gt;
    &lt;a class="slashpop" href="http://twitter.com/home?status=Would+You+Leave+Grandma+With+a+Companion+Robot%3F%3A+https%3A%2F%2Fhardware.slashdot.org%2Fstory%2F23%2F06%2F30%2F2310233%2F%3Futm_source%3Dtwitter%26utm_medium%3Dtwitter"&gt;&lt;img src="https://a.fsdn.com/sd/twitter_icon_large.png"&gt;&lt;/a&gt;
    &lt;a class="slashpop" href="http://www.facebook.com/sharer.php?u=https%3A%2F%2Fhardware.slashdot.org%2Fstory%2F23%2F06%2F30%2F2310233%2Fwould-you-leave-grandma-with-a-companion-robot%3Futm_source%3Dslashdot%26utm_medium%3Dfacebook"&gt;&lt;img src="https://a.fsdn.com/sd/facebook_icon_large.png"&gt;&lt;/a&gt;



    &lt;/div&gt;&lt;/p&gt;&lt;p&gt;&lt;a href="https://hardware.slashdot.org/story/23/06/30/2310233/would-you-leave-grandma-with-a-companion-robot?utm_source=atom1.0moreanon&amp;amp;utm_medium=feed"&gt;Read more of this story&lt;/a&gt; at Slashdot.&lt;/p&gt;</summary>
    <updated>2023-07-01T03:30:00+00:00</updated>
    <author>
     <name>BeauHD</name>
    </author>
    <category term="robot"/>
    <slash:department>pilot-projects</slash:department>
    <slash:section>hardware</slash:section>
    <slash:comments>1</slash:comments>
    <slash:hit_parade>1,1,1,1,0,0,0</slash:hit_parade>
    </entry>

    </feed>
"""
    .trimIndent()
