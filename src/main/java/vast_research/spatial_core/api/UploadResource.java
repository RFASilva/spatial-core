package vast_research.spatial_core.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import vast_research.spatial_core.jobs.store.BatchLoader;

import com.google.common.io.ByteStreams;

@Path("/upload")
public class UploadResource {

	@GZIP
	@POST
	@Path("/batch/{layerName}")
	@Consumes("multipart/form-data")
	public Response uploadFile(@PathParam("layerName") final String layerName,
			final MultipartFormDataInput input) throws IOException {

		Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
		List<InputPart> inputParts = uploadForm.get("file");
		String key = "_" + new Date().getTime();
		File file = new File("work/" + layerName + key);
		for (InputPart inputPart : inputParts) {
			InputStream stream = inputPart.getBody(InputStream.class, null);
			ByteStreams.copy(stream, new FileOutputStream(file));
		}
		try {
			new BatchLoader().load(layerName, file);
			return Response.ok().build();
		} catch (Exception exception) {
			throw new WebApplicationException(exception);
		}

	}
}