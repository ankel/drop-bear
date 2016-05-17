package ankel.dropbear;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;

/**
 * @author Ankel (Binh Tran)
 */
@RequiredArgsConstructor
@Getter
public class RestClientResponseException extends Exception
{
  private final int statusCode;
  private final InputStream rawContent;
}
