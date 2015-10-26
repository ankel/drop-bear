package ankel.dropbear;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

import java.lang.reflect.Method;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * @author Binh Tran
 */
public class UriBuilderTest
{
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(8089);

  private Method getFooMethod() throws Exception
  {
    return RestInterface.class
        .getMethod("getFooWithParams", String.class, Integer.class, String.class, Long.class);
  }

  @Test
  public void testPathParams() throws Exception
  {
    final String s = RestClientUriBuilderUtils.generateUrl(
        () -> "http://www.google.com",
        getFooMethod(),
        new Object[] { "foo", 42, "bar", 101L });

    assertEquals("http://www.google.com/start/foo/42?q1=bar&q2=101", s);
  }

  @Test
  public void testGet() throws Exception
  {
    stubFor(get(urlEqualTo("/start/foo"))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
            .withBody("{\"foo\": \"bar\"}")));

    final RestInterface restInterface = RestClientBuilder.newBuilder(HttpAsyncClients.createDefault())
        .url("http://localhost:8089")
        .of(RestInterface.class);

    restInterface.getFoo();

    verify(getRequestedFor(urlMatching("/start/foo"))
        .withHeader(HttpHeaders.ACCEPT, matching(MediaType.APPLICATION_JSON)));
  }
}
