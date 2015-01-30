package vast_research.spatial_core.jobs.synthesis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import vast_research.spatial_core.shared.Entity;
import vast_research.spatial_core.shared.GranularSynthesis;
import vast_research.spatial_core.shared.LayerResolution;
import vast_research.spatial_core.shared.SpatialObject;
import vast_research.spatial_core.shared.SpatialType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class TmpClass {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		TmpClass me = new TmpClass();

		// preciso de uma resolução (TMP), external, e lista de entidades
		LinkedList<String> tmpList = new LinkedList<String>();
		tmpList.add("Count");
		tmpList.add("Density");
		LayerResolution tmpRes = new LayerResolution("tweets_raw", new Double(
				0.0001), tmpList, -1);
		tmpRes.setGridSize(1024L);

		TweetsExternalFunctions tmpExternal = new TweetsExternalFunctions(
				tmpRes);

		GeometryFactory geoF = new GeometryFactory();
		Point sameGeo = geoF.createPoint(new Coordinate(1, 5));

		HashMap<String, Double> tmpProp = new HashMap<String, Double>();
		tmpProp.put("Density", new Double(1));

		SpatialObject ent1 = new SpatialObject("t1", sameGeo, "1", tmpProp,
				SpatialType.Point, new Integer(0), sameGeo);

		SpatialObject ent2 = new SpatialObject("t1", sameGeo, "2", tmpProp,
				SpatialType.Point, new Integer(0), sameGeo);

		SpatialObject ent3 = new SpatialObject("t1", sameGeo, "3", tmpProp,
				SpatialType.Point, new Integer(0), sameGeo);

		HashMap<String, Double> tmpSynthProp = new HashMap<String, Double>();
		tmpSynthProp.put("Density", 0.5);

		GranularSynthesis synth1 = new GranularSynthesis("t1", 4L, sameGeo,
				"4", tmpSynthProp, new Integer(0), sameGeo);

		GranularSynthesis synth2 = new GranularSynthesis("t1", 4L, sameGeo,
				"5", tmpSynthProp, new Integer(0), sameGeo);

		Entity[] ents = new Entity[] { ent1, ent2, ent3, synth1, synth2 };

		List<GranularSynthesis> ret = TmpClass.SynthesisOperation("6", ents,
				sameGeo, sameGeo, tmpExternal);

		System.out.println(ret);

	}

	// função que executa a synthesis tendo por base uma resolução (a nova) e um
	// conjunto de entidades (da antiga resolução), bem como a nova expressão
	// espacial e um ID já anteriormente dado
	// retorna, naturalmente uma ou mais sinteses granulars (por tipo)
	public static List<GranularSynthesis> SynthesisOperation(final String id,
			final Entity[] entities, final Geometry newSpatialExpression,
			final Geometry upSpatialExpression,
			final ExternalFunctions datasetExternalFunctions) {

		List<GranularSynthesis> retGrS = new LinkedList<GranularSynthesis>();

		// lista de entidades a sintetizar organizadas por Atributo
		HashMap<String, List<Entity>> organizedEntities = new HashMap<String, List<Entity>>();

		// organiza por atributo para organizedEntities
		for (Entity ent : entities) {
			List<Entity> tmpList;
			String tmpAtrb = ent.getAttribute();
			if (organizedEntities.containsKey(tmpAtrb))
				tmpList = organizedEntities.get(tmpAtrb);
			else {
				tmpList = new LinkedList<Entity>();
				organizedEntities.put(tmpAtrb, tmpList);
			}
			tmpList.add(ent);
		}

		// para cada atributo e respectiva lista de entidades (poderão ser
		// spatialobject ou sinteses), efectuar a sintese
		for (String key : organizedEntities.keySet()) {
			// irá guardar nesta a lista consolidada.
			List<Entity> toConsolidateList = organizedEntities.get(key);
			// lista de IDs das entidades que foram sintetizadas
			List<String> entitiesIDsToReference = new LinkedList<String>();

			// obtem Map temporário com propriedades e array de valores de cada
			// entidade - para enviar para sintese.
			Map<String, ArrayList<Double>> propsToSynth = new HashMap<String, ArrayList<Double>>();

			// adiciona count para a sinteses
			ArrayList<Double> countArray = new ArrayList<Double>();
			propsToSynth.put("Count", countArray);

			for (Entity ent : toConsolidateList) {
				// guarda ID para ref ..
				entitiesIDsToReference.add(ent.getId());

				// adiciciona o count como propriedade
				countArray.add(new Double(ent.getCount()));

				// corre as propriedades e organiza.
				for (Entry<String, Double> prop : ent.getProperties()
						.entrySet()) {
					ArrayList<Double> propsVals;
					if (propsToSynth.containsKey(prop.getKey()))
						propsVals = propsToSynth.get(prop.getKey());
					else {
						// cria array
						propsVals = new ArrayList<Double>();
						propsToSynth.put(prop.getKey(), propsVals);
					}
					propsVals.add(prop.getValue());
				}
			}

			// depois, executa a sintese para cada propriedade e respectivo
			// array de valores, retornando apenas 1 valor - será tudo guardado
			// em propsSynthed
			Map<String, Double> propsSynthed = new HashMap<String, Double>();
			for (Entry<String, ArrayList<Double>> tmpPropToSynth : propsToSynth
					.entrySet()) {

				Double[] tmpValues = tmpPropToSynth.getValue().toArray(
						new Double[tmpPropToSynth.getValue().size()]);

				double synthVal = datasetExternalFunctions.execute(
						tmpPropToSynth.getKey(), false,
						new Object[] { tmpValues });
				propsSynthed.put(tmpPropToSynth.getKey(), synthVal);
			}

			// com base nas propriedades e gera nova sintese granular, para o
			// atributo key
			// nota geoHash vem a 0 para já ... é preciso computar no fim?
			long count = propsSynthed.get("Count").longValue();
			propsSynthed.remove("Count");

			GranularSynthesis oneGrS = new GranularSynthesis(key, count,
					newSpatialExpression, id, propsSynthed, new Integer(0),
					upSpatialExpression);
			// guardar references
			oneGrS.setReferences(entitiesIDsToReference);

			// guarda synth na lista para retornar - pode ser só 1 ...
			retGrS.add(oneGrS);
		}

		return retGrS;
	}

	// just for synthOperation
	public TmpClass() {

	}
}
