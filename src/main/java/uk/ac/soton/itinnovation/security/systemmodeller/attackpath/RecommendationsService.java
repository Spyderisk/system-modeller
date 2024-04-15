/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2023
//
// Copyright in this software belongs to University of Southampton
// IT Innovation Centre of Gamma House, Enterprise Road,
// Chilworth Science Park, Southampton, SO16 7NS, UK.
//
// This software may not be used, sold, licensed, transferred, copied
// or reproduced in whole or in part in any manner or form or in or
// on any media by any person other than in accordance with the terms
// of the Licence Agreement supplied with the software, or otherwise
// without the prior written consent of the copyright owners.
//
// This software is distributed WITHOUT ANY WARRANTY, without even the
// implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
// PURPOSE, except where stated in the Licence Agreement supplied with
// the software.
//
//      Created By:				Panos Melas
//      Created Date:			2023-01-24
//      Created for Project :   Cyberkit4SME
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.attackpath;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;

import uk.ac.soton.itinnovation.security.modelvalidator.Progress;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.RecommendationsAlgorithm;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.RecommendationsAlgorithmConfig;
import uk.ac.soton.itinnovation.security.systemmodeller.model.RecommendationEntity;
import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.RecommendationRepository;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.recommendations.RecommendationReportDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.RiskModeMismatchException;

@Service
public class RecommendationsService {

	private static final Logger logger = LoggerFactory.getLogger(RecommendationsService.class);

    @Autowired
    private RecommendationRepository recRepository;

	@Value("${recommendations.timeout.secs: 900}")
	private Integer recommendationsTimeoutSecs;

    public void startRecommendationTask(String jobId, RecommendationsAlgorithmConfig config, Progress progress) {

        logger.debug("startRecommendationTask for {}", jobId);
        logger.debug("recommendationsTimeoutSecs: {}", this.recommendationsTimeoutSecs);

        // create recEntry and save it to mongo db
        RecommendationEntity recEntity = new RecommendationEntity();
        recEntity.setId(jobId);
        recEntity.setModelId(config.getModelId());
        recEntity.setState(RecommendationJobState.STARTED);
        recRepository.save(recEntity);
        logger.debug("rec entity saved for {}", recEntity.getId());

        try {
			RecommendationsAlgorithm reca = new RecommendationsAlgorithm(config, recommendationsTimeoutSecs);

            if (!reca.checkRiskCalculationMode(config.getRiskMode())) {
                throw new RiskModeMismatchException();
            }

            reca.setRecRepository(recRepository, jobId);

            RecommendationReportDTO report = reca.recommendations(progress);

            storeRecReport(jobId, report);

            RecommendationJobState finalState = reca.getFinalState() != null ? reca.getFinalState() : RecommendationJobState.FINISHED;
            updateRecommendationJobState(jobId, finalState);
        } catch (Exception e) {
            updateRecommendationJobState(jobId, RecommendationJobState.FAILED);
        }
    }

    public void updateRecommendationJobState(String recId, RecommendationJobState newState, String msg) {
        Optional<RecommendationEntity> optionalRec = recRepository.findById(recId);
        optionalRec.ifPresent(rec -> {
            rec.setState(newState);
            rec.setMessage(msg);
            rec.setModifiedAt(LocalDateTime.now());
            recRepository.save(rec);
        });
    }

    public void updateRecommendationJobState(String recId, RecommendationJobState newState) {
        Optional<RecommendationEntity> optionalRec = recRepository.findById(recId);
        optionalRec.ifPresent(rec -> {
            rec.setState(newState);
            rec.setModifiedAt(LocalDateTime.now());
            recRepository.save(rec);
        });
    }

    public void storeRecReport(String jobId, RecommendationReportDTO report) {
        Optional<RecommendationEntity> optionalRec = recRepository.findById(jobId);
        optionalRec.ifPresent(job -> {
            job.setReport(report);
            job.setState(RecommendationJobState.FINISHED);
            job.setModifiedAt(LocalDateTime.now());
            recRepository.save(job);
        });
    }

    public Optional<RecommendationJobState> getRecommendationJobState(String jobId) {
        return recRepository.findById(jobId).map(RecommendationEntity::getState);
    }

    public Optional<String> getRecommendationJobMessage(String jobId) {
        return recRepository.findById(jobId).map(RecommendationEntity::getMessage);
    }


    public Optional<RecommendationReportDTO> getRecReport(String jobId) {
        return recRepository.findById(jobId).map(RecommendationEntity::getReport);
    }

    public Optional<RecommendationEntity> getJobById(String jobId) {
        return recRepository.findById(jobId);
    }

    // TODO: the happy path for a job should be: created -> started-> running
    // -> finished.
    // For cancelled jobs the sequence should be: created -> started -> running
    // -> aborted -> finished?
    //
    // Somehow the recommendation report should indicate what has happened to
    // the job if it was cancelled, because cancelled jobs should still produce
    // results.
    //
    // Then use the RecommendationEntity to store additional information, eg
    // number of recommendations found.
    //
    public enum RecommendationJobState {
        CREATED,
        STARTED,
        RUNNING,
        FAILED,
        FINISHED,
        ABORTED,
        TIMED_OUT,
        UNKNOWN
    }

}

