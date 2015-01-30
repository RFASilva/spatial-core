package vast_research.spatial_core.api;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.GZIP;

import vast_research.spatial_core.jobs.out.DownloadResolution;

@Path("/download")
public class DownloadResource {

	@GZIP
	@GET
	@Path("/{layerName}/{zoom}")
	@Produces("text/csv")
	public Response downloadFile(
			@PathParam("layerName") final String layerName,
			@PathParam("zoom") final Integer zoomLevel) throws IOException {
		try {
			DownloadResolution downres = new DownloadResolution(layerName,
					zoomLevel);
			return Response.ok(downres).build();
		} catch (Exception exception) {
			throw new WebApplicationException(exception);
		}
	}
}