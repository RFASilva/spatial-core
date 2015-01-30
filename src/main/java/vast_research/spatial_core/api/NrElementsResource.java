package vast_research.spatial_core.api;

import java.sql.Connection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.GZIP;

import vast_research.spatial_core.resources.DataStoreInfo;
import vast_research.spatial_core.resources.DataStoreManager;
import vast_research.spatial_core.shared.LayerResolution;
import vast_research.spatial_core.shared.Request;
import vast_research.spatial_core.shared.Zoom;

@Path("nrelements")
public class NrElementsResource {

	@GZIP
	@GET
	@Path("/{layerName}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getNrElements(@PathParam("layerName") final String layerName,
			@QueryParam("sw") final String sw,
			@QueryParam("ne") final String ne,
			@QueryParam("gridSize") final Long gridSize) {
		Request request = new Request(gridSize, ne, sw, null, layerName);

		Integer zoom = Zoom.getLevelByGridSize(gridSize).getZoom();

		// gets the meta-store connection ...
		Connection conn = DataStoreInfo.getMetaStore();
		// queries the meta-store for layer with name and zoom

		LayerResolution[] allRes = DataStoreManager.getLayersTables(conn,
				layerName, zoom.toString());

		// LayerResolution[] allRes = DataStoreManager.getLayersTables(conn,
		// layerName, "-1");

		LayerResolution resolution = allRes[0];

		Connection connection = DataStoreInfo.getConnection(resolution
				.getDataStoreURL());
		Long nrElems = DataStoreManager.countElems(connection, resolution,
				request.getEnvelope());

		return "{\"nspatialobjects\":" + nrElems + "}";
	}
}
