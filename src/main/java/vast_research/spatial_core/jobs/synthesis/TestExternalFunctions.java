package vast_research.spatial_core.jobs.synthesis;

import vast_research.spatial_core.shared.LayerResolution;

public class TestExternalFunctions extends ExternalFunctions {

	public TestExternalFunctions(final LayerResolution rawLayer) {
		super(rawLayer);
		mapping.put("CountPersons", new String[] { "standardUp",
				"standardSynth" });
		mapping.put("CountFatals",
				new String[] { "standardUp", "standardSynth" });
		mapping.put("CountDrunks",
				new String[] { "standardUp", "standardSynth" });
	}
}
