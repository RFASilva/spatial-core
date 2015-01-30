package vast_research.spatial_core.jobs.synthesis;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import vast_research.spatial_core.shared.LayerResolution;

public class ExternalFunctions implements Serializable {

	private static final long serialVersionUID = 1L;

	// function mapping - by default always count
	// usage:
	// property_name, [up_function_name, synth_function_name]
	public Map<String, String[]> mapping;

	// rawset resolution - needed for synth or up from rawset
	public LayerResolution rawset;

	public ExternalFunctions(final LayerResolution rawLayer) {
		mapping = new HashMap<String, String[]>();
		// always inserts count
		mapping.put("Count", new String[] { "standardUp", "standardSynth" });
		rawset = rawLayer;
	}

	// execute UP according to property name
	public double execute(final String propName, final boolean up,
			final Object[] vals) {
		try {
			int upOrSynth = up ? 0 : 1;
			// getting method instance reflectively
			String function_name = mapping.get(propName)[upOrSynth];

			Class[] paramsType = up ? new Class[] { Double.class }
					: new Class[] { new Double[1].getClass() };

			Method function = this.getClass().getMethod(function_name,
					paramsType);
			return ((Double) function.invoke(this, vals)).doubleValue();

		} catch (Exception ex) {
			ex.printStackTrace();
			return Double.NaN;
		}
	}

	// plain sum
	public double standardSynth(final Double[] propertiesValues) {
		double sum = 0;
		for (double val : propertiesValues)
			sum += val;
		return sum;
	}

	// plain val
	public double standardUp(final Double val) {
		return val;
	}

}
