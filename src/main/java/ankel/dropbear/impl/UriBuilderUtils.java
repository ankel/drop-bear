package ankel.dropbear.impl;

import com.google.common.collect.Lists;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;

import javax.ws.rs.FormParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @author Binh Tran
 */
@UtilityClass
public class UriBuilderUtils
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

  public static Object getEntityObject(
      final Method method,
      final Object[] arguments)
  {
    final Parameter[] parameters = method.getParameters();

    for (int i = 0; i < parameters.length; ++i)
    {
      if (parameters[i].getAnnotation(PathParam.class) == null &&
          parameters[i].getAnnotation(QueryParam.class) == null)
      {
        return arguments[i];
      }
    }

    throw new IllegalArgumentException(
        String.format(
            "There is no suitable entity argument from [%s]. All arguments are annotated with PathParam or QueryParam",
            method.toGenericString()));
  }

  public static List<NameValuePair> getFormParameters(
      final Method method,
      final Object[] arguments)
  {
    final List<NameValuePair> nameValuePairs = Lists.newArrayList();
    final Parameter[] parameters = method.getParameters();
    for (int i = 0; i < parameters.length; ++i)
    {
      if (parameters[i].getAnnotation(FormParam.class) != null)
      {
        nameValuePairs.add(
            new BasicNameValuePair(parameters[i].getAnnotation(FormParam.class).value(),
                String.valueOf(arguments[i])));
      }
    }
    return nameValuePairs;
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
