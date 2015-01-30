package vast_research.spatial_core.jobs.store;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvMapReader;
import org.supercsv.prefs.CsvPreference;

import vast_research.spatial_core.Server;
import vast_research.spatial_core.resources.Config;
import vast_research.spatial_core.resources.DataStoreInfo;
import vast_research.spatial_core.resources.DataStoreManager;
import vast_research.spatial_core.resources.MonitorItem;
import vast_research.spatial_core.resources.Workers;
import vast_research.spatial_core.shared.Entity;
import vast_research.spatial_core.shared.Functions;
import vast_research.spatial_core.shared.GranularSynthesis;
import vast_research.spatial_core.shared.LayerResolution;
import vast_research.spatial_core.shared.SpatialObject;
import vast_research.spatial_core.shared.SpatialType;
import vast_research.spatial_core.shared.Zoom;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKTReader;

public class BatchLoader {

	private static final Integer BATCH_SIZE = Config.getConfigInt("batch_work");

	private static final Integer JOBS_SIZE = Config.getConfigInt("num_workers");

	private Envelope envelope;

	private GeometryFactory geofact;

	private Long gridSize;

	private String[] headers;

	private final Logger logger = LoggerFactory.getLogger(Server.class);

	private boolean longLat = true;

	private Double precision;

	private List<String> properties;

	private Long total = 0L;

	private void expand(final Envelope envelope, final Geometry geometry) {
		for (Coordinate coordinate : geometry.getCoordinates())
			envelope.expandToInclude(coordinate);
	}

	public void load(final String layerName, final File file) throws Exception {
		Long time = System.currentTimeMillis();

		MonitorItem op = new MonitorItem(MonitorItem.opType.UPLOAD, layerName,
				time, -1L);
		Server.monitor.add(op);

		logger.info("Starting Load " + layerName + " in  file "
				+ file.getName());

		BufferedReader readerAux = Files.newReader(file, Charsets.UTF_8);
		loadDummyHeader(readerAux);

		// TODO: considerar remover este passo inicial e só fazer a escrita do
		// up no final quando temos todas as entidades na BD.
		// este passo consome, no MBP do RPL - 18 segundos - numa BD é mais
		// rápido?
		CsvMapReader csvReader = new CsvMapReader(readerAux,
				CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE);
		envelope = new Envelope();
		double bestPrecision = 1;
		while (true) {
			Map<String, String> row = csvReader.read(headers);
			if (row == null)
				break;
			else {
				Geometry geom = parseCoordsRow(row);
				bestPrecision = Functions
						.findBestPrecision(bestPrecision, geom);
				expand(envelope, geom);
			}
		}
		logger.info("MIN/MAX envelope and precision obtained.");

		BufferedReader reader = Files.newReader(file, Charsets.UTF_8);
		loadFileHeader(reader);

		// usa-se a precisão computada anteriormente junto com o envelope.
		precision = precision > bestPrecision ? precision : bestPrecision;

		LayerResolution resolution = new LayerResolution(layerName, precision,
				properties, -1);

		// AQUI!!! decides which store for this dataset - REVER ISTO!!
		Connection conn = DataStoreManager.getConnection();
		resolution.setDataStoreURL(conn.getMetaData().getURL());
		conn.close();

		resolution.setEnvelope(new GeometryFactory().toGeometry(envelope));
		gridSize = Functions.computeGridSize(resolution.getMinCoordinate(),
				resolution.getMaxCoordinate(), precision);

		total = 0L;
		System.out.println("GRIDSIZE: " + gridSize);

		resolution.setGridSize(gridSize);
		csvReader.close();

		logger.info("Finished 1st pass (find min/max envelope & precision) "
				+ (System.currentTimeMillis() - time) / 1000 + " seconds");

		csvReader = new CsvMapReader(reader,
				CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE);

		List<Entity> entities = new LinkedList<Entity>();
		List<Future<Boolean>> futures = new LinkedList<Future<Boolean>>();

		// drop table aqui se já existir
		if (DataStoreManager.tableExists(resolution))
			DataStoreManager.tableDrop(resolution);

		logger.info("Starting Reading CSV.");
		int oldcount = 0;
		int newcount = 0;
		Boolean first = true;
		Boolean hasRow = true;
		while (hasRow) {
			Map<String, String> row = csvReader.read(headers);
			newcount++;

			if (row != null) {
				Entity entity = parseRow(row);
				entities.add(entity);
			} else
				hasRow = false;

			if (entities.size() == BATCH_SIZE || hasRow == false) {
				logger.info("Submitted Job [" + oldcount + " - " + newcount
						+ "]");
				if (first) {
					Workers.submit(new StoreEntitiesJob(entities, resolution))
							.get(3600, TimeUnit.SECONDS);
					first = false;
				} else
					futures.add(Workers.submit(new StoreEntitiesJob(entities,
							resolution)));
				oldcount = newcount;
				entities.clear();
			}

			if (futures.size() == JOBS_SIZE) {
				logger.info("Job limit reached - waiting for finish!");
				for (Future<Boolean> future : futures)
					future.get(3600, TimeUnit.SECONDS);
				futures.clear();
			}

			//
			// if (row == null) {
			// logger.info("Submitted (last) Job [" + oldcount + " - "
			// + newcount + "]");
			// futures.add(Workers.submit(new StoreEntitiesJob(entities,
			// resolution)));
			// hasRow = false;
			// } else {
			// Entity entity = parseRow(row);
			// entities.add(entity);
			// if (entities.size() == BATCH_SIZE) {
			// logger.info("Submitted Job [" + oldcount + " - " + newcount
			// + "]");
			// if (first) {
			// Workers.submit(
			// new StoreEntitiesJob(entities, resolution))
			// .get(3600, TimeUnit.SECONDS);
			// first = false;
			// } else
			// futures.add(Workers.submit(new StoreEntitiesJob(
			// entities, resolution)));
			// oldcount = newcount;
			// entities.clear();
			// }
			//
			// if (futures.size() == JOBS_SIZE) {
			// logger.info("Job limit reached - waiting for finish!");
			// for (Future<Boolean> future : futures)
			// future.get(3600, TimeUnit.SECONDS);
			// futures.clear();
			// }
			// }
		}
		logger.info("End Reading CSV.");
		for (Future<Boolean> future : futures)
			future.get(3600, TimeUnit.SECONDS);

		csvReader.close();
		reader.close();
		file.delete();

		resolution.setTotalCount(DataStoreManager.countElems(
				DataStoreInfo.getConnection(resolution.getDataStoreURL()),
				resolution));
		resolution.setSingularCount(DataStoreManager.countSingulars(
				DataStoreInfo.getConnection(resolution.getDataStoreURL()),
				resolution));
		resolution.setSynthCount(DataStoreManager.countSynths(
				DataStoreInfo.getConnection(resolution.getDataStoreURL()),
				resolution));

		Long timeSpent = System.currentTimeMillis() - time;
		resolution.setProcessedTime(timeSpent);
		logger.info("BatchLoader.load done in " + timeSpent / 1000 + " seconds");

		DataStoreManager.save(
				DataStoreInfo.getConnection(resolution.getDataStoreURL()),
				resolution);

		// fazer um vacuum
		DataStoreManager.vacuumAnalyze(resolution);

		// Nota: demasiado longo para ser assim ...
		// Workers.submit(new UpLoader(resolution));
		// UpLoader up = new UpLoader(resolution);
		// up.call();

		Server.monitor.remove(op);
		op.setEndTime(System.currentTimeMillis() - time);
		Server.monitor.add(op);
	}

	private void loadDummyHeader(final BufferedReader reader)
			throws IOException {
		while (true) {
			String line = reader.readLine();

			// TODO: e o resto dos headers?

			if (line.contains("coordinates: ")) {
				String coordsTypes = line.substring(
						new String("coordinates: ").length(), line.length());
				if (coordsTypes.equals("latLong"))
					longLat = false;
			}

			if (line.equals("#DATA#"))
				break;
		}

		// Mexidas na precisão serão aqui ...
		String precStr = reader.readLine().split(";")[1];
		precision = new Double(precStr);
		geofact = new GeometryFactory(new PrecisionModel(), 4326);

		headers = reader.readLine().split(";");
		logger.info("File Meta Data Parsed.");
	}

	private void loadFileHeader(final BufferedReader reader) throws IOException {
		while (true) {
			String line = reader.readLine();

			// TODO: e o resto dos headers?

			if (line.contains("coordinates: ")) {
				String coordsTypes = line.substring(
						new String("coordinates: ").length(), line.length());
				if (coordsTypes.equals("latLong"))
					longLat = false;
			}

			if (line.equals("#DATA#"))
				break;
		}
		precision = new Double(reader.readLine().split(";")[1]);
		headers = reader.readLine().split(";");
		properties = Arrays.asList(Arrays.copyOfRange(headers, 5,
				headers.length));
		logger.info("File Meta Data Loaded.");
	}

	private Geometry parseCoordsRow(final Map<String, String> row)
			throws Exception {
		String coords = row.get("SpatialExp.WKT");

		// Parece-me que só consegue carregar pontos (NOTA RS)

		if (!longLat) {
			String[] part = coords.trim().replaceAll(" +", " ")
					.split("[\\s()]");
			coords = part[0] + "(" + part[2] + " " + part[1] + ")";
		}

		Geometry geometry = new WKTReader(geofact).read(coords);
		return geometry;
	}

	private Entity parseRow(final Map<String, String> row) throws Exception {
		String coords = row.get("SpatialExp.WKT");

		if (!longLat) {
			String[] part = coords.trim().replaceAll(" +", " ")
					.split("[\\s()]");
			coords = part[0] + "(" + part[2] + " " + part[1] + ")";
		}

		Geometry geometry = new WKTReader(geofact).read(coords);
		SpatialType type = SpatialType.valueOf(row.get("SpatialType"));
		String id = row.get("ID");
		String attribute = row.get("Atrb");
		Map<String, Double> properties = new HashMap<String, Double>();
		for (String property : this.properties)
			properties.put(property, new Double(row.get(property)));

		Integer upGeoHash;
		Geometry upGeometry;
		if (gridSize == null) {
			upGeoHash = 0;
			upGeometry = null;
		} else {
			// tá mal ... é para o próximo ...
			// Long nextGridSize =
			// Zoom.getLevelByGridSize(gridSize).getGridSize();
			// Este computa para o próximo
			Long nextGridSize = Zoom.getNextLevel(
					Zoom.getLevelByGridSize(gridSize)).getGridSize();

			Double x = envelope.getMinX();
			Double y = envelope.getMinY();
			upGeometry = Functions.convertToUp(geometry, new Coordinate(x, y),
					precision, gridSize, nextGridSize);
			upGeoHash = upGeometry.hashCode();
		}

		switch (type) {
		case GranularSynthesis:
			Long count = new Long(row.get("Count"));
			total += count;
			return new GranularSynthesis(attribute, count, geometry, id,
					properties, upGeoHash, upGeometry);
		default:
			total++;
			return new SpatialObject(attribute, geometry, id, properties, type,
					upGeoHash, upGeometry);
		}
	}
}
