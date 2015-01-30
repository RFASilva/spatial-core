package vast_research.spatial_core.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.GZIP;

import vast_research.spatial_core.Server;
import vast_research.spatial_core.resources.MonitorItem;

@Path("status")
public class StatusResource {

	private static String returnsLastNLines(final File file, final int nlines)
			throws FileNotFoundException, IOException {

		RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
		int lines = 0;
		StringBuilder ret = new StringBuilder();
		StringBuilder builder = new StringBuilder();
		long length = file.length();
		length--;
		randomAccessFile.seek(length);
		for (long seek = length; seek >= 0; --seek) {
			randomAccessFile.seek(seek);
			char c = (char) randomAccessFile.read();
			builder.append(c);
			if (c == '\n') {
				builder = builder.reverse();
				ret.insert(0, builder.toString());
				lines++;
				builder = null;
				builder = new StringBuilder();
				if (lines == nlines)
					break;
			}

		}

		return ret.toString();
	}

	@GZIP
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String status() throws Exception {
		long time = System.currentTimeMillis();
		MonitorItem op = new MonitorItem(MonitorItem.opType.STATUS, "", time,
				-1L);
		Server.monitor.add(op);

		File f = new File("logs/status.log");
		String ret = returnsLastNLines(f, 500);

		Server.monitor.remove(op);
		op.setEndTime(System.currentTimeMillis() - time);
		Server.monitor.add(op);

		return ret;
	}
}
