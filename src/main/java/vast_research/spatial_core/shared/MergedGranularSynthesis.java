package vast_research.spatial_core.shared;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class MergedGranularSynthesis extends Entity {

	private static final long serialVersionUID = 1L;

	private final List<GranularSynthesis> synthesis;

	public MergedGranularSynthesis(final Long count, final Geometry geometry,
			final String id, final Map<String, Double> properties,
			final Integer upGeoHash, final Geometry upGeometry,
			final GranularSynthesis synth) {

		// note: count is sum of syntheses
		// properties are sum of properties of synthesis
		// atribute is "-";
		super("-", count, geometry, id, properties,
				SpatialType.MergedGranularSynthesis, upGeoHash, upGeometry);
		synthesis = new LinkedList<GranularSynthesis>();
		synthesis.add(synth);
	}

	public void addSynthesis(final GranularSynthesis synth) {
		synthesis.add(synth);
		count += synth.count;
		for (String prop : synth.properties.keySet()) {
			Double propValue = synth.properties.get(prop);
			if (properties.containsKey(prop))
				propValue += properties.get(prop);
			properties.put(prop, propValue);
		}
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		MergedGranularSynthesis other = (MergedGranularSynthesis) obj;
		if (synthesis == null) {
			if (other.synthesis != null)
				return false;
		} else if (!synthesis.equals(other.synthesis))
			return false;
		return true;
	}

	public List<GranularSynthesis> getMergedSynthesis() {
		return synthesis;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ (synthesis == null ? 0 : synthesis.hashCode());
		return result;
	}

	@Override
	public String toJson() {
		// StringBuilder propsVals = new StringBuilder();
		//
		// for (GranularSynthesis synth : synthesis) {
		// StringBuilder curpropsVals = new StringBuilder();
		// curpropsVals.append("\"" + synth.attribute + "\",");
		//
		// for (String prop : properties.keySet())
		// curpropsVals
		// .append(synth.properties.get(prop).toString() + ",");
		//
		// propsVals.append("["
		// + curpropsVals.substring(0, curpropsVals.length() - 1)
		// + "],");
		// }
		//
		// String tmpPropsVals = propsVals.substring(0, propsVals.length() - 1);
		//
		// String ret = "{\"g\": {\"t\": \"Point\",\"c\": ["
		// + ((Point) geometry).getX() + ", " + ((Point) geometry).getY()
		// + "]},\"p\": {" + "\"v\": [" + tmpPropsVals + "]," + "\"s\":"
		// + "\"true\"}}";
		//
		// return ret;

		StringBuilder propsVals = new StringBuilder();

		for (GranularSynthesis synth : synthesis) {
			StringBuilder curpropsVals = new StringBuilder();
			curpropsVals.append("\"").append(synth.attribute).append("\",")
					.append(synth.count).append(",");

			for (String prop : properties.keySet())
				curpropsVals.append(synth.properties.get(prop).toString())
						.append(",");

			propsVals
					.append("[")
					.append(curpropsVals.substring(0, curpropsVals.length() - 1))
					.append("],");
		}

		String tmpPropsVals = propsVals.substring(0, propsVals.length() - 1);

		StringBuilder ret = new StringBuilder();

		ret.append("{\"g\":{\"t\":\"Point\",\"c\":[")
				.append(((Point) geometry).getX()).append(", ")
				.append(((Point) geometry).getY()).append("]},\"p\":{")
				.append("\"v\":[").append(tmpPropsVals)
				.append("],\"s\":\"true\"}}");

		return ret.toString();

	}

	@Override
	public String toString() {
		return "MergedGranularSynthesis [attribute=" + attribute + ", count="
				+ count + ", geometry=" + geometry + ", id=" + id
				+ ", properties=" + properties + ", type=" + type
				+ ", upGeoHash=" + upGeoHash + ", upGeometry=" + upGeometry
				+ ", synthesis=" + synthesis + "]";
	}

}
