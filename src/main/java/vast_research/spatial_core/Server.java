package vast_research.spatial_core;

import java.util.List;

import org.jboss.resteasy.jsapi.JSAPIServlet;
import org.jboss.resteasy.plugins.cache.server.ServerCacheFeature;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;

import vast_research.spatial_core.api.DataResource;
import vast_research.spatial_core.api.DownloadResource;
import vast_research.spatial_core.api.ExecuteUpResource;
import vast_research.spatial_core.api.InfoResource;
import vast_research.spatial_core.api.MonitorResource;
import vast_research.spatial_core.api.NrElementsResource;
import vast_research.spatial_core.api.StaticResources;
import vast_research.spatial_core.api.StatusResource;
import vast_research.spatial_core.api.UploadResource;
import vast_research.spatial_core.resources.Config;
import vast_research.spatial_core.resources.MonitorItem;
import vast_research.spatial_core.resources.Workers;

public class Server {

	// public static HashMap<String, Response> requestCache = new
	// HashMap<String, Response>();

	// temos de mudar isto para lista shared
	public static List<MonitorItem> monitor;

	private static TJWSEmbeddedJaxrsServer server;

	public static void main(final String[] args) {

		Workers wks = new Workers();

		// start server
		server = new TJWSEmbeddedJaxrsServer();
		server.setPort(Config.getConfigInt("server_port"));
		server.setRootResourcePath("/");

		// List<Class> resources = server.getDeployment()
		// .getActualResourceClasses();
		//
		// resources.add(UploadResource.class);
		// resources.add(ExecuteUpResource.class);
		// resources.add(DownloadResource.class);
		// resources.add(DataResource.class);
		// resources.add(InfoResource.class);
		// resources.add(NrElementsResource.class);
		// resources.add(StaticResources.class);

		server.start();

		monitor = Workers.init().getList("monitor");
		monitor.add(new MonitorItem(MonitorItem.opType.SERVER, "Start", System
				.currentTimeMillis(), -1L));

		ServerCacheFeature cache = new ServerCacheFeature();
		server.getDeployment().getProviderFactory()
				.registerProvider(cache.getClass());

		server.getDeployment().getRegistry()
				.addPerRequestResource(StaticResources.class);
		server.getDeployment().getRegistry()
				.addPerRequestResource(StatusResource.class);
		server.getDeployment().getRegistry()
				.addPerRequestResource(MonitorResource.class);
		server.getDeployment().getRegistry()
				.addPerRequestResource(UploadResource.class);
		server.getDeployment().getRegistry()
				.addPerRequestResource(ExecuteUpResource.class);
		server.getDeployment().getRegistry()
				.addPerRequestResource(DownloadResource.class);
		server.getDeployment().getRegistry()
				.addPerRequestResource(DataResource.class);
		server.getDeployment().getRegistry()
				.addPerRequestResource(InfoResource.class);
		server.getDeployment().getRegistry()
				.addPerRequestResource(NrElementsResource.class);

		server.addServlet("/rest-js", new JSAPIServlet());
	}
}
