package vast_research.spatial_core.jobs.query;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vast_research.spatial_core.Server;
import vast_research.spatial_core.resources.DataStoreInfo;
import vast_research.spatial_core.resources.DataStoreManager;
import vast_research.spatial_core.resources.MonitorItem;
import vast_research.spatial_core.resources.Workers;
import vast_research.spatial_core.shared.LayerResolution;
import vast_research.spatial_core.shared.Request;

public class QueryResolution implements StreamingOutput {

	private final Logger logger = LoggerFactory.getLogger(Server.class);

	private final Request request;

	public QueryResolution(final Request request) {
		this.request = request;
	}

	@Override
	public void write(final OutputStream os) throws IOException,
			WebApplicationException {

		long time = System.currentTimeMillis();
		MonitorItem op = new MonitorItem(MonitorItem.opType.GET,
				request.getLayerName() + " - " + request.getZoom() + " - "
						+ request.getEnvelope().toString(), time, -1L);
		Server.monitor.add(op);

		logger.info("Starting getting (query) of " + request.getLayerName()
				+ " on level " + request.getZoom() + " for bbox: "
				+ request.getEnvelope().toString());

		Writer writer = new BufferedWriter(new OutputStreamWriter(os), 1048576);

		try {
			boolean ret = true;

			 LayerResolution[] resolutions = DataStoreManager.getLayersTables(
			 DataStoreInfo.getMetaStore(), request.getLayerName(),
			 request.getZoom().toString());
			
//			LayerResolution[] resolutions = DataStoreManager.getLayersTables(
//					DataStoreInfo.getMetaStore(), request.getLayerName(), "-1");

			if (resolutions.length == 0)
				// não tenho esse nível de zoom, então retorna RAW
				resolutions = DataStoreManager.getLayersTables(
						DataStoreInfo.getMetaStore(), request.getLayerName(),
						"-1");

			List<Future<StringBuilder>> futures = new LinkedList<Future<StringBuilder>>();

			for (LayerResolution resolution : resolutions)
				futures.add(Workers.submit(new QueryJob(request.getEnvelope(),
						resolution)));

			for (Future<StringBuilder> future : futures) {
				// ret = ret && future.get();
				writer.write(future.get().toString());
				writer.flush();
			}

		} catch (InterruptedException exception) {
			exception.printStackTrace();
		} catch (ExecutionException exception) {
			exception.printStackTrace();
		}

		logger.info("End getting (query).");
		writer.flush();
		writer.close();

		Server.monitor.remove(op);
		op.setEndTime(System.currentTimeMillis() - time);
		Server.monitor.add(op);
	}
}
