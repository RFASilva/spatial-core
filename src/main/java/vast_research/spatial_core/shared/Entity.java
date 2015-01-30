package vast_research.spatial_core.shared;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTWriter;

public abstract class Entity implements Serializable {

	private static final long serialVersionUID = 1L;

	protected String attribute;

	protected Long count;

	protected Geometry geometry;

	protected String id;

	protected Map<String, Double> properties = new HashMap<String, Double>();

	protected SpatialType type;

	protected Integer upGeoHash;

	protected Geometry upGeometry;

	public Entity(final String attribute, final Long count,
			final Geometry geometry, final String id,
			final Map<String, Double> properties, final SpatialType type,
			final Integer upGeoHash, final Geometry upGeometry) {
		this.attribute = attribute;
		this.count = count;
		this.geometry = geometry;
		this.id = id;
		this.properties = properties;
		this.type = type;
		this.upGeoHash = upGeoHash;
		this.upGeometry = upGeometry;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Entity other = (Entity) obj;
		if (attribute == null) {
			if (other.attribute != null)
				return false;
		} else if (!attribute.equals(other.attribute))
			return false;
		if (count == null) {
			if (other.count != null)
				return false;
		} else if (!count.equals(other.count))
			return false;
		if (geometry == null) {
			if (other.geometry != null)
				return false;
		} else if (!geometry.equals(other.geometry))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		if (type != other.type)
			return false;
		if (upGeoHash == null) {
			if (other.upGeoHash != null)
				return false;
		} else if (!upGeoHash.equals(other.upGeoHash))
			return false;
		if (upGeometry == null) {
			if (other.upGeometry != null)
				return false;
		} else if (!upGeometry.equals(other.upGeometry))
			return false;
		return true;
	}

	public String getAttribute() {
		return attribute;
	}

	public Long getCount() {
		return count;
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public String getId() {
		return id;
	}

	public Map<String, Double> getProperties() {
		return properties;
	}

	public SpatialType getType() {
		return type;
	}

	public Integer getUpGeoHash() {
		return upGeoHash;
	}

	public Geometry getUpGeometry() {
		return upGeometry;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (attribute == null ? 0 : attribute.hashCode());
		result = prime * result + (count == null ? 0 : count.hashCode());
		result = prime * result + (geometry == null ? 0 : geometry.hashCode());
		result = prime * result + (id == null ? 0 : id.hashCode());
		result = prime * result
				+ (properties == null ? 0 : properties.hashCode());
		result = prime * result + (type == null ? 0 : type.hashCode());
		result = prime * result
				+ (upGeoHash == null ? 0 : upGeoHash.hashCode());
		result = prime * result
				+ (upGeometry == null ? 0 : upGeometry.hashCode());
		return result;
	}

	public void setAttribute(final String attribute) {
		this.attribute = attribute;
	}

	public void setCount(final Long count) {
		this.count = count;
	}

	public void setGeometry(final Geometry geometry) {
		this.geometry = geometry;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public void setProperties(final Map<String, Double> properties) {
		this.properties = properties;
	}

	public void setType(final SpatialType type) {
		this.type = type;
	}

	public void setUpGeoHash(final Integer upGeoHash) {
		this.upGeoHash = upGeoHash;
	}

	public void setUpGeometry(final Geometry upGeometry) {
		this.upGeometry = upGeometry;
	}

	public String toCSV() {
		WKTWriter wkt = new WKTWriter();
		String ret = getId() + ";" + getType().toString() + ";"
				+ wkt.write(getGeometry()) + ";" + getAttribute() + ";"
				+ getCount().toString();
		for (String prop : getProperties().keySet())
			ret += ";" + getProperties().get(prop).toString();
		return ret;
	}

	public String toJson() {
		// StringBuilder propsVals = new StringBuilder();
		//
		// for (String prop : properties.keySet())
		// propsVals.append(properties.get(prop).toString() + ",");
		//
		// String tmpPropsVals = "\"" + attribute + "\","
		// + propsVals.substring(0, propsVals.length() - 1);
		// if (getType().equals(SpatialType.GranularSynthesis))
		// tmpPropsVals = "[" + tmpPropsVals + "]";
		//
		// String ret = "{\"g\": {\"t\": \"Point\",\"c\": ["
		// + ((Point) geometry).getX() + ", " + ((Point) geometry).getY()
		// + "]},\"p\": {"
		// // + "\"d\": [\"Atribute\"," + tmpPropsDescs+ "],"
		// + "\"v\": [" + tmpPropsVals + "]," + "\"s\":" + "\""
		// + getType().equals(SpatialType.GranularSynthesis) + "\"}}";
		// return ret;

		StringBuilder propsVals = new StringBuilder();

		for (String prop : properties.keySet())
			propsVals.append(properties.get(prop).toString()).append(",");

		StringBuilder tmpPropsVals = new StringBuilder();
		tmpPropsVals.append("\"").append(attribute).append("\",").append(count)
				.append(",")
				.append(propsVals.substring(0, propsVals.length() - 1));

		if (getType().equals(SpatialType.GranularSynthesis)) {
			StringBuilder tmp = new StringBuilder();
			tmp.append("[").append(tmpPropsVals).append("]");
			tmpPropsVals = tmp;
		}

		StringBuilder ret = new StringBuilder();
		ret.append("{\"g\":{\"t\":\"Point\",\"c\":[")
				.append(((Point) geometry).getX()).append(", ")
				.append(((Point) geometry).getY()).append("]},\"p\":{\"v\":[")
				.append(tmpPropsVals).append("],\"s\":\"")
				.append(getType().equals(SpatialType.GranularSynthesis))
				.append("\"}}");

		return ret.toString();
	}

	@Override
	public String toString() {
		return "Entity [attribute=" + attribute + ", count=" + count
				+ ", geometry=" + geometry + ", id=" + id + ", properties="
				+ properties + ", type=" + type + ", upGeoHash=" + upGeoHash
				+ ", upGeometry=" + upGeometry + "]";
	}
}
