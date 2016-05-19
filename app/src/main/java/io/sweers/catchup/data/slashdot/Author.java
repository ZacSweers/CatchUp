package io.sweers.catchup.data.slashdot;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(strict = false)
public class Author {

  @Element(name = "name", required = true)
  public String name;

}
