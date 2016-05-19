package io.sweers.catchup.data.slashdot;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.NamespaceList;
import org.simpleframework.xml.Root;

import java.util.List;

@NamespaceList(
    {
        @Namespace(reference = "http://www.w3.org/2005/Atom", prefix = "atom")
    }
)
@Root(strict = false)
public class Feed {

  @ElementList(name = "item", required = true, inline = true)
  public List<Entry> itemList;

  @Element(name = "title", required = false)
  public String title;

  @Element(name = "language", required = false)
  public String language;

  @Element(name = "updated", required = false)
  public String updated;

}
