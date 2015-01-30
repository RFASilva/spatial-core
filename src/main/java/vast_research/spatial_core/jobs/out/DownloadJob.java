package vast_research.spatial_core.jobs.out;

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

public class DownloadJob implements Callable<StringBuilder>, Serializable {

	private static final Integer BATCH_SIZE = Config.getConfigInt("batch_work");

	private static final Integer JOBS_SIZE = Config.getConfigInt("num_workers");

	private static final long serialVersionUID = 1L;

	// private final Writer writer;
	// private final String listRef;

	private final Logger logger = LoggerFactory.getLogger(Server.class);
	private final LayerResolution resolution;

	public DownloadJob(
	// final String listRef,
			final LayerResolution resolution) {
		this.resolution = resolution;
		// listRef = listRef;
	}

	@Override
	public StringBuilder call() {
		// boolean ret = false;
		StringBuilder ret = new StringBuilder();

		logger.info("Starting downloading JOBS for " + resolution.getName()
				+ " on zoom " + resolution.getZoom() + " on store "
				+ resolution.getDataStoreURL());

		try {
			List<long[]> upGEOHASHidx = DataStoreManager.getEntitiesIdxForGets(
					resolution, BATCH_SIZE);

			List<Future<StringBuilder>> futures = new LinkedList<Future<StringBuilder>>();

			long firstUPGEOHASH;
			long lastUPGEOHASH;

			for (long[] currentIT : upGEOHASHidx) {
				// offset = currentIT[0];
				firstUPGEOHASH = (int) currentIT[1];
				lastUPGEOHASH = currentIT[2];
				futures.add(Workers.submit(new ExecuteDownload(resolution,
						firstUPGEOHASH, lastUPGEOHASH)));

				// ret = true;

				if (futures.size() == JOBS_SIZE) {
					for (Future<StringBuilder> future : futures)
						ret.append(future.get());
					futures.clear();
				}
			}
			for (Future<StringBuilder> future : futures)
				ret.append(future.get());

			logger.info("Ending downloading JOBS.");

		} catch (InterruptedException exception) {
			exception.printStackTrace();
		} catch (ExecutionException exception) {
			exception.printStackTrace();
		}

		return ret;
	}
}
