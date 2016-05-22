package io.sweers.catchup.data.slashdot;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.NamespaceList;
import org.simpleframework.xml.Root;

import io.sweers.catchup.ui.base.HasStableId;

@NamespaceList(
    {
        @Namespace(reference = "http://purl.org/rss/1.0/modules/slash/", prefix = "slash")
    }
)
@Root(name = "entry", strict = false)
public class Entry implements HasStableId {

  @Element(name = "title", required = false)
  public String title;

  @Element(name = "id")
  public String id;

  @Element(name = "link", required = false)
  public String link;

  @Element(name = "updated", required = false)
  public String updated;

  @Element(name = "section", required = false)
  public String section;

  @Element(name = "comments", required = false)
  public int comments;

  @Element(name = "author", required = false)
  public Author author;

  @Element(name = "department", required = false)
  public String department;

  @Override
  public long stableId() {
    return id.hashCode();
  }
}
