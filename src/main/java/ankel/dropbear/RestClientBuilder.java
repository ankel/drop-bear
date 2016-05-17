package ankel.dropbear;

import ankel.dropbear.impl.InvocationHandler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.http.nio.client.HttpAsyncClient;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Binh Tran
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RestClientBuilder
{
  private final HttpAsyncClient httpAsyncClient;
  private Supplier<String> urlSupplier = null;
  private List<RestClientDeserializer> restClientDeserializers = new ArrayList<>();
  private List<RestClientSerializer> restClientSerializers = new ArrayList<>();

  public static RestClientBuilder newBuilder(final HttpAsyncClient httpAsyncClient)
  {
    return new RestClientBuilder(httpAsyncClient);
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

    return (T) Proxy.newProxyInstance(
        klass.getClassLoader(),
        new Class[] { klass },
        new InvocationHandler(urlSupplier, klass, httpAsyncClient,
            restClientDeserializers, restClientSerializers));
  }
}
