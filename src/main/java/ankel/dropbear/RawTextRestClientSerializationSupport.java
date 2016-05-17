package ankel.dropbear;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import lombok.Getter;

import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author Ankel (Binh Tran)
 */
public final class RawTextRestClientSerializationSupport
    implements RestClientDeserializer, RestClientSerializer
{
  @Getter
  private final List<String> supportedMediaTypes;

  /**
   * Construct an instance of {@link RawTextRestClientSerializationSupport} with {@code MediaType.TEXT_PLAIN}
   * as its media type
   */
  public static RawTextRestClientSerializationSupport getDefaultInstance()
  {
    return new RawTextRestClientSerializationSupport(MediaType.TEXT_PLAIN);
  }

  /**
   * Construct an instance of {@link RawTextRestClientSerializationSupport} that supports multiple media type
   */
  public static RawTextRestClientSerializationSupport getInstanceForMediaTypes(final String type,
      final String... types)
  {
    return new RawTextRestClientSerializationSupport(type, types);
  }


  private RawTextRestClientSerializationSupport(final String type, final String... types)
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
              "Cannot use RawTextRestClientSerializationSupport with return type %s",
              type.getTypeName()));
    }

    if (!(klass).isAssignableFrom(String.class))
    {
      throw new IllegalArgumentException(
          String.format(
              "RawTextRestClientSerializationSupport expects String type. Actually got %s",
              type.getTypeName()));
    }

    return (T) CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
  }

  @Override
  public String serialize(final Object object)
  {
    return String.valueOf(object);
  }
}
