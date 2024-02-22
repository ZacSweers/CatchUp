package catchup.repro;

import static org.mockito.Mockito.mock;

import org.junit.Before;

public class ApiRxAdapterTest {

  private HttpClient httpClientMock;

  @Before
  public void setUp() {
    httpClientMock = mock(HttpClient.class);
  }
}
