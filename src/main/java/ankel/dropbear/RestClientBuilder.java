package ankel.dropbear;

import ankel.dropbear.impl.InvocationHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.http.Header;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.nio.client.HttpAsyncClient;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Binh Tran
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RestClientBuilder
{
  private HttpAsyncClient httpAsyncClient;
  private Supplier<String> urlSupplier = null;
  private final List<RestClientDeserializer> restClientDeserializers = Lists.newArrayList();
  private final List<RestClientSerializer> restClientSerializers = Lists.newArrayList();
  private final List<Header> defaultHeaders = Lists.newArrayList();
  private ObjectMapper objectMapper;

  private void setDefaultObjectMapper()
  {
    if (objectMapper == null)
    {
      objectMapper = new ObjectMapper();
    }
  }

  private void setDefaultDeserializers()
  {
    if (objectMapper == null)
    {
      throw new RuntimeException("ObjectMapper is null");
    }

    if (restClientDeserializers.size() == 0)
    {
      restClientDeserializers.add(new JacksonRestClientSerializationSupport(objectMapper));
      restClientDeserializers.add(RawTextRestClientSerializationSupport.getDefaultInstance());
    }
  }

  private void setDefaultSerializers()
  {
    if (objectMapper == null)
    {
      throw new RuntimeException("ObjectMapper is null");
    }

    if (restClientSerializers.size() == 0)
    {
      restClientSerializers.add(new JacksonRestClientSerializationSupport(objectMapper));
      restClientSerializers.add(RawTextRestClientSerializationSupport.getDefaultInstance());
    }
  }

  private void setDefaultHttpClient()
  {
    final HttpAsyncClientBuilder httpAsyncClientBuilder = HttpAsyncClientBuilder.create();
    if (defaultHeaders.size() > 0)
    {
      httpAsyncClientBuilder.setDefaultHeaders(defaultHeaders);
    }
    final CloseableHttpAsyncClient closeableHttpAsyncClient = httpAsyncClientBuilder.build();
    closeableHttpAsyncClient.start();
    httpAsyncClient = closeableHttpAsyncClient;
  }

  /**
   *
   * @return RestClientBuilder with default httpSyncClient;
   */

  public static RestClientBuilder newBuilder()
  {
    return new RestClientBuilder();
  }

  public RestClientBuilder setHttpAsyncClient(HttpAsyncClient httpAsyncClient)
  {
    this.httpAsyncClient = httpAsyncClient;
    return this;
  }

  public RestClientBuilder addDefaultHeader(final Header header)
  {
    defaultHeaders.add(header);
    return this;
  }

  public RestClientBuilder urlSupplier(final Supplier<String> urlSupplier)
  {
    this.urlSupplier = urlSupplier;
    return this;
  }

  public RestClientBuilder url(final String url)
  {
    urlSupplier = () -> url;
    return this;
  }

  public RestClientBuilder addRestClientDeserializer(final RestClientDeserializer deserializer)
  {
    restClientDeserializers.add(deserializer);
    return this;
  }

  public RestClientBuilder addRestClientSerializer(final RestClientSerializer serializer)
  {
    restClientSerializers.add(serializer);
    return this;
  }

  @SuppressWarnings("unchecked")
  public final <T> T of(final Class<T> klass)
  {
    if (urlSupplier == null)
    {
      throw new NullPointerException("No url supplier specified");
    }

    setDefaultObjectMapper();
    setDefaultDeserializers();
    setDefaultSerializers();
    setDefaultHttpClient();

    return (T) Proxy.newProxyInstance(
        klass.getClassLoader(),
        new Class[] { klass },
        new InvocationHandler(urlSupplier, klass, httpAsyncClient,
            restClientDeserializers, restClientSerializers));
  }
}
