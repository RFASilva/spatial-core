package vast_research.spatial_core.shared;

import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;

public class SpatialObject extends Entity {

	private static final long serialVersionUID = 1L;

	public SpatialObject(final String attribute, final Geometry geometry,
			final String id, final Map<String, Double> properties,
			final SpatialType type, final Integer upGeoHash,
			final Geometry upGeometry) {
		super(attribute, 1L, geometry, id, properties, type, upGeoHash,
				upGeometry);
	}

	@Override
	public String toString() {
		return "SpatialObject [attribute=" + attribute + ", count=" + count
				+ ", geometry=" + geometry + ", id=" + id + ", properties="
				+ properties + ", type=" + type + ", upGeoHash=" + upGeoHash
				+ "]";
	}

}
