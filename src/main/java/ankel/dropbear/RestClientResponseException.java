package ankel.dropbear;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * @author Ankel (Binh Tran)
 */
@RequiredArgsConstructor
@Getter
public class RestClientResponseException extends Exception
{
  private final int statusCode;
  private final InputStream rawContent;
  private final Map<String, List<String>> headers;
}
