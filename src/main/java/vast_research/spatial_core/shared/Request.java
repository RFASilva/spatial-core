package vast_research.spatial_core.shared;

import java.io.Serializable;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class Request implements Serializable {

	private static final long serialVersionUID = 1L;

	private Envelope envelope;

	private Long gridSize;

	private String layerName;

	private Point ne;

	private Point sw;

	private Integer zoom;

	public Request() {

	}

	public Request(final Long gridSize, final String ne, final String sw,
			final Integer zoom, final String layerName) {
		this.gridSize = gridSize;
		this.layerName = layerName;
		String[] neTokens = ne.split(",");
		this.ne = new GeometryFactory().createPoint(new Coordinate(new Double(
				neTokens[0]), new Double(neTokens[1])));
		String[] swTokens = sw.split(",");
		this.sw = new GeometryFactory().createPoint(new Coordinate(new Double(
				swTokens[0]), new Double(swTokens[1])));
		envelope = new Envelope(this.sw.getCoordinate(),
				this.ne.getCoordinate());
		this.zoom = zoom;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Request other = (Request) obj;
		if (envelope == null) {
			if (other.envelope != null)
				return false;
		} else if (!envelope.equals(other.envelope))
			return false;
		if (gridSize == null) {
			if (other.gridSize != null)
				return false;
		} else if (!gridSize.equals(other.gridSize))
			return false;
		if (layerName == null) {
			if (other.layerName != null)
				return false;
		} else if (!layerName.equals(other.layerName))
			return false;
		if (ne == null) {
			if (other.ne != null)
				return false;
		} else if (!ne.equals(other.ne))
			return false;
		if (sw == null) {
			if (other.sw != null)
				return false;
		} else if (!sw.equals(other.sw))
			return false;
		if (zoom == null) {
			if (other.zoom != null)
				return false;
		} else if (!zoom.equals(other.zoom))
			return false;
		return true;
	}

	public Envelope getEnvelope() {
		return envelope;
	}

	public Long getGridSize() {
		return gridSize;
	}

	public String getLayerName() {
		return layerName;
	}

	public Point getNe() {
		return ne;
	}

	public Point getSw() {
		return sw;
	}

	public Integer getZoom() {
		return zoom;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (envelope == null ? 0 : envelope.hashCode());
		result = prime * result + (gridSize == null ? 0 : gridSize.hashCode());
		result = prime * result
				+ (layerName == null ? 0 : layerName.hashCode());
		result = prime * result + (ne == null ? 0 : ne.hashCode());
		result = prime * result + (sw == null ? 0 : sw.hashCode());
		result = prime * result + (zoom == null ? 0 : zoom.hashCode());
		return result;
	}

	public void setEnvelope(final Envelope envelope) {
		this.envelope = envelope;
	}

	public void setGridSize(final Long gridSize) {
		this.gridSize = gridSize;
	}

	public void setLayerName(final String layerName) {
		this.layerName = layerName;
	}

	public void setNe(final Point ne) {
		this.ne = ne;
	}

	public void setSw(final Point sw) {
		this.sw = sw;
	}

	public void setZoom(final Integer zoom) {
		this.zoom = zoom;
	}

	@Override
	public String toString() {
		return "Request [envelope=" + envelope + ", gridSize=" + gridSize
				+ ", layerName=" + layerName + ", ne=" + ne + ", sw=" + sw
				+ ", zoom=" + zoom + "]";
	}

}
