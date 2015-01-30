package vast_research.spatial_core.api;

import java.io.File;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.jboss.resteasy.annotations.GZIP;

@Path("s")
public class StaticResources {

	@GZIP
	@GET
	@Path("{file:.*}")
	public File load(@PathParam("file") final String filename) {
		return new File("resources/static/" + filename);
	}

}
