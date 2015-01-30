package vast_research.spatial_core.resources;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import vast_research.spatial_core.shared.Entity;
import vast_research.spatial_core.shared.Functions;
import vast_research.spatial_core.shared.GranularSynthesis;
import vast_research.spatial_core.shared.LayerResolution;
import vast_research.spatial_core.shared.SpatialObject;
import vast_research.spatial_core.shared.SpatialType;
import vast_research.spatial_core.shared.Zoom;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKTReader;

public final class DataStoreManager {

	private static final long BATCHINSERT_SIZE = Config
			.getConfigInt("insert_chunk_size");

	private static final String EXISTS_TABLE = "select * from information_schema.tables where table_name = ?";

	private static final int FETCH_SIZE = Config.getConfigInt("fetch_size");

	private static Integer index = 0;

	// private static final String INSERT_LAYER_RES =
	// "INSERT INTO layer_resolution (_data_store_table,_data_store_url,_name,_precision,_properties,_zoom,_envelope,_grid_size,_total_count,_singular_count,_synth_count, _reduction, _processed_time) VALUES (?,?,?,?,?,?,ST_SnapToGrid(ST_GeomFromText(?),0.000000000001),?,?,?,?,?,?)";
	private static final String INSERT_LAYER_RES = "INSERT INTO layer_resolution (_data_store_table,_data_store_url,_name,_precision,_properties,_zoom,_envelope,_grid_size,_total_count,_singular_count,_synth_count, _reduction, _processed_time) VALUES (?,?,?,?,?,?,ST_GeomFromText(?),?,?,?,?,?,?)";

	private static final String SELECT_LAYER_RES = "SELECT * FROM layer_resolution WHERE _data_store_table=? AND _data_store_url=?";

	// private static final String SELECT_LAYER_RES_BY_NAME =
	// "SELECT _pk,_data_store_url,_data_store_table,ST_AsText(ST_SnapToGrid(_envelope,0.000000000001)) as geometry,_grid_size,_name,_precision,_properties,_zoom,_total_count,_singular_count,_synth_count, _reduction, _processed_time FROM layer_resolution WHERE _name=?";
	private static final String SELECT_LAYER_RES_BY_NAME = "SELECT _pk,_data_store_url,_data_store_table,ST_AsText(_envelope) as geometry,_grid_size,_name,_precision,_properties,_zoom,_total_count,_singular_count,_synth_count, _reduction, _processed_time FROM layer_resolution WHERE _name=?";

	// private static final String UPDATE_LAYER_RES =
	// "UPDATE layer_resolution SET _data_store_table =?,_data_store_url=?,_name=?,_precision=?,_properties=?,_zoom=?,_envelope=ST_SnapToGrid(ST_GeomFromText(?),0.000000000001),_grid_size=?, _total_count=?,_singular_count=?,_synth_count=?, _reduction=?, _processed_time=? WHERE _pk = ?";
	private static final String UPDATE_LAYER_RES = "UPDATE layer_resolution SET _data_store_table =?,_data_store_url=?,_name=?,_precision=?,_properties=?,_zoom=?,_envelope=ST_GeomFromText(?),_grid_size=?, _total_count=?,_singular_count=?,_synth_count=?, _reduction=?, _processed_time=? WHERE _pk = ?";

	static {
		// When data store manager is loaded create meta table.
		createLayerResolutionTable();
	}

	public static long countAux(final String sql, final Connection connection) {
		try {
			connection.setAutoCommit(false);

			PreparedStatement ps = connection.prepareStatement(sql,
					ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
					ResultSet.CLOSE_CURSORS_AT_COMMIT);
			ResultSet resultSet = ps.executeQuery();

			// ResultSet resultSet = connection.prepareStatement(sql)
			// .executeQuery();
			resultSet.next();
			return resultSet.getLong(1);
		} catch (SQLException exception) {
			exception.printStackTrace();
		}
		return 0;
	}

	public static long countElems(final Connection connection,
			final LayerResolution resolution) {

		String sql = "select count(*) from (select _id from "
				+ resolution.getDataStoreTable()
				+ " group by _id order by _id) as tmpTable";

		return countAux(sql, connection);
	}

	public static long countElems(final Connection connection,
			final LayerResolution resolution, final Envelope envelope) {

		String sql = "select count(*) from (select _id from "
				+ resolution.getDataStoreTable()
				+ " where (_geometry && ST_MakeEnvelope(" + envelope.getMinX()
				+ "," + envelope.getMinY() + "," + envelope.getMaxX() + ","
				+ envelope.getMaxY() + ", 4326) )"
				+ " group by _id order by _id) as tmpTable";

		System.out.println(sql);

		return countAux(sql, connection);
	}

	public static long countSingulars(final Connection connection,
			final LayerResolution resolution) {

		String sql = "select count(*) from (select _id from "
				+ resolution.getDataStoreTable()
				+ " where _spatial_type <> 'GranularSynthesis'"
				+ " group by _id order by _id) as tmpTable";

		return countAux(sql, connection);
	}

	public static long countSynths(final Connection connection,
			final LayerResolution resolution) {

		String sql = "select count(*) from (select _id from "
				+ resolution.getDataStoreTable()
				+ " where _spatial_type = 'GranularSynthesis'"
				+ " group by _id order by _id) as tmpTable";

		return countAux(sql, connection);
	}

	public static void createEntityTable(final Connection connection,
			final LayerResolution resolution) {
		Table table = new Table(resolution.getDataStoreTable(), "_pk_id");
		table.add(new Column("_pk_id", true, false, "BIGSERIAL"));
		table.add(new Column("_id", true, false, "TEXT"));
		table.add(new Column("_spatial_type", true, false, "TEXT"));
		table.add(new Column("_geometry", false, true, "GEOMETRY"));
		table.add(new Column("_attribute", true, false, "TEXT"));
		table.add(new Column("_count", true, false, "BIGINT"));
		table.add(new Column("_references", false, false, "TEXT"));
		table.add(new Column("_up_geo_hash", true, false, "INTEGER"));
		table.add(new Column("_up_geometry", false, false, "GEOMETRY"));
		for (String property : resolution.getProperties())
			table.add(new Column(property, false, false, "DOUBLE PRECISION"));
		table.createTable(connection);
	}

	private static void createLayerResolutionTable() {
		Table table = new Table("layer_resolution", "_pk");
		table.add(new Column("_pk", true, false, "BIGSERIAL PRIMARY KEY"));
		table.add(new Column("_data_store_url", true, false, "TEXT"));
		table.add(new Column("_data_store_table", true, false, "TEXT"));
		table.add(new Column("_envelope", false, true, "GEOMETRY"));
		table.add(new Column("_grid_size", true, false, "BIGINT"));
		table.add(new Column("_name", true, false, "TEXT"));
		table.add(new Column("_precision", true, false, "DOUBLE PRECISION"));
		table.add(new Column("_properties", false, false, "TEXT"));
		table.add(new Column("_zoom", true, false, "INTEGER"));
		table.add(new Column("_total_count", true, false, "BIGINT"));
		table.add(new Column("_singular_count", true, false, "BIGINT"));
		table.add(new Column("_synth_count", true, false, "BIGINT"));
		table.add(new Column("_reduction", true, false, "DOUBLE PRECISION"));
		table.add(new Column("_processed_time", true, false, "BIGINT"));
		try {
			Connection connection = DataStoreInfo.getMetaStore();
			table.createTable(connection);
			connection.close();
		} catch (SQLException exception) {
			exception.printStackTrace();
		}
	}

	public synchronized static Connection getConnection() {
		List<Connection> connections = DataStoreInfo.getDataStores();
		Connection connection = connections.get(index);
		index = (index + 1) % connections.size();
		return connection;
	}

	// optimizar isto?
	public static List<Entity> getEntities(final LayerResolution resolution,
			final Envelope envelope, final long first, final long last) {

		// String sql =
		// "SELECT _id, _spatial_type, ST_AsBinary(ST_SnapToGrid(_geometry,0.000000000001)) AS _geometry, _attribute, _count, _references, _up_geo_hash, ST_AsBinary(ST_SnapToGrid(_up_geometry,0.000000000001)) AS _up_geometry";
		// String sql =
		// "SELECT _id, _spatial_type, ST_AsBinary(_geometry) AS _geometry, _attribute, _count, _references, _up_geo_hash, ST_AsBinary(_up_geometry) AS _up_geometry";
		String sql = "SELECT _id, _spatial_type, x, y, _attribute, _count, _references, _up_geo_hash, ST_AsBinary(_up_geometry) AS _up_geometry";
		for (String property : resolution.getProperties())
			sql += "," + property;
		sql += " FROM " + resolution.getDataStoreTable();
		sql += " WHERE (_pk_id >= " + first + " AND _pk_id <" + last + ")";
		sql += " AND (_geometry && ST_MakeEnvelope(" + envelope.getMinX() + ","
				+ envelope.getMinY() + "," + envelope.getMaxX() + ","
				+ envelope.getMaxY() + ", 4326) )";
		sql += " ORDER BY _pk_id";

		System.out.println(sql);

		return getEntitiesAux(sql, resolution);
	}

	public static List<Entity> getEntities(final LayerResolution resolution,
			final long first, final long last) {

		// String sql =
		// "SELECT _id, _spatial_type, ST_AsEWKB(ST_SnapToGrid(_geometry,0.000000000001)) AS _geometry, _attribute, _count, _references, _up_geo_hash, ST_AsEWKB(ST_SnapToGrid(_up_geometry,0.000000000001)) AS _up_geometry";
		String sql = "SELECT _id, _spatial_type, ST_AsBinary(_geometry) AS _geometry, _attribute, _count, _references, _up_geo_hash, ST_AsBinary(_up_geometry) AS _up_geometry";
		for (String property : resolution.getProperties())
			sql += "," + property;
		sql += " FROM " + resolution.getDataStoreTable();
		sql += " WHERE _pk_id >= " + first + " AND _pk_id <" + last;
		sql += " ORDER BY _pk_id";

		return getEntitiesAux(sql, resolution);
	}

	public static List<Entity> getEntitiesAux(final String sql,
			final LayerResolution resolution) {
		List<Entity> result = new LinkedList<Entity>();
		try {
			Long time = System.currentTimeMillis();

			Connection connection = DataStoreInfo.getConnection(resolution
					.getDataStoreURL());
			connection.setAutoCommit(false);

			System.out.println("[QUERY " + Thread.currentThread().getId()
					+ "] - connection in "
					+ (System.currentTimeMillis() - time) + "ms.");

			PreparedStatement ps = connection.prepareStatement(sql,
					ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
					ResultSet.CLOSE_CURSORS_AT_COMMIT);
			ps.setFetchSize(FETCH_SIZE);

			// Statement st = connection.createStatement(
			// ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
			// ResultSet.CLOSE_CURSORS_AT_COMMIT);
			// st.setFetchSize(25000);

			System.out.println("[QUERY " + Thread.currentThread().getId()
					+ "] - statement in " + (System.currentTimeMillis() - time)
					+ "ms.");

			ResultSet resultSet = ps.executeQuery();
			// ResultSet resultSet = st.executeQuery(sql);

			System.out.println("[QUERY " + Thread.currentThread().getId()
					+ "] - done in " + (System.currentTimeMillis() - time)
					+ "ms.");

			GeometryFactory geofact = new GeometryFactory(new PrecisionModel(),
					4326);
			WKBReader wkbReader = new WKBReader(geofact);

			String id;
			String spatialType;
			Geometry geometry;
			String attribute;
			Long count;
			String reference;
			Integer upGeoHash;
			Geometry upGeometry;
			Map<String, Double> properties;
			List<String> propsList = resolution.getProperties();

			while (resultSet.next()) {
				id = resultSet.getString(1);
				spatialType = resultSet.getString(2);

				geometry = wkbReader.read(resultSet.getBytes(3));
				attribute = resultSet.getString(4);
				count = resultSet.getLong(5);
				reference = resultSet.getString(6);
				upGeoHash = resultSet.getInt(7);
				upGeometry = wkbReader.read(resultSet.getBytes(8));

				properties = new HashMap<String, Double>();
				for (int i = 0; i < propsList.size(); i++) {
					Double aux = resultSet.getDouble(9 + i);
					properties.put(propsList.get(i), aux);
				}

				if (!spatialType.equals("GranularSynthesis")) {
					SpatialObject object = new SpatialObject(attribute,
							geometry, id, properties,
							SpatialType.valueOf(spatialType), upGeoHash,
							upGeometry);
					result.add(object);
				} else {
					GranularSynthesis synthesis = new GranularSynthesis(
							attribute, count, geometry, id, properties,
							upGeometry.hashCode(), upGeometry);

					synthesis.setReferences(reference.substring(1,
							reference.length() - 1).split(","));
					result.add(synthesis);
				}
			}

			System.out.println("[QUERY " + Thread.currentThread().getId()
					+ "] - done & iterated in "
					+ (System.currentTimeMillis() - time) + "ms.");

			resultSet.close();
			ps.close();
			// st.close();
			connection.commit();
			connection.close();
		} catch (SQLException exception) {
			exception.printStackTrace();
		} catch (ParseException exception) {
			exception.printStackTrace();
		}
		return result;
	}

	private static List<long[]> getEntitiesIdxAux(final String sql,
			final long batchSize, final LayerResolution resolution) {
		LinkedList<long[]> upGEOHASHidx = new LinkedList<long[]>();

		try {
			Connection connection = DataStoreInfo.getConnection(resolution
					.getDataStoreURL());
			connection.setAutoCommit(false);

			PreparedStatement ps = connection.prepareStatement(sql,
					ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
					ResultSet.CLOSE_CURSORS_AT_COMMIT);
			ps.setFetchSize(FETCH_SIZE);
			ResultSet geohashIDXQuery = ps.executeQuery();

			geohashIDXQuery.next();

			long offset = 0;
			long firstUPGEOHASH = geohashIDXQuery.getInt(1);
			while (geohashIDXQuery.next()) {
				long lastUPGEOHASH = geohashIDXQuery.getInt(1);
				upGEOHASHidx.add(new long[] { offset, firstUPGEOHASH,
						lastUPGEOHASH });
				firstUPGEOHASH = lastUPGEOHASH;
				offset += batchSize;
			}
			upGEOHASHidx.add(new long[] { offset, firstUPGEOHASH,
					firstUPGEOHASH + 10 });

			geohashIDXQuery.close();
			ps.close();
			connection.close();
		} catch (SQLException exception) {
			exception.printStackTrace();
		}

		return upGEOHASHidx;
	}

	public synchronized static List<long[]> getEntitiesIdxForGets(
			final LayerResolution resolution, final long batchSize) {

		String sql = "with index_id as ( select _pk_id, row_number() OVER (order by _pk_id) as rnum from "
				+ resolution.getDataStoreTable()
				+ " group by _pk_id order by _pk_id ), "
				+ "count_id as ( select count(*) as _count from index_id) "
				+ " select _pk_id, rnum from index_id,count_id where (rnum = 1) or ((rnum % "
				+ batchSize
				+ ") = 0) or (rnum >= count_id._count) order by rnum";

		return getEntitiesIdxAux(sql, batchSize, resolution);
	}

	public synchronized static List<long[]> GetEntitiesIdxForUps(
			final LayerResolution resolution, final long batchSize) {

		// String sql =
		// "with index_geohash as ( select _up_geo_hash, row_number() OVER (order by _up_geo_hash) as rnum, sum(_count) as _sum from "
		// + resolution.getDataStoreTable()
		// + " group by _up_geo_hash order by _up_geo_hash ), "
		// + "count_geohash as ( select count(*) as _count from index_geohash) "
		// +
		// " select _up_geo_hash, rnum, (select sum(_sum) from index_geohash) as _sum from index_geohash,count_geohash where (rnum = 1) or ((rnum % "
		// + batchSize
		// + ") = 0) or (rnum >= count_geohash._count) order by rnum";

		String sql = "with index_geohash as ( select _up_geo_hash, row_number() OVER (order by _up_geo_hash) as rnum from "
				+ resolution.getDataStoreTable()
				+ " group by _up_geo_hash order by _up_geo_hash ), "
				+ "count_geohash as ( select count(*) as _count from index_geohash) "
				+ " select _up_geo_hash, rnum  from index_geohash,count_geohash where (rnum = 1) or ((rnum % "
				+ batchSize
				+ ") = 0) or (rnum >= count_geohash._count) order by rnum";

		return getEntitiesIdxAux(sql, batchSize, resolution);
	}

	public synchronized static List<Entity[]> getEntityToUp(
			final LayerResolution resolution, final long first,
			final long last, final Zoom nextLevel) {
		List<Entity[]> result = new LinkedList<Entity[]>();
		try {
			Connection connection = DataStoreInfo.getConnection(resolution
					.getDataStoreURL());

			connection.setAutoCommit(false);

			// String sql =
			// "SELECT array_agg(_id) AS _id, array_agg(_spatial_type) AS _spatial_type, array_agg(_attribute) AS _attribute, array_agg(_count) AS _count, array_agg(_references) AS _references, array_agg(ST_AsText(ST_SnapToGrid(_up_geometry,0.000000000001))) AS _up_geometry";
			String sql = "SELECT array_agg(_id) AS _id, array_agg(_spatial_type) AS _spatial_type, array_agg(_attribute) AS _attribute, array_agg(_count) AS _count, array_agg(_references) AS _references, array_agg(ST_AsText(_up_geometry)) AS _up_geometry";
			for (String property : resolution.getProperties())
				sql += ", array_agg(" + property + ") AS " + property;
			sql += " FROM " + resolution.getDataStoreTable();
			sql += " WHERE _up_geo_hash >= " + first + " AND _up_geo_hash <"
					+ last;
			sql += " GROUP BY _up_geo_hash ORDER BY _up_geo_hash";

			PreparedStatement ps = connection.prepareStatement(sql,
					ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
					ResultSet.CLOSE_CURSORS_AT_COMMIT);
			ps.setFetchSize(FETCH_SIZE);
			ResultSet resultSet = ps.executeQuery();

			GeometryFactory geofact = new GeometryFactory(new PrecisionModel(),
					4326);
			WKTReader wktReader = new WKTReader(geofact);

			while (resultSet.next()) {
				String[] ids = (String[]) resultSet.getArray(1).getArray();
				String[] spatialTypes = (String[]) resultSet.getArray(2)
						.getArray();
				String[] attributes = (String[]) resultSet.getArray(3)
						.getArray();
				Long[] counts = (Long[]) resultSet.getArray(4).getArray();
				String[] references = (String[]) resultSet.getArray(5)
						.getArray();
				String[] upGeometries = (String[]) resultSet.getArray(6)
						.getArray();
				int countProp = 7;
				Map<String, Double[]> properties = new HashMap<String, Double[]>();
				for (String property : resolution.getProperties()) {
					Double[] aux = (Double[]) resultSet.getArray(countProp)
							.getArray();
					properties.put(property, aux);
					countProp++;
				}
				Entity[] entities = new Entity[ids.length];

				double tmpResolution = resolution.getPrecision()
						* resolution.getGridSize() / nextLevel.getGridSize();
				long newNextLevelGridSize = nextLevel != Zoom.Level_00 ? Zoom
						.getNextLevel(nextLevel).getGridSize() : 0;

				for (int i = 0; i < ids.length; i++) {
					Geometry geometry = wktReader.read(upGeometries[i]);
					Geometry upGeometry;
					if (nextLevel == Zoom.Level_00)
						upGeometry = new GeometryFactory()
								.createPoint(new Coordinate());
					else
						upGeometry = Functions.convertToUp(geometry,
								resolution.getMinCoordinate(), tmpResolution,
								nextLevel.getGridSize(), newNextLevelGridSize);
					Map<String, Double> props = new HashMap<String, Double>();
					for (Entry<String, Double[]> entry : properties.entrySet())
						props.put(entry.getKey(), entry.getValue()[i]);

					if (!spatialTypes[i].equals("GranularSynthesis")) {
						SpatialObject object = new SpatialObject(attributes[i],
								geometry, ids[i], props,
								SpatialType.valueOf(spatialTypes[i]),
								upGeometry.hashCode(), upGeometry);
						entities[i] = object;
					} else {
						GranularSynthesis synthesis = new GranularSynthesis(
								attributes[i], counts[i], geometry, ids[i],
								props, upGeometry.hashCode(), upGeometry);
						// Iterable<String> it = Splitter.on(",")
						// .trimResults(CharMatcher.anyOf("[]"))
						// .split(references[i]);
						// synthesis.setReferences(it);
						synthesis.setReferences(references[i].substring(1,
								references[i].length() - 1).split(","));
						entities[i] = synthesis;
					}
				}
				result.add(entities);
			}
			resultSet.close();
			ps.close();
			connection.close();
		} catch (SQLException exception) {
			exception.printStackTrace();
		} catch (ParseException exception) {
			exception.printStackTrace();
		}
		return result;
	}

	// returns a LayerResolution for each datastore/table
	public static LayerResolution[] getLayersTables(
			final Connection connection, final String layerName,
			final String layerZoom) {

		LayerResolution[] ret = null;

		GeometryFactory geofact = new GeometryFactory(new PrecisionModel(),
				4326);
		WKTReader reader = new WKTReader(geofact);

		try {
			String sql = SELECT_LAYER_RES_BY_NAME;
			sql += layerZoom != null ? " AND _zoom=?" : "";
			sql += " ORDER BY _grid_size DESC";
			PreparedStatement st0 = connection.prepareStatement(sql);
			st0.setString(1, layerName);
			if (layerZoom != null)
				st0.setInt(2, Integer.parseInt(layerZoom));
			ResultSet resultSet = st0.executeQuery();
			List<LayerResolution> tmpRet = new LinkedList<LayerResolution>();

			// int count = 0;
			while (resultSet.next()) {
				String datastoreURL = resultSet.getString(2);
				Geometry geom = reader.read(resultSet.getString(4));
				Long gridSize = resultSet.getLong(5);
				Double precision = resultSet.getDouble(7);
				String array = resultSet.getString(8);
				String[] props = array.substring(1, array.length() - 1).split(
						", ");
				int zoom = resultSet.getInt(9);

				Long totalCount = resultSet.getLong(10);
				Long singularCount = resultSet.getLong(11);
				Long synthCount = resultSet.getLong(12);
				Double reduction = resultSet.getDouble(13);
				Long processedTime = resultSet.getLong(14);

				LayerResolution lr = new LayerResolution(layerName, precision,
						Arrays.asList(props), new Integer(zoom));
				lr.setGridSize(gridSize);
				lr.setDataStoreURL(datastoreURL);
				lr.setEnvelope(geom);

				lr.setTotalCount(totalCount);
				lr.setSingularCount(singularCount);
				lr.setSynthCount(synthCount);
				lr.setReduction(reduction);
				lr.setProcessedTime(processedTime);

				// ret[count] = lr;
				tmpRet.add(lr);
				// count++;
			}

			ret = tmpRet.toArray(new LayerResolution[0]);
		} catch (SQLException exception) {
			exception.printStackTrace();
		} catch (ParseException exception) {
			exception.printStackTrace();
		}

		return ret;
	}

	public static String getSingleEntitiesOptimizedForGet(
			final LayerResolution resolution, final Envelope envelope,
			final long first, final long last) {

		StringBuilder ret = new StringBuilder();
		List<String> propsList = resolution.getProperties();
		int propsSize = resolution.getProperties().size();
		String[] propsArray = new String[propsSize];

		String sql = "SELECT ST_AsBinary(_geometry), _attribute, _count";
		int pIdx = 0;
		for (String property : propsList) {
			sql += "," + property;
			propsArray[pIdx] = property;
			pIdx++;
		}
		sql += " FROM " + resolution.getDataStoreTable();
		sql += " WHERE (_pk_id >= " + first + " AND _pk_id <" + last + ")";
		sql += " AND _spatial_type='Point'";
		sql += " AND (_geometry && ST_MakeEnvelope(" + envelope.getMinX() + ","
				+ envelope.getMinY() + "," + envelope.getMaxX() + ","
				+ envelope.getMaxY() + ", 4326) )";
		sql += " ORDER BY _pk_id";

		System.out.println(sql);

		Long time = System.currentTimeMillis();

		try {

			Connection connection = DataStoreInfo.getDataStores().get(0);
			connection.setAutoCommit(false);

			System.out.println("[QUERY " + Thread.currentThread().getId()
					+ "] - connection in "
					+ (System.currentTimeMillis() - time) + "ms.");

			Statement st = connection.createStatement(
					ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
					ResultSet.CLOSE_CURSORS_AT_COMMIT);
			st.setFetchSize(250);

			System.out.println("[QUERY " + Thread.currentThread().getId()
					+ "] - statement in " + (System.currentTimeMillis() - time)
					+ "ms.");

			ResultSet resultSet = st.executeQuery(sql);

			System.out.println("[QUERY " + Thread.currentThread().getId()
					+ "] - done in " + (System.currentTimeMillis() - time)
					+ "ms.");

			GeometryFactory geofact = new GeometryFactory(new PrecisionModel(),
					4326);
			WKBReader wkbReader = new WKBReader(geofact);

			Geometry geometry;
			String attribute;
			Long count;

			while (resultSet.next()) {
				geometry = wkbReader.read(resultSet.getBytes(1));
				attribute = resultSet.getString(2);
				count = resultSet.getLong(3);

				StringBuilder propsVals = new StringBuilder();
				propsVals.append("\"").append(attribute).append("\",")
						.append(count).append(",");

				Double aux;
				for (int i = 0; i < pIdx; i++) {
					aux = resultSet.getDouble(4 + i);
					propsVals.append(aux).append(",");
				}

				ret.append("{\"g\":{\"t\":\"Point\",\"c\":[")
						.append(((Point) geometry).getX()).append(", ")
						.append(((Point) geometry).getY())
						.append("]},\"p\":{\"v\":[")
						.append(propsVals.substring(0, propsVals.length() - 1))
						.append("],\"s\":\"").append("false").append("\"}}")
						.append(",");
			}

			System.out.println("[QUERY " + Thread.currentThread().getId()
					+ "] - done & iterated in "
					+ (System.currentTimeMillis() - time) + "ms.");

			resultSet.close();
			st.close();
			connection.commit();
			connection.close();
		} catch (SQLException exception) {
			exception.printStackTrace();
		} catch (ParseException exception) {
			exception.printStackTrace();
		}

		String retStr;
		if (ret.length() > 0)
			retStr = ret.substring(0, ret.length() - 1);
		else
			retStr = new String();

		return retStr;
	}

	public static List<Entity> getSynthEntitiesOptimizedForGet(
			final LayerResolution resolution, final Envelope envelope,
			final long first, final long last) {

		List<String> propsList = resolution.getProperties();
		int propsSize = resolution.getProperties().size();
		String[] propsArray = new String[propsSize];

		String sql = "SELECT _id, ST_AsBinary(_geometry), _attribute, _count, _references";
		int pIdx = 0;
		for (String property : propsList) {
			sql += "," + property;
			propsArray[pIdx] = property;
			pIdx++;
		}
		sql += " FROM " + resolution.getDataStoreTable();
		sql += " WHERE (_pk_id >= " + first + " AND _pk_id <" + last + ")";
		sql += " AND _spatial_type='GranularSynthesis'";
		sql += " AND (_geometry && ST_MakeEnvelope(" + envelope.getMinX() + ","
				+ envelope.getMinY() + "," + envelope.getMaxX() + ","
				+ envelope.getMaxY() + ", 4326) )";
		sql += " ORDER BY _pk_id";

		System.out.println(sql);

		List<Entity> result = new LinkedList<Entity>();
		try {
			Long time = System.currentTimeMillis();

			Connection connection = DataStoreInfo.getConnection(resolution
					.getDataStoreURL());
			connection.setAutoCommit(false);

			System.out.println("[QUERY " + Thread.currentThread().getId()
					+ "] - connection in "
					+ (System.currentTimeMillis() - time) + "ms.");

			// PreparedStatement ps = connection.prepareStatement(sql,
			// ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
			// ResultSet.CLOSE_CURSORS_AT_COMMIT);
			// ps.setFetchSize(FETCH_SIZE);

			Statement st = connection.createStatement(
					ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
					ResultSet.CLOSE_CURSORS_AT_COMMIT);
			st.setFetchSize(FETCH_SIZE);

			System.out.println("[QUERY " + Thread.currentThread().getId()
					+ "] - statement in " + (System.currentTimeMillis() - time)
					+ "ms.");

			// ResultSet resultSet = ps.executeQuery();
			ResultSet resultSet = st.executeQuery(sql);

			System.out.println("[QUERY " + Thread.currentThread().getId()
					+ "] - done in " + (System.currentTimeMillis() - time)
					+ "ms.");

			GeometryFactory geofact = new GeometryFactory(new PrecisionModel(),
					4326);
			WKBReader wkbReader = new WKBReader(geofact);

			String id;
			Geometry geometry;
			String attribute;
			Long count;
			String reference;
			Map<String, Double> properties;
			// String granularSynthString = "GranularSynthesis";

			while (resultSet.next()) {
				id = resultSet.getString(1);

				geometry = wkbReader.read(resultSet.getBytes(2));
				attribute = resultSet.getString(3);
				count = resultSet.getLong(4);
				reference = resultSet.getString(5);

				properties = new HashMap<String, Double>();
				for (int i = 0; i < propsSize; i++) {
					Double aux = resultSet.getDouble(6 + i);
					properties.put(propsArray[i], aux);
				}

				// if (!spatialType.equals(granularSynthString)) {
				// SpatialObject object = new SpatialObject(attribute,
				// geometry, id, properties,
				// SpatialType.valueOf(spatialType), 0, null);
				// result.add(object);
				// } else {
				GranularSynthesis synthesis = new GranularSynthesis(attribute,
						count, geometry, id, properties, 0, null);

				synthesis.setReferences(reference.substring(1,
						reference.length() - 1).split(","));
				result.add(synthesis);
				// }
			}

			System.out.println("[QUERY " + Thread.currentThread().getId()
					+ "] - done & iterated in "
					+ (System.currentTimeMillis() - time) + "ms.");

			resultSet.close();
			// ps.close();
			st.close();
			connection.commit();
			connection.close();
		} catch (SQLException exception) {
			exception.printStackTrace();
		} catch (ParseException exception) {
			exception.printStackTrace();
		}
		return result;
	}

	public static void main(final String[] args) throws Exception {
		// String sql =
		// "SELECT _id, _spatial_type, ST_AsBinary(_geometry), _attribute, _count, _references, countpersons, countfatals, countdrunks, density";
		// sql += " FROM accidents_5";
		// sql +=
		// " WHERE (_geometry && ST_MakeEnvelope(-126.60644531250001,26.43122806450644,-63.369140625,52.96187505907603, 4326) )";
		// sql += " ORDER BY _pk_id";
		//
		// System.out.println(sql);
		//
		// Long time = System.currentTimeMillis();
		// try {
		//
		// Connection connection = DataStoreInfo.getDataStores().get(0);
		// connection.setAutoCommit(false);
		//
		// System.out.println("[QUERY " + Thread.currentThread().getId()
		// + "] - connection in "
		// + (System.currentTimeMillis() - time) + "ms.");
		//
		// // PreparedStatement ps = connection.prepareStatement(sql,
		// // ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
		// // ResultSet.CLOSE_CURSORS_AT_COMMIT);
		// // ps.setFetchSize(250);
		//
		// // Statement st = connection.createStatement(
		// // ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
		// // ResultSet.CLOSE_CURSORS_AT_COMMIT);
		// // st.setFetchSize(250);
		// Statement st = connection.createStatement();
		//
		// System.out.println("[QUERY " + Thread.currentThread().getId()
		// + "] - statement in " + (System.currentTimeMillis() - time)
		// + "ms.");
		//
		// // ResultSet resultSet = ps.executeQuery();
		// ResultSet resultSet = st.executeQuery(sql);
		//
		// System.out.println("[QUERY " + Thread.currentThread().getId()
		// + "] - done in " + (System.currentTimeMillis() - time)
		// + "ms.");
		//
		// GeometryFactory geofact = new GeometryFactory(new PrecisionModel(),
		// 4326);
		// WKBReader wkbReader = new WKBReader(geofact);
		//
		// String id;
		// String spatialType;
		// Geometry geometry;
		// String attribute;
		// Long count;
		// String reference;
		// Map<String, Double> properties;
		// String granularSynthString = "GranularSynthesis";
		//
		// while (resultSet.next()) {
		// id = resultSet.getString(1);
		// spatialType = resultSet.getString(2);
		//
		// // geometry = wkbReader.read(resultSet.getBytes(3));
		// attribute = resultSet.getString(4);
		// count = resultSet.getLong(5);
		// reference = resultSet.getString(6);
		//
		// properties = new HashMap<String, Double>();
		// Double aux = resultSet.getDouble(7);
		// properties.put("countpersons", aux);
		// aux = resultSet.getDouble(8);
		// properties.put("countfatals", aux);
		// aux = resultSet.getDouble(9);
		// properties.put("countdrunks", aux);
		// aux = resultSet.getDouble(10);
		// properties.put("density", aux);
		//
		// // if (!spatialType.equals(granularSynthString)) {
		// // SpatialObject object = new SpatialObject(attribute,
		// // geometry, id, properties,
		// // SpatialType.valueOf(spatialType), 0, null);
		// // result.add(object);
		// // } else {
		// // GranularSynthesis synthesis = new GranularSynthesis(
		// // attribute, count, geometry, id, properties, 0, null);
		// //
		// // synthesis.setReferences(reference.substring(1,
		// // reference.length() - 1).split(","));
		// // result.add(synthesis);
		// // }
		// }
		//
		// System.out.println("[QUERY " + Thread.currentThread().getId()
		// + "] - done & iterated in "
		// + (System.currentTimeMillis() - time) + "ms.");
		//
		// resultSet.close();
		// // ps.close();
		// st.close();
		// connection.commit();
		// connection.close();
		// } catch (SQLException exception) {
		// exception.printStackTrace();
		// // } catch (ParseException exception) {
		// // exception.printStackTrace();
		// }
		//
		// // for (Entity ent : result)
		// // ent.toJson();
		//
		// System.out.println("[TOJSON " + Thread.currentThread().getId()
		// + "] - done in " + (System.currentTimeMillis() - time) + "ms.");

	}

	public static void save(final Connection connection,
			final LayerResolution resolution) {
		try {
			PreparedStatement st0 = connection
					.prepareStatement(SELECT_LAYER_RES);
			st0.setString(1, resolution.getDataStoreTable());
			st0.setString(2, resolution.getDataStoreURL());
			ResultSet resultSet = st0.executeQuery();
			PreparedStatement st1;
			if (!resultSet.next()) {
				String url = connection.getMetaData().getURL();
				resolution.setDataStoreURL(url);
				st1 = connection.prepareStatement(INSERT_LAYER_RES);
			} else {
				st1 = connection.prepareStatement(UPDATE_LAYER_RES);
				st1.setBigDecimal(14, resultSet.getBigDecimal(1));
			}
			st1.setString(1, resolution.getDataStoreTable());
			st1.setString(2, resolution.getDataStoreURL());
			st1.setString(3, resolution.getName());
			st1.setDouble(4, resolution.getPrecision());
			st1.setString(5, resolution.getProperties().toString());
			st1.setInt(6, resolution.getZoom());
			st1.setString(7, resolution.getEnvelope().toText());
			st1.setLong(8, resolution.getGridSize());

			st1.setLong(9, resolution.getTotalCount());
			st1.setLong(10, resolution.getSingularCount());
			st1.setLong(11, resolution.getSynthCount());
			st1.setDouble(12, resolution.getReduction());
			st1.setLong(13, resolution.getProcessedTime());

			st1.execute();
		} catch (SQLException exception) {
			exception.printStackTrace();
		}
	}

	public synchronized static void save(final List<Entity> entities,
			final Connection connection, final LayerResolution resolution) {
		try {
			// TODO: optimizar isto conforme GET
			String sql = "INSERT INTO "
					+ resolution.getDataStoreTable()
					+ " (_id,_spatial_type,_geometry,_attribute,_count,_references,_up_geo_hash,_up_geometry";
			for (String property : resolution.getProperties())
				sql += "," + property;
			// sql +=
			// ") VALUES (?,?,ST_SnapToGrid(ST_GeomFromText(?,4326),0.000000000001),?,?,?,?,ST_SnapToGrid(ST_GeomFromText(?,4326),0.000000000001)";
			sql += ") VALUES (?,?,ST_GeomFromText(?,4326),?,?,?,?,ST_GeomFromText(?,4326)";
			for (int i = 0; i < resolution.getProperties().size(); i++)
				sql += "," + "?";
			sql += ")";

			PreparedStatement ps = connection.prepareStatement(sql);

			// WKBWriter writer = new WKBWriter();

			int batchCount = 0;
			for (Entity entity : entities) {

				ps.setString(1, entity.getId());
				ps.setString(2, entity.getType().name());
				ps.setString(3, entity.getGeometry().toText());
				ps.setString(4, entity.getAttribute());
				ps.setLong(5, entity.getCount());
				if (entity instanceof GranularSynthesis)
					ps.setString(6, ((GranularSynthesis) entity)
							.getReferences().toString());
				else
					ps.setString(6, "[]");
				ps.setInt(7, entity.getUpGeoHash());
				ps.setString(8, entity.getUpGeometry().toText());
				int pcount = 9;
				for (String property : resolution.getProperties()) {
					Double value = entity.getProperties().get(property);
					ps.setDouble(pcount, value);
					pcount++;
				}

				ps.addBatch();
				if (batchCount == BATCHINSERT_SIZE) {
					ps.executeBatch();
					batchCount = 0;
				} else
					batchCount++;
			}
			ps.executeBatch();

		} catch (SQLException exception) {
			exception.printStackTrace();
			System.exit(0);
		}
	}

	public static Boolean tableDrop(final LayerResolution res) {
		Boolean result = false;
		try {
			Connection connection = DataStoreInfo.getConnection(res
					.getDataStoreURL());

			Statement st = connection.createStatement();
			result = st.execute("DROP TABLE " + res.getDataStoreTable() + ";");
			st.close();
			connection.close();
		} catch (SQLException exception) {
			exception.printStackTrace();
		}
		return result;
	}

	public static Boolean tableExists(final Connection connection,
			final String tableName) {
		Boolean result = false;
		try {
			PreparedStatement st = connection.prepareStatement(EXISTS_TABLE);
			st.setString(1, tableName);
			ResultSet resultSet = st.executeQuery();
			result = resultSet.next();
			st.close();
		} catch (SQLException exception) {
			exception.printStackTrace();
		}
		return result;
	}

	public static Boolean tableExists(final LayerResolution res) {
		Boolean result = false;
		try {
			Connection connection = DataStoreInfo.getConnection(res
					.getDataStoreURL());
			result = tableExists(connection, res.getDataStoreTable());
			connection.close();
		} catch (SQLException exception) {
			exception.printStackTrace();
		}
		return result;

	}

	public static Boolean vacuumAnalyze(final LayerResolution res) {
		Boolean result = false;
		try {
			Connection connection = DataStoreInfo.getConnection(res
					.getDataStoreURL());

			Statement st = connection.createStatement();
			result = st.execute("VACUUM ANALYZE " + res.getDataStoreTable()
					+ ";");
			st.close();
			connection.close();
		} catch (SQLException exception) {
			exception.printStackTrace();
		}
		return result;
	}

	private DataStoreManager() {

	}

}
