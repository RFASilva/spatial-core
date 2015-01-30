package vast_research.spatial_core.resources;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedList;
import java.util.List;

public class DataStoreInfo {

	// private static Map<String, DataSource> dataSources = new HashMap<String,
	// DataSource>();

	public static Connection getConnection(final String url) {
		// if (!dataSources.containsKey(url)) {
		// ConnectionFactory connectionFactory = new
		// DriverManagerConnectionFactory(
		// url, null);
		// PoolableConnectionFactory factory = new PoolableConnectionFactory(
		// connectionFactory, new GenericObjectPool(null), null, null,
		// false, true);
		// dataSources.put(url, new PoolingDataSource(factory.getPool()));
		// }
		//
		// DataSource ds = dataSources.get(url);
		Connection connection = null;
		try {
			Class.forName("org.postgresql.Driver");
			connection = DriverManager.getConnection(url);
		} catch (Exception exception) {
			exception.printStackTrace();
		}

		return connection;
		// try {
		// return ds.getConnection();
		// } catch (SQLException exception) {
		// exception.printStackTrace();
		// return null;
		// }
	}

	public static List<Connection> getDataStores() {
		List<Connection> result = new LinkedList<Connection>();
		try {
			int numDBs = Config.getConfigInt("data_stores_num");
			for (int i = 1; i <= numDBs; i++) {
				String urlDB = Config.getConfigString("data_store_url_" + i);
				result.add(getConnection(urlDB));
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		return result;
	}

	public static Connection getMetaStore() {
		return getConnection(Config.getConfigString("meta_store_url"));
	}
}
