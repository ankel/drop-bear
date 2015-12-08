package ankel.dropbear;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * @author Ankel (Binh Tran)
 */
@RequiredArgsConstructor
public final class JacksonRestClientDeserializer implements RestClientDeserializer
{
  private final ObjectMapper mapper;

  @Override
  public List<String> getSupportedMediaTypes()
  {
    return Collections.singletonList(MediaType.APPLICATION_JSON);
  }

  @Override
  public <T> T deserialize(final InputStream inputStream, final Type type) throws Exception
  {
    final JavaType returnType = mapper.constructType(type);
    return mapper.readValue(inputStream, returnType);
  }
}
