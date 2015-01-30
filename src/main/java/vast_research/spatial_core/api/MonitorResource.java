package vast_research.spatial_core.api;

import java.util.Iterator;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.GZIP;

import vast_research.spatial_core.Server;
import vast_research.spatial_core.resources.MonitorItem;

@Path("monitor")
public class MonitorResource {

	@GZIP
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String monitor() {
		List<MonitorItem> monitor = Server.monitor;
		Iterator<MonitorItem> it = monitor.iterator();

		StringBuilder ret = new StringBuilder();

		ret.append("[");

		while (it.hasNext()) {
			MonitorItem op = it.next();
			ret.append(op.toJSON());
			if (it.hasNext())
				ret.append(",");
		}

		ret.append("]");
		return ret.toString();
	}
}