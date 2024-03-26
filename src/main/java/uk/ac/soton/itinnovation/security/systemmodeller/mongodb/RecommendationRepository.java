package uk.ac.soton.itinnovation.security.systemmodeller.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

import uk.ac.soton.itinnovation.security.systemmodeller.model.RecommendationEntity;

public interface RecommendationRepository extends MongoRepository<RecommendationEntity, String> {
    public RecommendationEntity findOneById(String id);
}

