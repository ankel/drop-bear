package ankel.dropbear;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

/**
 * @author Ankel (Binh Tran)
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RawTextResponseDeserializer implements ResponseDeserializer
{

  public static RawTextResponseDeserializer getInstance()
  {
    return new RawTextResponseDeserializer();
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
              "Cannot use RawTextResponseDeserializer with return type %s",
              type.getTypeName()));
    }

    if (!(klass).isAssignableFrom(String.class))
    {
      throw new IllegalArgumentException(
          String.format(
              "RawTextResponseDeserializer expects String type. Actually got %s",
              type.getTypeName()));
    }

    return (T) CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
  }
}
