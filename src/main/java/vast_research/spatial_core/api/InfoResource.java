package vast_research.spatial_core.api;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.Cache;
import org.jboss.resteasy.plugins.cache.server.ServerCache;

import vast_research.spatial_core.Server;
import vast_research.spatial_core.resources.DataStoreInfo;
import vast_research.spatial_core.resources.DataStoreManager;
import vast_research.spatial_core.resources.MonitorItem;
import vast_research.spatial_core.shared.LayerResolution;

@Path("info")
public class InfoResource {

	public class InfoResponse implements StreamingOutput {

		private final String requestedLayer;

		public InfoResponse(final String layerName) {
			super();
			requestedLayer = layerName;
		}

		@Override
		public void write(final OutputStream os) throws IOException,
				WebApplicationException {

			long time = System.currentTimeMillis();
			MonitorItem op = new MonitorItem(MonitorItem.opType.INFO,
					requestedLayer, time, -1L);
			Server.monitor.add(op);

			Writer writer = new BufferedWriter(new OutputStreamWriter(os));

			String ret = null;

			// gets the meta-store connection ...
			Connection conn = DataStoreInfo.getMetaStore();

			// queries the meta-store for all layers with that name (zoom = null
			// ==
			// all zooms)
			LayerResolution[] allRes = DataStoreManager.getLayersTables(conn,
					requestedLayer, null);

			if (allRes.length > 0)
				ret = "[";

			int i = 0;
			for (LayerResolution res : allRes) {
				ret += "{" + "\"gridSize\":" + res.getGridSize()
						+ ",\"totalElements\":" + res.getTotalCount()
						+ ",\"NSyntheses\":" + res.getSynthCount()
						+ ",\"NSingularElements\":" + res.getSingularCount()
						+ ",\"reductionRate\":" + res.getReduction()
						+ ",\"zoom\":"
						+ (res.getZoom() == -1 ? null : res.getZoom())
						+ ",\"processedTime\":" + res.getProcessedTime() + "}";
				i++;
				if (i != allRes.length)
					ret += ",";
			}
			ret += "]";

			System.out.println(ret);

			writer.write(ret);

			writer.flush();
			writer.close();

			Server.monitor.remove(op);
			op.setEndTime(System.currentTimeMillis() - time);
			Server.monitor.add(op);
		}
	}

	@GZIP
	@GET
	@Path("/{layerName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Cache(maxAge = 0)
	public Response getInfos(@Context final HttpServletRequest req,
			@Context final ServerCache cache,
			@PathParam("layerName") final String layerName) {
		try {
			// req.getPathInfo())
			InfoResponse info = new InfoResponse(layerName);
			return Response.ok(info).build();
		} catch (Exception exception) {
			throw new WebApplicationException(exception);
		}
	}

}
