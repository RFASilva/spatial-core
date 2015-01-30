package vast_research.spatial_core.jobs.synthesis;

import vast_research.spatial_core.shared.LayerResolution;

public class TweetsExternalFunctions extends ExternalFunctions {

	public TweetsExternalFunctions(final LayerResolution rawLayer) {
		super(rawLayer);
		mapping.put("Density", new String[] { "standardUp", "synthDensity" });
	}

	// for tweets, besides counts, we need density
	public double synthDensity(final Double[] propertiesValues) {
		int numCells = propertiesValues.length;
		double tmpCells = rawset.getGridSize() / numCells;
		tmpCells = tmpCells * tmpCells;
		double ret = standardSynth(propertiesValues);
		return ret / tmpCells;
	}

}
