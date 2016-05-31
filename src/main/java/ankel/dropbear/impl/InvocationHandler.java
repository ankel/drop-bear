package ankel.dropbear.impl;

import ankel.dropbear.RestClientDeserializer;
import ankel.dropbear.RestClientResponseException;
import ankel.dropbear.RestClientSerializer;
import com.jive.foss.pnky.Pnky;
import com.jive.foss.pnky.PnkyPromise;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.*;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.nio.client.HttpAsyncClient;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Binh Tran
 */
@Slf4j
public final class InvocationHandler implements java.lang.reflect.InvocationHandler
{
  private final Supplier<String> rootUriSupplier;
  private final String rootConsume;
  private final String rootProduces;
  private final HttpAsyncClient httpAsyncClient;
  private final List<RestClientDeserializer> restClientDeserializers;
  private final List<RestClientSerializer> restClientSerializers;

  public InvocationHandler(
      final java.util.function.Supplier<String> rootUrlSupplier,
      final Class<?> klass,
      final HttpAsyncClient httpAsyncClient,
      final List<RestClientDeserializer> restClientDeserializers,
      final List<RestClientSerializer> restClientSerializers)
  {
    this.httpAsyncClient = httpAsyncClient;
    this.rootUriSupplier = rootUrlSupplier;
    this.restClientDeserializers = restClientDeserializers;
    this.restClientSerializers = restClientSerializers;

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
    log.debug("Proxy class [{}] method [{}] args [{}]", method.getDeclaringClass(), method, args);

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

    final HttpUriRequest httpRequest;

    final String uri = UriBuilderUtils.generateUrl(rootUriSupplier, method, args);

    if (method.getAnnotation(GET.class) != null)
    {
      httpRequest = createHttpUriGetRequest(uri);
    }
    else if (method.getAnnotation(POST.class) != null)
    {
      httpRequest = setEntityForHttpRequest(new HttpPost(uri), method, args);
    }
    else if (method.getAnnotation(PUT.class) != null)
    {
      httpRequest = setEntityForHttpRequest(new HttpPut(uri), method, args);
    }
    else
    {
      throw new IllegalArgumentException("Only support GET, POST and PUT method");
    }

    final RestClientDeserializer restClientDeserializer = setHeaders(httpRequest, method);

    final Pnky resultPromise = Pnky.create();

    httpAsyncClient.execute(httpRequest, new FutureCallback<HttpResponse>()
    {

      public void completed(final HttpResponse response)
      {
        final Object result;

        if (response.getStatusLine().getStatusCode() / 100 == 2)
        {

          if (returnedInnerType.equals(Void.class))
          {
            resultPromise.resolve();
          }
          else
          {
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
        }
        else
        {
          InputStream content = null;
          try
          {
            content = response.getEntity().getContent();
          }
          catch (IOException e)
          {
            log.error("Failed to extract input stream content from response", e);
          }

          Map<String, List<String>> headers = new HashMap<>();

          for (final Header header : response.getAllHeaders())
          {
            headers.computeIfAbsent(header.getName(), (__) -> new ArrayList<>())
                .add(header.getValue());
          }

          resultPromise.reject(new RestClientResponseException(
              response.getStatusLine().getStatusCode(), content, headers));
        }
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

  private HttpUriRequest createHttpUriGetRequest(final String uri)
      throws URISyntaxException
  {
    return new HttpGet(uri);
  }

  private HttpUriRequest setEntityForHttpRequest(
      final HttpEntityEnclosingRequestBase baseRequest, final Method method, final Object[] args)
  {
    final String methodConsumes = Optional.ofNullable(method.getAnnotation(Consumes.class))
        .map(Consumes::value)
        .map(this::headerValueFromArray)
        .orElse(rootConsume);

    final String contentTypeHeader;

    if (methodConsumes != null)
    {
      contentTypeHeader = methodConsumes;
    }
    else
    {
      contentTypeHeader = MediaType.WILDCARD;
    }

    baseRequest.setHeader(HttpHeaders.CONTENT_TYPE, contentTypeHeader);

    final HttpEntity httpEntity;

    if (contentTypeHeader.equals(MediaType.APPLICATION_FORM_URLENCODED))
    {
      // Get the Name/Value Pairs

      final List<NameValuePair> nameValuePairs = UriBuilderUtils.getFormParameters(method, args);
      httpEntity = EntityBuilder.create().setParameters(nameValuePairs).build();
    }
    else
    {

      final Object entityPojo = UriBuilderUtils.getEntityObject(method, args);

      if (entityPojo instanceof InputStream)
      {
        httpEntity = new InputStreamEntity(
            (InputStream) entityPojo, ContentType.create(contentTypeHeader));
      }
      else
      {
        RestClientSerializer serializer = null;

        for (RestClientSerializer s : restClientSerializers)
        {
          if (s.getSupportedMediaTypes().contains(contentTypeHeader))
          {
            serializer = s;
            break;
          }
        }

        if (serializer == null)
        {
          throw new IllegalStateException(
              String.format("Cannot find a suitable serializer for method %s#%s",
                  method.getDeclaringClass().getCanonicalName(), method.getName()));
        }

        try
        {
          httpEntity = new StringEntity(
              serializer.serialize(entityPojo),
              ContentType.create(contentTypeHeader));
        }
        catch (Exception e)
        {
          throw new RuntimeException("Failed to serialize entity", e);
        }
      }
    }

    baseRequest.setEntity(httpEntity);

    return baseRequest;
  }

  private RestClientDeserializer setHeaders(final HttpUriRequest httpRequest, final Method method)
  {
    final String methodProduces = Optional.ofNullable(method.getAnnotation(Produces.class))
        .map(Produces::value)
        .map(this::headerValueFromArray)
        .orElse(rootProduces);

    final String acceptHeader;

    if (methodProduces != null)
    {
      acceptHeader = methodProduces;
    }
    else
    {
      acceptHeader = MediaType.WILDCARD; // TODO: default to wildcard, not sure if that's a good
                                         // idea.
    }

    httpRequest.setHeader(HttpHeaders.ACCEPT, acceptHeader);
    httpRequest.setHeader(HttpHeaders.USER_AGENT, getClass().getCanonicalName() + " v1");

    return restClientDeserializers.stream()
        .filter((deserializer) -> deserializer.getSupportedMediaTypes().contains(acceptHeader))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "Cannot find suitable deserializer for media type "
                + acceptHeader));
  }

  private String headerValueFromArray(final String[] array)
  {
    return Arrays.asList(array)
        .stream()
        .collect(Collectors.joining(", "));
  }
}
