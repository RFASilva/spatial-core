package vast_research.spatial_core.jobs.query;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vast_research.spatial_core.Server;
import vast_research.spatial_core.resources.DataStoreManager;
import vast_research.spatial_core.shared.Entity;
import vast_research.spatial_core.shared.GranularSynthesis;
import vast_research.spatial_core.shared.LayerResolution;
import vast_research.spatial_core.shared.MergedGranularSynthesis;
import vast_research.spatial_core.shared.SpatialType;

import com.vividsolutions.jts.geom.Envelope;

public class ExecuteQuery implements Callable<StringBuilder>, Serializable {
	// Callable<List<Entity>> {

	private static final long serialVersionUID = 1L;

	private final Envelope envelope;

	private final long firstUPGEOHASH;

	private final long lastUPGEOHASH;

	private final Logger logger = LoggerFactory.getLogger(Server.class);

	private final LayerResolution resolution;

	public ExecuteQuery(final long firstUPGEOHASH, final long lastUPGEOHASH,
			final LayerResolution resolution, final Envelope envelope) {
		this.firstUPGEOHASH = firstUPGEOHASH;
		this.lastUPGEOHASH = lastUPGEOHASH;
		this.resolution = resolution;
		this.envelope = envelope;
	}

	@Override
	public StringBuilder call() throws Exception {
		// public List<Entity> call() {

		StringBuilder ret = new StringBuilder();

		// é preciso juntar as GranularSynthesis todas
		// 1) criar um object GroupedGranularSynthesis
		// 2) passar das entities para uma lista de entities onde as
		// granularSynthesis são grouped

		logger.info("Starting getting entities & merging & writing synths JOB ["
				+ firstUPGEOHASH
				+ " - "
				+ lastUPGEOHASH
				+ "] for Envelope: ["
				+ envelope.toString() + "]");

		Long time = System.currentTimeMillis();

		String singleEntitites = DataStoreManager
				.getSingleEntitiesOptimizedForGet(resolution, envelope,
						firstUPGEOHASH, lastUPGEOHASH);

		logger.info("Getting SINGLE ENTITIES done in "
				+ (System.currentTimeMillis() - time) + "ms.");

		List<Entity> entities = DataStoreManager
				.getSynthEntitiesOptimizedForGet(resolution, envelope,
						firstUPGEOHASH, lastUPGEOHASH);

		logger.info("Getting SYNTH ENTITIES done in "
				+ (System.currentTimeMillis() - time) + "ms.");

		// para retorno, lista final de merged entities
		// LinkedList<Entity> mergedEntities = new LinkedList<Entity>();

		// map de controlo de synths - nota: Entity mas poderão ser Granular ou
		// Merged
		HashMap<String, Entity> synthIDs = new HashMap<String, Entity>();

		// ciclo para processar as entidades, cada granular guarda como granular
		// or merge.
		for (Entity entity : entities)
			// if (entity.getType().equals(SpatialType.GranularSynthesis)) {
			// é synth, mete no map se não existir ou merge com outra
			if (synthIDs.containsKey(entity.getId())) {
				// já tenho, faz merge
				Entity existingSynth = synthIDs.get(entity.getId());
				// caso seja merged faz add, caso contrario, cria e faz add
				if (existingSynth.getType().equals(
						SpatialType.MergedGranularSynthesis)) {
					// já sou merge, faz apenas add
					MergedGranularSynthesis alreadMerged = (MergedGranularSynthesis) existingSynth;
					alreadMerged.addSynthesis((GranularSynthesis) entity);
				} else {
					// sou apenas synth, cria Merged, faz add da actual
					// synth e da entity, e replace da actual synth por esta
					MergedGranularSynthesis newMerged = new MergedGranularSynthesis(
							existingSynth.getCount(),
							existingSynth.getGeometry(), existingSynth.getId(),
							existingSynth.getProperties(),
							existingSynth.getUpGeoHash(),
							existingSynth.getUpGeometry(),
							(GranularSynthesis) existingSynth);

					newMerged.addSynthesis((GranularSynthesis) entity);
					synthIDs.put(existingSynth.getId(), newMerged);
				}
			} else
				// não existe ainda?, mete no synthID
				synthIDs.put(entity.getId(), entity);
		// } else {
		// // não é synth, guarda
		// // mergedEntities.add(entity);
		// ret.append(entity.toJson());
		// ret.append(",");
		// }

		// merge singlePoints
		if (!singleEntitites.isEmpty())
			ret.append(singleEntitites).append(",");

		// junta no mergedEntities
		for (String synthID : synthIDs.keySet()) {
			Entity toAdd = synthIDs.get(synthID);
			ret.append(toAdd.toJson());
			ret.append(",");
			// mergedEntities.add(toAdd);
		}

		// logger.info("Getting+Merging ENTITIES done in "
		// + (System.currentTimeMillis() - time) + "ms.");

		// for (Entity ent : mergedEntities)
		// writer.write(ent.toJson() + ",");

		logger.info("Ending getting entities & merging & writing synths JOB.");

		logger.info("Getting+Merging+Writing ENTITIES done in "
				+ (System.currentTimeMillis() - time) + "ms.");

		// return mergedEntities;
		return ret;
	}
}
