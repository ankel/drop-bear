package ankel.dropbear;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author Ankel (Binh Tran)
 */
public interface RestClientDeserializer
{
  List<String> getSupportedMediaTypes();
  <T> T deserialize(final InputStream inputStream, final Type type) throws Exception;
}
