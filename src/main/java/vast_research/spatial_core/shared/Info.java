/* to be removed - not usefull */

package vast_research.spatial_core.shared;

import java.io.Serializable;

public class Info implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long gridSize;

	private Long nSingularElements;

	private Long nSyntheses;

	private Double reductionRate;

	private Long totalElements;

	private Integer zoom;

	public Info() {

	}

	public Info(final Long gridSize, final Long nSingularElements,
			final Long nSyntheses, final Double reductionRate,
			final Long totalElements, final Integer zoom) {
		this.gridSize = gridSize;
		this.nSingularElements = nSingularElements;
		this.nSyntheses = nSyntheses;
		this.reductionRate = reductionRate;
		this.totalElements = totalElements;
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
		Info other = (Info) obj;
		if (gridSize == null) {
			if (other.gridSize != null)
				return false;
		} else if (!gridSize.equals(other.gridSize))
			return false;
		if (nSingularElements == null) {
			if (other.nSingularElements != null)
				return false;
		} else if (!nSingularElements.equals(other.nSingularElements))
			return false;
		if (nSyntheses == null) {
			if (other.nSyntheses != null)
				return false;
		} else if (!nSyntheses.equals(other.nSyntheses))
			return false;
		if (reductionRate == null) {
			if (other.reductionRate != null)
				return false;
		} else if (!reductionRate.equals(other.reductionRate))
			return false;
		if (totalElements == null) {
			if (other.totalElements != null)
				return false;
		} else if (!totalElements.equals(other.totalElements))
			return false;
		if (zoom == null) {
			if (other.zoom != null)
				return false;
		} else if (!zoom.equals(other.zoom))
			return false;
		return true;
	}

	public Long getGridSize() {
		return gridSize;
	}

	public Long getnSingularElements() {
		return nSingularElements;
	}

	public Long getnSyntheses() {
		return nSyntheses;
	}

	public Double getReductionRate() {
		return reductionRate;
	}

	public Long getTotalElements() {
		return totalElements;
	}

	public Integer getZoom() {
		return zoom;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (gridSize == null ? 0 : gridSize.hashCode());
		result = prime
				* result
				+ (nSingularElements == null ? 0 : nSingularElements.hashCode());
		result = prime * result
				+ (nSyntheses == null ? 0 : nSyntheses.hashCode());
		result = prime * result
				+ (reductionRate == null ? 0 : reductionRate.hashCode());
		result = prime * result
				+ (totalElements == null ? 0 : totalElements.hashCode());
		result = prime * result + (zoom == null ? 0 : zoom.hashCode());
		return result;
	}

	public void setGridSize(final Long gridSize) {
		this.gridSize = gridSize;
	}

	public void setnSingularElements(final Long nSingularElements) {
		this.nSingularElements = nSingularElements;
	}

	public void setnSyntheses(final Long nSyntheses) {
		this.nSyntheses = nSyntheses;
	}

	public void setReductionRate(final Double reductionRate) {
		this.reductionRate = reductionRate;
	}

	public void setTotalElements(final Long totalElements) {
		this.totalElements = totalElements;
	}

	public void setZoom(final Integer zoom) {
		this.zoom = zoom;
	}

	@Override
	public String toString() {
		return "Info [gridSize=" + gridSize + ", nSingularElements="
				+ nSingularElements + ", nSyntheses=" + nSyntheses
				+ ", reductionRate=" + reductionRate + ", totalElements="
				+ totalElements + ", zoom=" + zoom + "]";
	}
}
