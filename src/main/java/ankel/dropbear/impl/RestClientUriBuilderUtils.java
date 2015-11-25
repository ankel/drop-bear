package ankel.dropbear.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import lombok.experimental.UtilityClass;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import com.google.common.base.Supplier;

/**
 * @author Binh Tran
 */
@UtilityClass
public class RestClientUriBuilderUtils
{

  public static String generateUrl(
      final Supplier<String> rootUrlSupplier,
      final Method method,
      final Object[] arguments)
      throws URISyntaxException
  {
    final Parameter[] parameters = method.getParameters();

    final String path = rootUrlSupplier.get() + "/" +
        getPathFromClass(method.getDeclaringClass()) + "/" +
        getFragmentFromMethodPath(method, parameters, arguments);

    final URIBuilder uriBuilder = new URIBuilder()
        .setPath(path);

    for (int i = 0; i < parameters.length; ++i)
    {
      final Parameter p = parameters[i];
      final Object a = arguments[i];
      Optional.ofNullable(p.getAnnotation(QueryParam.class))
          .map(QueryParam::value)
          .ifPresent((s) -> uriBuilder.addParameter(s, String.valueOf(a)));
    }

    return StringUtils.stripStart(uriBuilder.build().toString(), "/");
  }

  private static String getPathFromClass(final Class<?> declaringClass)
  {
    return Optional.ofNullable(declaringClass.getAnnotation(Path.class))
        .map(Path::value)
        .map((s) -> StringUtils.strip(s, "/"))
        .orElse("");
  }

  private static String getFragmentFromMethodPath(final Method method,
      final Parameter[] parameters, final Object[] arguments)
  {
    final String fragmentString = Optional.ofNullable(method.getAnnotation(Path.class))
        .map(Path::value)
        .orElse("");

    final AtomicReference<String> fragment = new AtomicReference<>(fragmentString);

    for (int i = 0; i < parameters.length; ++i)
    {
      final Parameter p = parameters[i];
      final Object a = arguments[i];
      Optional.ofNullable(p.getAnnotation(PathParam.class))
          .map(PathParam::value)
          .ifPresent((s) -> fragment.set(
              fragment.get().replace("{" + s + "}", String.valueOf(a))));
    }

    return StringUtils.stripStart(fragment.get(), "/");
  }
}
