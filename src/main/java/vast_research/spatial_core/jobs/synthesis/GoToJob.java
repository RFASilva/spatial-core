package vast_research.spatial_core.jobs.synthesis;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vast_research.spatial_core.Server;
import vast_research.spatial_core.resources.Config;
import vast_research.spatial_core.resources.DataStoreInfo;
import vast_research.spatial_core.resources.DataStoreManager;
import vast_research.spatial_core.resources.Workers;
import vast_research.spatial_core.shared.LayerResolution;
import vast_research.spatial_core.shared.Zoom;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;

public class GoToJob implements Callable<LayerResolution>, Serializable {

	private static final Integer JOBS_SIZE = Config
			.getConfigInt("goto_num_workers");

	private static final long serialVersionUID = 1L;

	private static final Integer WORK_BATCH = Config
			.getConfigInt("goto_batch_work");

	private final ExternalFunctions extFunctions;

	private final Logger logger = LoggerFactory.getLogger(Server.class);

	private final Zoom nextLevel;

	private final LayerResolution resolution;

	public GoToJob(final LayerResolution resolution, final Zoom nextLevel,
			final ExternalFunctions extFunc) {
		this.resolution = resolution;
		this.nextLevel = nextLevel;
		extFunctions = extFunc;
	}

	@Override
	public LayerResolution call() {
		long time = System.currentTimeMillis();

		Envelope envelope = new Envelope();

		logger.info("Starting GOTO");

		logger.info(resolution.toString());
		logger.info(nextLevel.toString());

		// TODO: precision muda conforme os pontos - é preciso calcular
		// criamos já a nova layer
		LayerResolution result = new LayerResolution(resolution.getName(),
				resolution.getPrecision(), resolution.getProperties(),
				nextLevel.getZoom());
		result.setGridSize(nextLevel.getGridSize());

		logger.info(result.toString());

		// hum, isto não faz sentido para já ...
		// for (Connection connection : DataStoreInfo.getDataStores())
		try {
			Boolean first = true;

			// ISTO TÁ MAL!!
			// resolution.setDataStoreURL(connection.getMetaData().getURL());
			// Long count = DataStoreManager.count(resolution);
			// Esta é melhor!
			result.setDataStoreURL(resolution.getDataStoreURL());

			List<Future<Envelope>> futures = new LinkedList<Future<Envelope>>();

			List<long[]> upGEOHASHidx = DataStoreManager.GetEntitiesIdxForUps(
					resolution, WORK_BATCH);

			logger.info("Starting work ...");

			// drop table aqui se já existir
			if (DataStoreManager.tableExists(result))
				DataStoreManager.tableDrop(result);

			long offset;
			long firstUPGEOHASH;
			long lastUPGEOHASH;
			for (long[] currentIT : upGEOHASHidx) {
				offset = currentIT[0];
				firstUPGEOHASH = (int) currentIT[1];
				lastUPGEOHASH = currentIT[2];
				if (first) {
					logger.info("Creating execution job [ " + offset + " - "
							+ (offset + WORK_BATCH) + "]");
					Envelope tmpEnvelope = Workers.submit(
							new ExecuteGoTo(nextLevel, firstUPGEOHASH,
									lastUPGEOHASH, resolution, result,
									extFunctions)).get(3600, TimeUnit.SECONDS);

					envelope.expandToInclude(tmpEnvelope);
					// maxPrecision = maxPrecision < (Double) ret[1] ?
					// maxPrecision
					// : (Double) ret[1];
					first = false;
				} else {
					logger.info("Preparing execution job [ " + offset + " - "
							+ (offset + WORK_BATCH) + "]");
					futures.add(Workers.submit(new ExecuteGoTo(nextLevel,
							firstUPGEOHASH, lastUPGEOHASH, resolution, result,
							extFunctions)));
				}

				if (futures.size() == JOBS_SIZE) {
					for (Future<Envelope> future : futures) {
						Envelope tmpEnvelope = future.get(3600,
								TimeUnit.SECONDS);
						envelope.expandToInclude(tmpEnvelope);
						// maxPrecision = maxPrecision < (Double) ret[1] ?
						// maxPrecision
						// : (Double) ret[1];
					}
					futures.clear();
				}
			}

			for (Future<Envelope> future : futures) {
				Envelope tmpEnvelope = future.get(3600, TimeUnit.SECONDS);
				envelope.expandToInclude(tmpEnvelope);
				// maxPrecision = maxPrecision < (Double) ret[1] ?
				// maxPrecision
				// : (Double) ret[1];
			}

			// } catch (SQLException exception) {
			// exception.printStackTrace();
		} catch (InterruptedException exception) {
			exception.printStackTrace();
		} catch (ExecutionException exception) {
			exception.printStackTrace();
		} catch (Exception exception) {
			exception.printStackTrace();
		}

		DataStoreManager.vacuumAnalyze(result);

		// faz sentido recalcular novamente o envelope? - se calhar não
		result.setEnvelope(new GeometryFactory().toGeometry(envelope));
		// result.setEnvelope(resolution.getEnvelope());

		result.setPrecision(resolution.getPrecision()
				* resolution.getGridSize() / result.getGridSize());

		try {
			// save metada related to layer ...
			result.setTotalCount(DataStoreManager.countElems(
					DataStoreInfo.getConnection(result.getDataStoreURL()),
					result));
			result.setSingularCount(DataStoreManager.countSingulars(
					DataStoreInfo.getConnection(result.getDataStoreURL()),
					result));
			result.setSynthCount(DataStoreManager.countSynths(
					DataStoreInfo.getConnection(result.getDataStoreURL()),
					result));

			if (resolution != null) {
				double tmp = 1.0 - (double) result.getTotalCount()
						/ resolution.getTotalCount();
				result.setReduction(tmp);
			}

			long timeSpent = System.currentTimeMillis() - time;
			result.setProcessedTime(timeSpent);
			logger.info("Ending GOTO - done in " + timeSpent / 1000
					+ " seconds");

			Connection connection = DataStoreInfo.getMetaStore();
			DataStoreManager.save(connection, result);
			connection.close();
		} catch (SQLException exception) {
			exception.printStackTrace();
		}

		return result;
	}
}
