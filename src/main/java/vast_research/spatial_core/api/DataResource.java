package vast_research.spatial_core.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.Cache;
import org.jboss.resteasy.plugins.cache.server.ServerCache;

import vast_research.spatial_core.jobs.query.QueryResolution;
import vast_research.spatial_core.shared.Request;
import vast_research.spatial_core.shared.Zoom;

@Path("data")
public class DataResource {

	@GZIP
	@GET
	@Path("/{layerName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Cache(maxAge = 0)
	public Response getData(@Context final ServerCache cache,
			@PathParam("layerName") final String layerName,
			@QueryParam("zoom") final Integer zoom,
			@QueryParam("gridSize") final Long gridSize,
			@QueryParam("sw") final String sw, @QueryParam("ne") final String ne) {
		try {

			Integer checkedZoom = gridSize != null ? Zoom.getLevelByGridSize(
					gridSize).getZoom() : zoom;
			Request request = new Request(gridSize, ne, sw, checkedZoom,
					layerName);

			QueryResolution qr = new QueryResolution(request);
			return Response.ok(qr).build();
		} catch (Exception exception) {
			throw new WebApplicationException(exception);
		}

	}
}
