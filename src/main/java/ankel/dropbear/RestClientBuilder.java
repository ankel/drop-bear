package ankel.dropbear;

import java.lang.reflect.Proxy;

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
  private ResponseDeserializer responseDeserializer;

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

  public RestClientBuilder responseDeserializer(final ResponseDeserializer responseDeserializer)
  {
    this.responseDeserializer = responseDeserializer;
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
        new RestClientInvocationHandler(urlSupplier, klass, httpAsyncClient, responseDeserializer));
  }
}
