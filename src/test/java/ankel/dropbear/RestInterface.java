package ankel.dropbear;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import com.jive.foss.pnky.PnkyPromise;

/**
 * @author Binh Tran
 */
@Path("start")
public interface RestInterface
{
  @Path("{p1}/{p2}")
  @GET
  String getFooWithParams(
      @PathParam("p1") final String p1,
      @PathParam("p2") final Integer p2,
      @QueryParam("q1") final String q1,
      @QueryParam("q2") final Long q2);

  @Path("foo")
  @GET
  PnkyPromise<Map<String, String>> getFoo();
}
