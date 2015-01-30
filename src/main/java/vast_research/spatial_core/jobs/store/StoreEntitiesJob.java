package vast_research.spatial_core.jobs.store;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vast_research.spatial_core.Server;
import vast_research.spatial_core.resources.DataStoreInfo;
import vast_research.spatial_core.resources.DataStoreManager;
import vast_research.spatial_core.shared.Entity;
import vast_research.spatial_core.shared.LayerResolution;

public class StoreEntitiesJob implements Callable<Boolean>, Serializable {

	private static final long serialVersionUID = 1L;

	private final List<Entity> entities;

	private final Logger logger = LoggerFactory.getLogger(Server.class);

	private final LayerResolution resolution;

	public StoreEntitiesJob(final List<Entity> entities,
			final LayerResolution resolution) {
		this.entities = new ArrayList<Entity>(entities);
		this.resolution = resolution;
	}

	@Override
	public Boolean call() throws Exception {
		boolean ret = false;

		logger.info("Starting Store Entities JOB.");
		try {
			// isto tá mal, só devia ser 1 ...
			// Connection connection = DataStoreManager.getConnection();
			Connection connection = DataStoreInfo.getConnection(resolution
					.getDataStoreURL());

			while (!DataStoreManager.tableExists(connection,
					resolution.getDataStoreTable())) {
				DataStoreManager.save(connection, resolution);
				DataStoreManager.createEntityTable(connection, resolution);
			}

			connection.setAutoCommit(false);
			DataStoreManager.save(entities, connection, resolution);
			logger.info("Saved Entities.");
			connection.commit();
			connection.close();
			ret = true;
		} catch (SQLException exception) {
			exception.printStackTrace();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		logger.info("Ending Store Entities JOB.");

		return ret;
	}

}
