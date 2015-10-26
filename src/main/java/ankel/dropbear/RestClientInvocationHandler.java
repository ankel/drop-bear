package ankel.dropbear;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.nio.client.HttpAsyncClient;

import com.google.common.base.Supplier;
import com.jive.foss.pnky.Pnky;
import com.jive.foss.pnky.PnkyPromise;

/**
 * @author Binh Tran
 */
@Slf4j
public class RestClientInvocationHandler implements InvocationHandler
{
  private final Supplier<String> rootUriSupplier;
  private final String rootConsume;
  private final String rootProduces;
  private final HttpAsyncClient httpAsyncClient;

  public RestClientInvocationHandler(
      final Supplier<String> rootUrlSupplier,
      final Class<?> klass,
      final HttpAsyncClient httpAsyncClient)
  {
    this.httpAsyncClient = httpAsyncClient;

    this.rootUriSupplier = rootUrlSupplier;

    this.rootConsume = Optional.ofNullable(klass.getAnnotation(Consumes.class))
        .map(Consumes::value)
        .map(this::headerValueFromArray)
        .orElse(null);

    this.rootProduces = Optional.ofNullable(klass.getAnnotation(Produces.class))
        .map(Produces::value)
        .map(this::headerValueFromArray)
        .orElse(null);
  }

  @SuppressWarnings("unchecked")
  public Object invoke(final Object proxy, final Method method, final Object[] args)
      throws Throwable
  {
//    log.info("Proxy [{}] method [{}] args [{]]", proxy, method, args);

    if (method.getAnnotation(GET.class) == null)
    {
      throw new IllegalArgumentException("Only support GET method");
    }

    if (!method.getReturnType().isAssignableFrom(PnkyPromise.class))
    {
      throw new IllegalArgumentException("Only return type PnkyPromise is supported");
    }

    final String uri = RestClientUriBuilderUtils.generateUrl(rootUriSupplier, method, args);

    final HttpGet get = new HttpGet(uri);

    final String methodProduces = Optional.ofNullable(method.getAnnotation(Produces.class))
        .map(Produces::value)
        .map(this::headerValueFromArray)
        .orElse(null);

    if (methodProduces != null)
    {
      get.setHeader(HttpHeaders.ACCEPT, methodProduces);
    }
    else if (rootProduces != null)
    {
      get.setHeader(HttpHeaders.ACCEPT, rootProduces);
    }

    PnkyPromise pnkyPromise = Pnky.create();

    httpAsyncClient.execute(get, new FutureCallback<HttpResponse>() {

      public void completed(final HttpResponse response) {

      }

      public void failed(final Exception ex) {
      }

      public void cancelled() {
      }

    });

    return pnkyPromise;
  }

  private String headerValueFromArray(final String[] array)
  {
    return Arrays.asList(array)
        .stream()
        .collect(Collectors.joining(", "));
  }
}
