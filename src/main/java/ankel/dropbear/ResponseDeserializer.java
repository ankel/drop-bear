package ankel.dropbear;

import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * @author Ankel (Binh Tran)
 */
public interface ResponseDeserializer
{
  <T> T deserialize(final InputStream inputStream, final Type type) throws Exception;
}
