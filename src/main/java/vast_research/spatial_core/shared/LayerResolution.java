package vast_research.spatial_core.shared;

import java.io.Serializable;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class LayerResolution implements Serializable {

	private static final long serialVersionUID = 1L;

	private String dataStoreURL;

	private Geometry envelope;

	private Long gridSize;

	private Long id;

	private String name;

	private Double precision;

	private Long processedTime = -1L;

	private List<String> properties;
	private Double reduction = new Double(0);
	private Long singularCount = 0L;
	private Long synthCount = 0L;
	private Long totalCount = 0L;
	private Integer zoom;

	public LayerResolution() {

	}

	public LayerResolution(final String name, final Double precision,
			final List<String> properties, final Integer zoom) {
		this.name = name;
		this.precision = precision;
		this.properties = properties;
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
		LayerResolution other = (LayerResolution) obj;
		if (dataStoreURL == null) {
			if (other.dataStoreURL != null)
				return false;
		} else if (!dataStoreURL.equals(other.dataStoreURL))
			return false;
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
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (precision == null) {
			if (other.precision != null)
				return false;
		} else if (!precision.equals(other.precision))
			return false;
		if (processedTime == null) {
			if (other.processedTime != null)
				return false;
		} else if (!processedTime.equals(other.processedTime))
			return false;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		if (reduction == null) {
			if (other.reduction != null)
				return false;
		} else if (!reduction.equals(other.reduction))
			return false;
		if (singularCount == null) {
			if (other.singularCount != null)
				return false;
		} else if (!singularCount.equals(other.singularCount))
			return false;
		if (synthCount == null) {
			if (other.synthCount != null)
				return false;
		} else if (!synthCount.equals(other.synthCount))
			return false;
		if (totalCount == null) {
			if (other.totalCount != null)
				return false;
		} else if (!totalCount.equals(other.totalCount))
			return false;
		if (zoom == null) {
			if (other.zoom != null)
				return false;
		} else if (!zoom.equals(other.zoom))
			return false;
		return true;
	}

	public String getDataStoreTable() {
		return name + (zoom == -1 ? "_raw" : "_" + zoom);
	}

	public String getDataStoreURL() {
		return dataStoreURL;
	}

	public Geometry getEnvelope() {
		return envelope != null ? envelope : null;
	}

	public Long getGridSize() {
		return gridSize;
	}

	public Long getId() {
		return id;
	}

	public Coordinate getMaxCoordinate() {
		Double x = envelope.getEnvelopeInternal().getMaxX();
		Double y = envelope.getEnvelopeInternal().getMaxY();
		return new Coordinate(x, y);
	}

	public Coordinate getMinCoordinate() {
		Double x = envelope.getEnvelopeInternal().getMinX();
		Double y = envelope.getEnvelopeInternal().getMinY();
		return new Coordinate(x, y);
	}

	public String getName() {
		return name;
	}

	public Double getPrecision() {
		return precision;
	}

	public Long getProcessedTime() {
		return processedTime;
	}

	public List<String> getProperties() {
		return properties;
	}

	public Double getReduction() {
		return reduction;
	}

	public Long getSingularCount() {
		return singularCount;
	}

	public Long getSynthCount() {
		return synthCount;
	}

	public Long getTotalCount() {
		return totalCount;
	}

	public Integer getZoom() {
		return zoom;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (dataStoreURL == null ? 0 : dataStoreURL.hashCode());
		result = prime * result + (envelope == null ? 0 : envelope.hashCode());
		result = prime * result + (gridSize == null ? 0 : gridSize.hashCode());
		result = prime * result + (id == null ? 0 : id.hashCode());
		result = prime * result + (name == null ? 0 : name.hashCode());
		result = prime * result
				+ (precision == null ? 0 : precision.hashCode());
		result = prime * result
				+ (processedTime == null ? 0 : processedTime.hashCode());
		result = prime * result
				+ (properties == null ? 0 : properties.hashCode());
		result = prime * result
				+ (reduction == null ? 0 : reduction.hashCode());
		result = prime * result
				+ (singularCount == null ? 0 : singularCount.hashCode());
		result = prime * result
				+ (synthCount == null ? 0 : synthCount.hashCode());
		result = prime * result
				+ (totalCount == null ? 0 : totalCount.hashCode());
		result = prime * result + (zoom == null ? 0 : zoom.hashCode());
		return result;
	}

	public void setDataStoreURL(final String dataStoreURL) {
		this.dataStoreURL = dataStoreURL;
	}

	public void setEnvelope(final Geometry envelope) {
		this.envelope = envelope;
	}

	public void setGridSize(final Long gridSize) {
		this.gridSize = gridSize;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public void setPrecision(final Double precision) {
		this.precision = precision;
	}

	public void setProcessedTime(final Long processedTime) {
		this.processedTime = processedTime;
	}

	public void setProperties(final List<String> properties) {
		this.properties = properties;
	}

	public void setReduction(final Double reduction) {
		this.reduction = reduction;
	}

	public void setSingularCount(final Long singularCount) {
		this.singularCount = singularCount;
	}

	public void setSynthCount(final Long synthCount) {
		this.synthCount = synthCount;
	}

	public void setTotalCount(final Long totalCount) {
		this.totalCount = totalCount;
	}

	public void setZoom(final Integer zoom) {
		this.zoom = zoom;
	}

	@Override
	public String toString() {
		return "LayerResolution [dataStoreURL=" + dataStoreURL + ", envelope="
				+ envelope + ", gridSize=" + gridSize + ", id=" + id
				+ ", name=" + name + ", precision=" + precision
				+ ", properties=" + properties + ", zoom=" + zoom + ", total="
				+ totalCount + " singularCount=" + singularCount
				+ " synthCount=" + synthCount + " reduction=" + reduction
				+ " processedTime=" + processedTime + "]";
	}

}
