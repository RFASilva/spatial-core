package vast_research.spatial_core.jobs.out;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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

public class DownloadResolution implements StreamingOutput {

	private final Logger logger = LoggerFactory.getLogger(Server.class);

	private final String requestedLayer;
	private final Integer zoomLevel;

	public DownloadResolution(final String layerName, final Integer zoomLevel) {
		super();
		this.zoomLevel = zoomLevel;
		requestedLayer = layerName;
	}

	@Override
	public void write(final OutputStream os) throws IOException,
			WebApplicationException {

		long time = System.currentTimeMillis();
		MonitorItem op = new MonitorItem(MonitorItem.opType.DOWNLOAD,
				requestedLayer + " - " + zoomLevel, time, -1L);
		Server.monitor.add(op);

		logger.info("Starting download (out) of " + requestedLayer
				+ " on level " + zoomLevel + ".");

		Writer writer = new BufferedWriter(new OutputStreamWriter(os));

		try {
			LayerResolution[] resolutions = DataStoreManager.getLayersTables(
					DataStoreInfo.getMetaStore(), requestedLayer,
					zoomLevel.toString());

			LayerResolution requestedLayer = resolutions[0];

			List<Future<StringBuilder>> futures = new LinkedList<Future<StringBuilder>>();

			// starts generating header ...
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Date date = new Date();

			String propsTitles = "";
			for (String prop : requestedLayer.getProperties())
				propsTitles += ";" + prop;

			String header = "Name: " + requestedLayer.getName() + "\n"
					+ "GridSize: " + requestedLayer.getGridSize().toString()
					+ "\n" + "ZoomLevel: "
					+ requestedLayer.getZoom().toString() + "\n" + "Data: "
					+ dateFormat.format(date) + "\n\n" + "#DATA#\n"
					+ "Precision;" + requestedLayer.getPrecision() + "\n"
					+ "ID;SpatialType;SpatialExp.WKT;Atrb;Count" + propsTitles
					+ "\n";

			writer.write(header);
			writer.flush();

			for (LayerResolution resolution : resolutions)
				futures.add(Workers.submit(new DownloadJob(resolution)));

			for (Future<StringBuilder> future : futures)
				writer.write(future.get().toString());

		} catch (InterruptedException exception) {
			exception.printStackTrace();
		} catch (ExecutionException exception) {
			exception.printStackTrace();
		}

		logger.info("End download (out).");
		writer.flush();
		writer.close();

		Server.monitor.remove(op);
		op.setEndTime(System.currentTimeMillis() - time);
		Server.monitor.add(op);
	}
}
