package ankel.dropbear;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * @author Ankel (Binh Tran)
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class JacksonResponseDeserializer implements ResponseDeserializer
{
  private final ObjectMapper mapper;

  public static JacksonResponseDeserializer fromObjectMapper(final ObjectMapper mapper)
  {
    return new JacksonResponseDeserializer(mapper);
  }

  @Override
  public <T> T deserialize(final InputStream inputStream, final Type type) throws Exception
  {
    final JavaType returnType = mapper.constructType(type);
    return mapper.readValue(inputStream, returnType);
  }
}
