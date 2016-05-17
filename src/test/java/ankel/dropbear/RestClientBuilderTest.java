package ankel.dropbear;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import ankel.dropbear.impl.UriBuilderUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
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
public class RestClientBuilderTest
{
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(8089);

  private CloseableHttpAsyncClient httpAsyncClient;
  private RestInterface restInterface;
  private RestInterface.RequestObject requestObject;

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

    restInterface = RestClientBuilder.newBuilder(httpAsyncClient)
        .url("http://localhost:8089")
        .addRestClientDeserializer(RawTextRestClientSerializationSupport.getDefaultInstance())
        .addRestClientDeserializer(new JacksonRestClientSerializationSupport(new ObjectMapper()))
        .addRestClientSerializer(RawTextRestClientSerializationSupport.getDefaultInstance())
        .addRestClientSerializer(new JacksonRestClientSerializationSupport(new ObjectMapper()))
        .of(RestInterface.class);

    requestObject = new RestInterface.RequestObject("42", "answer to everything");
  }

  @After
  public void teardown() throws Exception
  {
    httpAsyncClient.close();
  }

  @Test
  public void testPathParams() throws Exception
  {
    final String s = UriBuilderUtils.generateUrl(
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
        .withHeader(HttpHeaders.USER_AGENT, containing("InvocationHandler"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
            .withBody("Some text")));

    assertEquals("Some text", restInterface.getFooString().get());

    verify(1, getRequestedFor(urlEqualTo("/start/foo"))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.TEXT_PLAIN))
        .withHeader(HttpHeaders.USER_AGENT, containing("InvocationHandler")));
  }

  @Test
  public void testGet404() throws Exception
  {
    stubFor(get(urlEqualTo("/start/foo"))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.TEXT_PLAIN))
        .withHeader(HttpHeaders.USER_AGENT, containing("InvocationHandler"))
        .willReturn(aResponse()
            .withStatus(404)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
            .withBody("Not Found response body")));

    try
    {
      restInterface.getFooString().get();
    }
    catch (ExecutionException e)
    {
      final Throwable cause = e.getCause();
      assertTrue(cause instanceof RestClientResponseException);

      RestClientResponseException responseException = (RestClientResponseException) cause;
      assertEquals(404, responseException.getStatusCode());

      final String content = CharStreams.toString(
          new InputStreamReader(responseException.getRawContent(), Charset.defaultCharset()));

      assertEquals("Not Found response body", content);
    }

    verify(1, getRequestedFor(urlEqualTo("/start/foo"))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.TEXT_PLAIN))
        .withHeader(HttpHeaders.USER_AGENT, containing("InvocationHandler")));
  }

  @Test
  public void testGetJson() throws Exception
  {
    stubFor(get(urlEqualTo("/start/foo"))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.USER_AGENT, containing("InvocationHandler"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
            .withBody("{\"foo\": \"bar\"}")));

    final Map<String, String> map = restInterface.getFoo().get();

    verify(1, getRequestedFor(urlEqualTo("/start/foo"))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.USER_AGENT, containing("InvocationHandler")));

    assertEquals(1, map.size());
    assertEquals("bar", map.get("foo"));
  }

  @Test
  public void testPostJson() throws Exception
  {
    stubFor(post(urlEqualTo("/start/foo"))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.USER_AGENT, containing("InvocationHandler"))
        .withRequestBody(equalTo("42"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
            .withBody("{\"foo\": \"bar\"}")));

    final Map<String, String> map = restInterface.postFoo(42L).get();

    verify(1, postRequestedFor(urlEqualTo("/start/foo"))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.USER_AGENT, containing("InvocationHandler"))
        .withRequestBody(equalTo("42")));

    assertEquals(1, map.size());
    assertEquals("bar", map.get("foo"));
  }

  @Test
  public void testPutString() throws Exception
  {
    stubFor(put(urlEqualTo("/start/foo"))
      .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
      .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.TEXT_PLAIN))
      .withHeader(HttpHeaders.USER_AGENT, containing("InvocationHandler"))
      .withRequestBody(equalTo("put string"))
      .willReturn(aResponse()
          .withStatus(202)
          .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)));

    restInterface.putStringReturnVoid("put string").get();

    verify(1, putRequestedFor(urlEqualTo("/start/foo"))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.TEXT_PLAIN))
        .withHeader(HttpHeaders.USER_AGENT, containing("InvocationHandler"))
        .withRequestBody(equalTo("put string")));
  }

  @Test
  public void testPostObject() throws Exception
  {
    stubFor(post(urlEqualTo("/start/foo"))
      .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
      .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON))
      .withHeader(HttpHeaders.USER_AGENT, containing("InvocationHandler"))
      .withRequestBody(equalTo("{\"id\":\"42\",\"requestType\":\"answer to everything\"}"))
      .willReturn(aResponse()
          .withStatus(202)
          .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)));

    restInterface.postObjectReturnVoid(requestObject).get();

    verify(1, postRequestedFor(urlEqualTo("/start/foo"))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.USER_AGENT, containing("InvocationHandler"))
        .withRequestBody(equalTo("{\"id\":\"42\",\"requestType\":\"answer to everything\"}")));
  }

  @Test
  public void testPutObject() throws Exception
  {
    stubFor(put(urlEqualTo("/start/foo"))
      .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
      .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON))
      .withHeader(HttpHeaders.USER_AGENT, containing("InvocationHandler"))
      .withRequestBody(equalTo("{\"id\":\"42\",\"requestType\":\"answer to everything\"}"))
      .willReturn(aResponse()
          .withStatus(200)
          .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
          .withBody("{\"foo\": \"bar\"}")));

    final Map<String, String> map = restInterface.putObject(requestObject).get();

    verify(1, putRequestedFor(urlEqualTo("/start/foo"))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.USER_AGENT, containing("InvocationHandler"))
        .withRequestBody(equalTo("{\"id\":\"42\",\"requestType\":\"answer to everything\"}")));

    assertEquals(1, map.size());
    assertEquals("bar", map.get("foo"));
  }
}
