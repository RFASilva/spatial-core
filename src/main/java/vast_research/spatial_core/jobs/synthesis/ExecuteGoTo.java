package vast_research.spatial_core.jobs.synthesis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vast_research.spatial_core.Server;
import vast_research.spatial_core.jobs.store.StoreEntitiesJob;
import vast_research.spatial_core.resources.DataStoreManager;
import vast_research.spatial_core.resources.Workers;
import vast_research.spatial_core.shared.Entity;
import vast_research.spatial_core.shared.GranularSynthesis;
import vast_research.spatial_core.shared.LayerResolution;
import vast_research.spatial_core.shared.SpatialType;
import vast_research.spatial_core.shared.Zoom;

import com.hazelcast.core.IAtomicLong;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class ExecuteGoTo implements Callable<Envelope>, Serializable {

	private static final long serialVersionUID = 1L;

	private final LayerResolution curResolution;

	private final ExternalFunctions extFunctions;

	// private final Integer limit;
	private final long firstUPGEOHASH;

	// private final Integer offset;
	private final long lastUPGEOHASH;

	private final Logger logger = LoggerFactory.getLogger(Server.class);
	private final LayerResolution newResolution;

	private final Zoom nextLevel;

	public ExecuteGoTo(final Zoom nextLevel, final long firstUPGEOHASH,
			final long lastUPGEOHASH, final LayerResolution curResolution,
			final LayerResolution newResolution, final ExternalFunctions extFunc) {
		this.nextLevel = nextLevel;
		this.firstUPGEOHASH = firstUPGEOHASH;
		this.lastUPGEOHASH = lastUPGEOHASH;
		this.curResolution = curResolution;
		this.newResolution = newResolution;
		extFunctions = extFunc;
	}

	@Override
	public Envelope call() throws Exception {
		logger.info("Executing GOTO - [" + firstUPGEOHASH + " - "
				+ lastUPGEOHASH + "]");

		List<Entity[]> entities = DataStoreManager.getEntityToUp(curResolution,
				firstUPGEOHASH, lastUPGEOHASH, nextLevel);
		List<Entity> result = new LinkedList<Entity>();
		Envelope envelope = new Envelope();
		// double maxPrec = 1;
		IAtomicLong atomicLong = Workers.init().getAtomicLong("atomic_default");
		int synthCount = 0;
		for (Entity[] aux : entities)
			if (aux.length == 1 || !checkIfMustSynth(aux))
				for (Entity entity : aux) {
					envelope.expandToInclude(entity.getGeometry()
							.getEnvelopeInternal());
					// maxPrec = Functions.findBestPrecision(maxPrec,
					// entity.getGeometry());
					result.add(entity);
				}
			else {

				List<GranularSynthesis> synthesis = synthesisOperation(
						"s_" + new Date().getTime() + "_"
								+ atomicLong.getAndIncrement(), aux,
						extFunctions);

				envelope.expandToInclude(synthesis.get(0).getGeometry()
						.getEnvelopeInternal());
				// maxPrec = Functions.findBestPrecision(maxPrec,
				// synthesis.get(0)
				// .getGeometry());
				result.addAll(synthesis);
				synthCount += 1;
			}

		// apenas para termos um envelope qq no new resolution ... no final há
		// save ...
		GeometryFactory gf = new GeometryFactory();
		newResolution.setEnvelope(gf.toGeometry(envelope));
		// newResolution.setPrecision(maxPrec);

		try {
			// Faz sentido lançar um worker? Acho que não!
			Workers.submit(new StoreEntitiesJob(result, newResolution)).get(
					3600, TimeUnit.SECONDS);
			// StoreEntitiesJob st = new StoreEntitiesJob(result,newResolution);
			// st.call();
		} catch (CancellationException exception) {
			exception.printStackTrace();
		} catch (InterruptedException exception) {
			exception.printStackTrace();
		} catch (ExecutionException exception) {
			exception.printStackTrace();
		}

		logger.info("Ending Executing GOTO .... Synths: " + synthCount);
		return envelope;
	}

	private boolean checkIfMustSynth(final Entity[] entities) {
		boolean ret = false;

		// lista de entidades a sintetizar organizadas por Atributo
		HashMap<String, Integer> organizedEntities = new HashMap<String, Integer>();

		// organiza por atributo para organizedEntities
		for (Entity ent : entities) {

			Integer tmpList;
			String tmpAtrb = ent.getAttribute();

			// if any of the entities is SpatiaObject, then synth
			if (!ent.getType().equals(SpatialType.GranularSynthesis)) {
				ret = true;
				break;
			}

			if (organizedEntities.containsKey(tmpAtrb)) {
				ret = true;
				break;
			} else {
				tmpList = 1;
				organizedEntities.put(tmpAtrb, tmpList);
			}
		}

		return ret;
	}

	private List<GranularSynthesis> synthesisOperation(final String id,
			final Entity[] entities,
			final ExternalFunctions datasetExternalFunctions) {
		Geometry newSpatialExpression = entities[0].getGeometry();
		Geometry upSpatialExpression = entities[0].getUpGeometry();
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
			long count = propsSynthed.get("Count").longValue();

			propsSynthed.remove("Count");

			GranularSynthesis oneGrS = new GranularSynthesis(key, count,
					newSpatialExpression, id, propsSynthed,
					upSpatialExpression.hashCode(), upSpatialExpression);
			// guardar references
			oneGrS.setReferences(entitiesIDsToReference);

			// guarda synth na lista para retornar - pode ser só 1 ...
			retGrS.add(oneGrS);
		}

		return retGrS;
	}
}
