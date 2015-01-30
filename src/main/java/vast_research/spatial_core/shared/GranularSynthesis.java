package vast_research.spatial_core.shared;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;

public class GranularSynthesis extends Entity {

	private static final long serialVersionUID = 1L;

	private List<String> references = new LinkedList<String>();

	public GranularSynthesis(final String attribute, final Long count,
			final Geometry geometry, final String id,
			final Map<String, Double> properties, final Integer upGeoHash,
			final Geometry upGeometry) {
		super(attribute, count, geometry, id, properties,
				SpatialType.GranularSynthesis, upGeoHash, upGeometry);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		GranularSynthesis other = (GranularSynthesis) obj;
		if (references == null) {
			if (other.references != null)
				return false;
		} else if (!references.equals(other.references))
			return false;
		return true;
	}

	public List<String> getReferences() {
		return references;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ (references == null ? 0 : references.hashCode());
		return result;
	}

	public void setReferences(final Iterable<String> references) {
		this.references = new LinkedList<String>();
		for (String reference : references)
			// this.references.add(reference.replaceAll("\\s+", ""));
			this.references.add(reference);
	}

	public void setReferences(final String[] references) {
		this.references = new LinkedList<String>();
		for (String reference : references)
			this.references.add(reference);
	}

	@Override
	public String toString() {
		return "GranularSynthesis [references=" + references + ", attribute="
				+ attribute + ", count=" + count + ", geometry=" + geometry
				+ ", id=" + id + ", properties=" + properties + ", type="
				+ type + ", upGeoHash=" + upGeoHash + "]";
	}

}
