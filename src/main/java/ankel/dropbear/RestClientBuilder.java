package ankel.dropbear;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import ankel.dropbear.impl.RestClientInvocationHandler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.apache.http.nio.client.HttpAsyncClient;

import com.google.common.base.Supplier;

/**
 * @author Binh Tran
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RestClientBuilder
{
  private final HttpAsyncClient httpAsyncClient;
  private Supplier<String> urlSupplier = null;
  private List<RestClientDeserializer> restClientDeserializers = new ArrayList<>();

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
    this.urlSupplier = () -> url;
    return this;
  }

  public RestClientBuilder addRestClientDeserializer(final RestClientDeserializer restClientDeserializer)
  {
    this.restClientDeserializers.add(restClientDeserializer);
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
        new RestClientInvocationHandler(urlSupplier, klass, httpAsyncClient, restClientDeserializers));
  }
}
