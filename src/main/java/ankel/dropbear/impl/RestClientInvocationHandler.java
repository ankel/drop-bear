package ankel.dropbear.impl;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.print.attribute.standard.Media;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import ankel.dropbear.RestClientDeserializer;
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
public final class RestClientInvocationHandler implements InvocationHandler
{
  private final Supplier<String> rootUriSupplier;
  private final String rootConsume;
  private final String rootProduces;
  private final HttpAsyncClient httpAsyncClient;
  private final List<RestClientDeserializer> restClientDeserializers;

  public RestClientInvocationHandler(
      final Supplier<String> rootUrlSupplier,
      final Class<?> klass,
      final HttpAsyncClient httpAsyncClient,
      final List<RestClientDeserializer> restClientDeserializers)
  {
    this.httpAsyncClient = httpAsyncClient;
    this.rootUriSupplier = rootUrlSupplier;
    this.restClientDeserializers = restClientDeserializers;

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
    log.debug("Proxy class [{}] method [{}] args [{]]", method.getDeclaringClass(), method, args);

    if (method.getAnnotation(GET.class) == null)
    {
      throw new IllegalArgumentException("Only support GET method");
    }

    if (!method.getReturnType().isAssignableFrom(PnkyPromise.class))
    {
      throw new IllegalArgumentException("Only return type com.jive.foss.pnky.PnkyPromise is supported");
    }

    final Type returnedInnerType;

    try
    {
      returnedInnerType =
          ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
    }
    catch (Exception e)
    {
      throw new IllegalArgumentException("Cannot return raw com.jive.foss.pnky.PnkyPromise type");
    }

    final String uri = RestClientUriBuilderUtils.generateUrl(rootUriSupplier, method, args);

    final HttpGet get = new HttpGet(uri);

    final String methodProduces = Optional.ofNullable(method.getAnnotation(Produces.class))
        .map(Produces::value)
        .map(this::headerValueFromArray)
        .orElse(null);

    final String acceptHeader;

    if (methodProduces != null)
    {
      acceptHeader = methodProduces;
    }
    else if (rootProduces != null)
    {
      acceptHeader = rootProduces;
    }
    else
    {
      acceptHeader = MediaType.WILDCARD;  //TODO: default to wildcard, not sure if that's a good idea.
    }

    get.setHeader(HttpHeaders.ACCEPT, acceptHeader);
    get.setHeader(HttpHeaders.USER_AGENT, getClass().getCanonicalName() + " v1");

    final RestClientDeserializer restClientDeserializer =
        restClientDeserializers.stream()
            .filter((deserializer) -> deserializer.getSupportedMediaTypes().contains(acceptHeader))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Cannot find suitable deserializer for media type "
                    + acceptHeader));

    final Pnky resultPromise = Pnky.create();

    httpAsyncClient.execute(get, new FutureCallback<HttpResponse>()
    {

      public void completed(final HttpResponse response)
      {
        final Object result;
        try
        {
          final InputStream responseStream = response.getEntity().getContent();
          result = restClientDeserializer.deserialize(responseStream, returnedInnerType);
        }
        catch (Exception e)
        {
          resultPromise.reject(e);
          return;
        }
        resultPromise.resolve(result);
      }

      public void failed(final Exception ex)
      {
        resultPromise.reject(ex);
      }

      public void cancelled()
      {
        resultPromise.reject(new InterruptedException("Request is interrupted"));
      }

    });

    return resultPromise;
  }

  private String headerValueFromArray(final String[] array)
  {
    return Arrays.asList(array)
        .stream()
        .collect(Collectors.joining(", "));
  }
}