package vast_research.spatial_core.jobs.synthesis;

import java.io.Serializable;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vast_research.spatial_core.Server;
import vast_research.spatial_core.resources.MonitorItem;
import vast_research.spatial_core.resources.Workers;
import vast_research.spatial_core.shared.LayerResolution;
import vast_research.spatial_core.shared.Zoom;

public class UpLoader implements Callable<Boolean>, Serializable {

	private static final long serialVersionUID = 1L;

	private final String externalFunctionsName;

	private final Logger logger = LoggerFactory.getLogger(Server.class);
	private LayerResolution resolution;

	// private final Long total;

	public UpLoader(final LayerResolution resolution) {
		// , final Long total) {
		this.resolution = resolution;
		// this.total = total;
		externalFunctionsName = null;
	}

	public UpLoader(final LayerResolution resolution,
			final String externalFunctionsName) {
		// , final Long total) {
		this.resolution = resolution;
		// this.total = total;
		this.externalFunctionsName = externalFunctionsName;
	}

	@Override
	public Boolean call() {
		long time = System.currentTimeMillis();

		MonitorItem op = new MonitorItem(MonitorItem.opType.UP,
				resolution.getName() + " - from RAW (-1) to 0 (grid:"
						+ resolution.getGridSize() + ")", time, -1L);
		Workers.init().getList("monitor").add(op);

		logger.info("Starting ups");

		Long currentGridSize = resolution.getGridSize();
		Zoom level = Zoom.getLevelByGridSize(currentGridSize);

		// então não preciso de fazer up neste nível, logo, vai para o
		// próximo
		if (currentGridSize.equals(level.getGridSize()))
			level = Zoom.getNextLevel(level);

		// TODO: external functions - ir buscar no batch loader pelo
		// nome da layer
		// ExternalFunctions extFunc = new TestExternalFunctions(resolution);
		// ExternalFunctions extFunc = new TweetsExternalFunctions(resolution);
		// ExternalFunctions extFunc = new
		// AccidentsExternalFunctions(resolution);
		try {
			// caso não tenha externalFunctionsName, usa o nome da resolution
			String resName = externalFunctionsName != null ? externalFunctionsName
					: resolution.getName();
			String clazzName = resName.toLowerCase();
			clazzName = Character.toString(clazzName.charAt(0)).toUpperCase()
					+ clazzName.substring(1);
			Class extFuncClass = Class
					.forName("vast_research.spatial_core.jobs.synthesis."
							+ clazzName + "ExternalFunctions");
			ExternalFunctions extFunc = (ExternalFunctions) extFuncClass
					.getConstructor(LayerResolution.class).newInstance(
							resolution);

			while (level.getZoom() >= 0) {
				logger.info("Up to level " + level.getZoom());

				MonitorItem opUP = new MonitorItem(MonitorItem.opType.UP,
						resolution.getName() + " - to level " + level.getZoom()
								+ " (grid:" + level.getGridSize() + ")", time,
						-1L);
				Workers.init().getList("monitor").add(opUP);

				// try {
				// estamos a ter timeouts, portanto vamos tirar isto
				// resolution = Workers.submit(new GoToJob(resolution, level,
				// extFunc)).get(3600,TimeUnit.SECONDS);
				GoToJob gtj = new GoToJob(resolution, level, extFunc);
				resolution = gtj.call();
				// } catch (InterruptedException exception) {
				// exception.printStackTrace();
				// } catch (ExecutionException exception) {
				// exception.printStackTrace();
				// }

				Workers.init().getList("monitor").remove(opUP);
				opUP.setEndTime(System.currentTimeMillis() - time);
				Workers.init().getList("monitor").add(opUP);

				Zoom nextLevel = Zoom.getNextLevel(level);
				if (nextLevel == null)
					break;
				level = nextLevel;
			}
			logger.info("Ending ups");
			logger.info("Ups Done in " + (System.currentTimeMillis() - time)
					/ 1000 + " seconds");

			Workers.init().getList("monitor").remove(op);
			op.setEndTime(System.currentTimeMillis() - time);
			Workers.init().getList("monitor").add(op);

			return true;

		} catch (Exception exp) {
			exp.printStackTrace();
			return false;
		}
	}
}
