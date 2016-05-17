package ankel.dropbear;

import java.util.Map;

import javax.print.attribute.standard.Media;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import com.jive.foss.pnky.PnkyPromise;
import lombok.RequiredArgsConstructor;

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

  @Path("foo")
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  PnkyPromise<Map<String, String>> postFoo(final long value);

  @Path("foo")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.TEXT_PLAIN)
  PnkyPromise<Void> putStringReturnVoid(final String value);

  @Path("foo")
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  PnkyPromise<Void> postObjectReturnVoid(final RequestObject value);

  @Path("foo")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  PnkyPromise<Map<String, String>> putObject(final RequestObject value);

  @RequiredArgsConstructor
  public static class RequestObject
  {
    public final String id;
    public final String requestType;
  }
}
