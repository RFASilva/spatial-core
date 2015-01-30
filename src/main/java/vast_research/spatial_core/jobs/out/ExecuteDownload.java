package vast_research.spatial_core.jobs.out;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vast_research.spatial_core.Server;
import vast_research.spatial_core.resources.DataStoreManager;
import vast_research.spatial_core.shared.Entity;
import vast_research.spatial_core.shared.LayerResolution;

public class ExecuteDownload implements Callable<StringBuilder>, Serializable {

	private static final long serialVersionUID = 1L;

	private final long first;
	private final long last;
	// private final Writer writer;
	// private final String listRef;
	private final Logger logger = LoggerFactory.getLogger(Server.class);
	private final LayerResolution resolution;

	public ExecuteDownload(
	// final String listRef,
			final LayerResolution resolution, final long first, final long last) {
		// listRef = listRef;
		this.resolution = resolution;
		this.first = first;
		this.last = last;
	}

	@Override
	public StringBuilder call() throws Exception {
		StringBuilder ret = new StringBuilder();

		logger.info("Starting getting & generating CSV entities JOB [" + first
				+ " - " + last + "]");

		try {
			List<Entity> entities = DataStoreManager.getEntities(resolution,
					first, last);

			for (Entity ent : entities) {
				// aqui
				// writer.write(ent.toCSV() + "\n");
				// outOutput.add(ent.toCSV() + "\n");
				ret.append(ent.toCSV());
				ret.append("\n");
			}

		} catch (Exception exception) {
			exception.printStackTrace();
		}
		// writer.flush();

		logger.info("Ending getting & generating CSV entities JOB.");

		return ret;
	}

}
