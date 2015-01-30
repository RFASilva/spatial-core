package vast_research.spatial_core.api;

import java.io.IOException;
import java.sql.Connection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.GZIP;

import vast_research.spatial_core.jobs.synthesis.UpLoader;
import vast_research.spatial_core.resources.DataStoreInfo;
import vast_research.spatial_core.resources.DataStoreManager;
import vast_research.spatial_core.resources.Workers;
import vast_research.spatial_core.shared.LayerResolution;

@Path("/executeup")
public class ExecuteUpResource {

	@GZIP
	@GET
	@Path("/{layerName}")
	public Response ExecuteUp(@PathParam("layerName") final String layerName,
			@QueryParam("externalFunctions") final String externalFunctionsName)
			throws IOException {
		try {
			Connection connection = DataStoreInfo.getMetaStore();
			LayerResolution[] parts = DataStoreManager.getLayersTables(
					connection, layerName, "-1");
			connection.close();
			Workers.submit(new UpLoader(parts[0], externalFunctionsName));
			return Response.ok().build();
		} catch (Exception exception) {
			throw new WebApplicationException(exception);
		}

	}
}
