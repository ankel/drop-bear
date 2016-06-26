package ankel.dropbear;

import lombok.RequiredArgsConstructor;
import rx.Observable;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * @author Binh Tran
 */
@Path("start")
@Produces(MediaType.APPLICATION_JSON)
public interface RestInterface {
  @Path("{p1}/{p2}")
  @GET
  String getFooWithParams(
      @PathParam("p1") final String p1,
      @PathParam("p2") final Integer p2,
      @QueryParam("q1") final String q1,
      @QueryParam("q2") final Long q2);

  @Path("foo")
  @GET
  Observable<Map<String, String>> getFoo();

  @Path("foo")
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  Observable<String> getFooString();

  @Path("foo")
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  Observable<Map<String, String>> postFoo(final long value);

  @Path("foo")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.TEXT_PLAIN)
  Observable<Void> putStringReturnVoid(final String value);

  @Path("foo")
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  Observable<Void> postObjectReturnVoid(final RequestObject value);

  @Path("foo")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  Observable<Map<String, String>> putObject(final RequestObject value);

  @Path("foo")
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  Observable<ResponseObject> postForm(@FormParam("id") final String id);


  @Path("foo")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  Observable<ResponseObject> putForm(@FormParam("id") final String id);


  @RequiredArgsConstructor
  class RequestObject {
    public final String id;
    public final String requestType;
  }

  @RequiredArgsConstructor
  class ResponseObject {
    public final String id;
  }
}
