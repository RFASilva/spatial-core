package vast_research.spatial_core.jobs.synthesis;

import vast_research.spatial_core.shared.LayerResolution;

public class AccidentsExternalFunctions extends ExternalFunctions {

	public AccidentsExternalFunctions(final LayerResolution rawLayer) {
		super(rawLayer);
		mapping.put("CountPersons", new String[] { "standardUp",
				"standardSynth" });
		mapping.put("CountFatals",
				new String[] { "standardUp", "standardSynth" });
		mapping.put("CountDrunks",
				new String[] { "standardUp", "standardSynth" });
		mapping.put("Density", new String[] { "standardUp", "synthDensity" });
	}

	// for accidents, besides counts, we need density
	public double synthDensity(final Double[] propertiesValues) {
		int numCells = propertiesValues.length;
		double tmpCells = rawset.getGridSize() / numCells;
		tmpCells = tmpCells * tmpCells;
		double ret = standardSynth(propertiesValues);
		return ret / tmpCells;
	}

}
