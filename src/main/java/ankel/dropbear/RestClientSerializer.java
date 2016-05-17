package ankel.dropbear;

import java.util.List;

/**
 * @author Ankel (Binh Tran)
 */
public interface RestClientSerializer
{
  List<String> getSupportedMediaTypes();
  String serialize(final Object object) throws Exception;
}
