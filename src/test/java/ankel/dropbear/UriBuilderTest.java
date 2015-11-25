package ankel.dropbear;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import ankel.dropbear.impl.RestClientUriBuilderUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.junit.After;
import org.junit.Before;
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

  private CloseableHttpAsyncClient httpAsyncClient;

  private Method getFooMethod() throws Exception
  {
    return RestInterface.class
        .getMethod("getFooWithParams", String.class, Integer.class, String.class, Long.class);
  }

  @Before
  public void setup() throws Exception
  {
    httpAsyncClient = HttpAsyncClients.createDefault();
    httpAsyncClient.start();
  }

  @After
  public void teardown() throws Exception
  {
    httpAsyncClient.close();
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
  public void testGetString() throws Exception
  {
    stubFor(get(urlEqualTo("/start/foo"))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.TEXT_PLAIN))
        .withHeader(HttpHeaders.USER_AGENT, containing("RestClientInvocationHandler"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
            .withBody("Some text")));

    final RestInterface restInterface = RestClientBuilder.newBuilder(httpAsyncClient)
        .url("http://localhost:8089")
        .responseDeserializer(RawTextResponseDeserializer.getInstance())
        .of(RestInterface.class);

    assertEquals("Some text", restInterface.getFooString().get());

    verify(1, getRequestedFor(urlMatching("/start/foo"))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.TEXT_PLAIN))
        .withHeader(HttpHeaders.USER_AGENT, containing("RestClientInvocationHandler")));
  }

  @Test
  public void testGetJson() throws Exception
  {
    stubFor(get(urlEqualTo("/start/foo"))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.USER_AGENT, containing("RestClientInvocationHandler"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
            .withBody("{\"foo\": \"bar\"}")));

    final RestInterface restInterface = RestClientBuilder.newBuilder(httpAsyncClient)
        .url("http://localhost:8089")
        .responseDeserializer(JacksonResponseDeserializer.fromObjectMapper(new ObjectMapper()))
        .of(RestInterface.class);

    final Map<String, String> map = restInterface.getFoo().get();

    verify(1, getRequestedFor(urlMatching("/start/foo"))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.USER_AGENT, containing("RestClientInvocationHandler")));

    assertEquals(1, map.size());
    assertEquals("bar", map.get("foo"));
  }
}
