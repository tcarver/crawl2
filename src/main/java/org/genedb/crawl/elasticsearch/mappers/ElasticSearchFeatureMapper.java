package org.genedb.crawl.elasticsearch.mappers;

import java.lang.reflect.Field;
import java.util.List;

import org.apache.log4j.Logger;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.client.action.index.IndexRequestBuilder;
import org.genedb.crawl.mappers.FeatureMapper;
import org.genedb.crawl.model.Coordinates;
import org.genedb.crawl.model.Cvterm;
import org.genedb.crawl.model.Feature;
import org.genedb.crawl.model.FeatureProperty;
import org.genedb.crawl.model.LocatedFeature;
import org.springframework.stereotype.Component;

@Component
public class ElasticSearchFeatureMapper extends ElasticSearchBaseMapper implements FeatureMapper {
	
	private Logger logger = Logger.getLogger(ElasticSearchFeatureMapper.class);
	
	@Override
	public Feature get(String uniqueName, String organism_id, String name) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public LocatedFeature get(String uniqueName) {
		try {
			return (LocatedFeature) jsonIzer.fromJson (getFromElastic(connection.getIndex(), connection.getFeatureType(), uniqueName), LocatedFeature.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public List<FeatureProperty> properties(Feature feature) {
		return feature.properties;
	}

	@Override
	public List<Cvterm> terms(Feature feature) {
		return feature.terms;
	}
	
	@Override
	public List<Coordinates> coordinates(Feature feature) {
		return feature.coordinates;
	}
	
//	public void createOrUpdate(ElasticSequence sequence) {
//		
//		try {
//			String json = jsonIzer.toJson(sequence);
//			
//			logger.debug("Storing sequence: " + sequence.name);
//			
//			connection.getClient().prepareIndex("sequences", "Sequence", sequence.name)
//				.setSource(json)
//				.execute()
//				.actionGet();
//		
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//		
//	}
	
	public void createOrUpdate(Feature feature) {
		
		if (feature.coordinates != null && feature.coordinates.size() > 0) {
			if (! (feature instanceof LocatedFeature)) {
				
				LocatedFeature lFeature = new LocatedFeature();
				
				for (Field field : Feature.class.getFields()) {
					try {
						field.set(lFeature, field.get(feature));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				
				Coordinates c = feature.coordinates.get(0);
				
				lFeature.fmax = c.fmax;
				lFeature.fmin = c.fmin;
				lFeature.region = c.region;
				lFeature.phase = c.phase;
				lFeature.strand = c.strand;
				
				feature = lFeature;
				
			}
		}
		
		try {
			
			logger.debug("Storing: " + feature.uniqueName);
			
			IndexRequestBuilder builder = connection.getClient().prepareIndex( connection.getIndex(), connection.getFeatureType(), feature.uniqueName);
			String json = jsonIzer.toJson(feature);
			
//			
			logger.debug("Source:");
			logger.debug(json);
			
			builder.setSource(json);
			
//			if (feature instanceof LocatedFeature) {
//				LocatedFeature lFeature = (LocatedFeature) feature;
//				if (lFeature.parent != null) {
//					logger.debug(String.format("Setting %s as parent of %s!", lFeature.parent, feature.uniqueName));
//					builder.setParent(lFeature.parent);
//				}
//			}
			
			//logger.debug(connection.getClient().prepareGet(index, type, feature.uniqueName).execute().actionGet().sourceAsString());
			
			
			builder.execute().actionGet();
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void delete(Feature feature) {
		logger.debug("Deleting " + feature.uniqueName);
		DeleteResponse response = connection
			.getClient()
			.prepareDelete()
			.setIndex(connection.getIndex())
			.setType(connection.getFeatureType())
			.setId(feature.uniqueName)
			.execute()
			.actionGet();
		
		if (response.isNotFound()) {
			logger.warn(feature.uniqueName + " not found");
		}
	}

	
//	public static String getIndex() {
//		return "features";
//	}
//
//	
//	public static String getType() {
//		return "Feature";
//	}
	
}
