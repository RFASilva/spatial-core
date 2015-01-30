package vast_research.spatial_core.jobs.query;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vast_research.spatial_core.Server;
import vast_research.spatial_core.resources.Config;
import vast_research.spatial_core.resources.DataStoreManager;
import vast_research.spatial_core.resources.Workers;
import vast_research.spatial_core.shared.LayerResolution;

import com.vividsolutions.jts.geom.Envelope;

public class QueryJob implements Callable<StringBuilder>, Serializable {

	private static final Integer JOBS_SIZE = Config.getConfigInt("num_workers");

	private static final long serialVersionUID = 1L;

	private static final Integer WORK_BATCH = Config.getConfigInt("batch_work");

	private final Envelope envelope;

	private final Logger logger = LoggerFactory.getLogger(Server.class);
	private final LayerResolution resolution;

	public QueryJob(final Envelope envelope, final LayerResolution resolution) {
		this.envelope = envelope;
		this.resolution = resolution;
	}

	@Override
	public StringBuilder call() throws Exception {
		StringBuilder ret = new StringBuilder();

		logger.info("Starting getting (query) JOBS for " + resolution.getName()
				+ " on zoom " + resolution.getZoom() + " on store "
				+ resolution.getDataStoreURL() + " for bbox: "
				+ envelope.toString());

		// String header = "{\"type\": \"FeatureCollection\",\"features\": [";
		String header = "{\"props\": [\"Attribute\",\"Count\",\""
				+ resolution
						.getProperties()
						.toString()
						.substring(
								1,
								resolution.getProperties().toString().length() - 1)
						.replace(", ", "\",\"") + "\"],\"points\": [";

		// writer.write(header);
		// writer.flush();
		ret.append(header);

		Long time = System.currentTimeMillis();

		try {
			List<Future<StringBuilder>> futures = new LinkedList<Future<StringBuilder>>();

			List<long[]> upGEOHASHidx = DataStoreManager.getEntitiesIdxForGets(
					resolution, WORK_BATCH);

			logger.info("Getting ENTITIES IDX done in "
					+ (System.currentTimeMillis() - time) + "ms.");

			long firstUPGEOHASH;
			long lastUPGEOHASH;

			for (long[] currentIT : upGEOHASHidx) {
				// offset = currentIT[0];
				firstUPGEOHASH = (int) currentIT[1];
				lastUPGEOHASH = currentIT[2];

				futures.add(Workers.submit(new ExecuteQuery(firstUPGEOHASH,
						lastUPGEOHASH, resolution, envelope)));

				// test para apenas 1 query
				// futures.add(Workers.submit(new ExecuteQuery(0L, 0L,
				// resolution,
				// envelope)));

				if (futures.size() == JOBS_SIZE) {
					for (Future<StringBuilder> future : futures)
						ret.append(future.get().toString());
					futures.clear();
				}

			}

			// última volta
			for (Future<StringBuilder> future : futures)
				ret.append(future.get().toString());

			logger.info("Ending getting JOBS.");

		} catch (InterruptedException exception) {
			exception.printStackTrace();
		} catch (ExecutionException exception) {
			exception.printStackTrace();
		} catch (Exception exception) {
			exception.printStackTrace();
		}

		ret.append("{}]}");

		logger.info("Getting TOTAL JOB done in "
				+ (System.currentTimeMillis() - time) + "ms.");

		return ret;
	}
}
