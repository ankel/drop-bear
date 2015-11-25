package ankel.dropbear;

import java.util.Map;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import com.jive.foss.pnky.PnkyPromise;

/**
 * @author Binh Tran
 */
@Path("start")
@Produces(MediaType.APPLICATION_JSON)
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

  @Path("foo")
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  PnkyPromise<String> getFooString();
}
