package ankel.dropbear;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import lombok.Getter;

import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author Ankel (Binh Tran)
 */
public final class RawTextRestClientDeserializer implements RestClientDeserializer
{
  @Getter
  private final List<String> supportedMediaTypes;

  public static RawTextRestClientDeserializer getDefaultInstance()
  {
    return new RawTextRestClientDeserializer(MediaType.TEXT_PLAIN);
  }

  public RawTextRestClientDeserializer(final String type, final String... types)
  {
    this.supportedMediaTypes = ImmutableList.<String>builder()
        .add(type)
        .add(types)
        .build();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T deserialize(final InputStream inputStream, final Type type) throws Exception
  {
    final Class klass;
    try
    {
      klass = (Class) type;
    }
    catch (Exception e)
    {
      throw new IllegalArgumentException(
          String.format(
              "Cannot use RawTextRestClientDeserializer with return type %s",
              type.getTypeName()));
    }

    if (!(klass).isAssignableFrom(String.class))
    {
      throw new IllegalArgumentException(
          String.format(
              "RawTextRestClientDeserializer expects String type. Actually got %s",
              type.getTypeName()));
    }

    return (T) CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
  }
}
