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

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import java.time.LocalDateTime;
import java.util.Optional;

import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.RecommendationsAlgorithm;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.RecommendationsAlgorithmConfig;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.RecommendationReportDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.model.RecommendationEntity;
import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.RecommendationRepository;

@Service
public class AsyncService {

	private static final Logger logger = LoggerFactory.getLogger(AsyncService.class);

    @Autowired
    private RecommendationRepository recRepository;

    private void performRecommendationCalculation(String modelId, String riskMode) {
        logger.debug("performRecommendationCalculation");
        try {
            Thread.sleep(50000);
            logger.debug("finished long running job");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void startRecommendationTask(String jobId, RecommendationsAlgorithmConfig config) {

        logger.debug("startRecommendationTask for {}", jobId);

        // create recEntry and save it to mongo db
        RecommendationEntity recEntity = new RecommendationEntity();
        recEntity.setId(jobId);
        recEntity.setModelId(config.getModelId());
        recEntity.setStatus(RecStatus.STARTED);
        recRepository.save(recEntity);
        logger.debug("rec entity saved for {}", recEntity.getId());

        try {
			RecommendationsAlgorithm reca = new RecommendationsAlgorithm(config);

            if (!reca.checkRiskCalculationMode(config.getRiskMode())) {
                logger.error("mismatch in risk calculation mode found");
                throw new Exception("mismatch between the stored and requested risk calculation mode, please run the risk calculation");
            }

            RecommendationReportDTO report = reca.recommendations();

            storeRecReport(jobId, report);

            updateRecStatus(jobId, RecStatus.FINISHED);
        } catch (Exception e) {
            updateRecStatus(jobId, RecStatus.FAILED);
        }
    }

    @Async
    public CompletableFuture<String> startRecommendationTaskOut(String modelId, String riskMode) {

        logger.debug("startRecommendationTask");
        // create recEntry and save it to mongo db
        RecommendationEntity recEntity = new RecommendationEntity();
        recEntity.setStatus(RecStatus.STARTED);
        recRepository.save(recEntity);

        String jobId = recEntity.getId();
        logger.debug("startRecommendationTask got jobId {}", jobId);

        try {
            performRecommendationCalculation(modelId, riskMode);

            updateRecStatus(jobId, RecStatus.FINISHED);
        } catch (Exception e) {
            updateRecStatus(jobId, RecStatus.FAILED);
        }

        // Return the job ID immediately
        return CompletableFuture.completedFuture(jobId);
    }

    public void updateRecStatus(String recId, RecStatus newStatus) {
        Optional<RecommendationEntity> optionalRec = recRepository.findById(recId);
        optionalRec.ifPresent(rec -> {
            rec.setStatus(newStatus);
            rec.setModifiedAt(LocalDateTime.now());
            recRepository.save(rec);
        });
    }

    public void storeRecReport(String jobId, RecommendationReportDTO report) {
        Optional<RecommendationEntity> optionalRec = recRepository.findById(jobId);
        optionalRec.ifPresent(job -> {
            job.setReport(report);
            job.setStatus(RecStatus.FINISHED);
            job.setModifiedAt(LocalDateTime.now());
            recRepository.save(job);
        });
    }

    public Optional<RecStatus> getRecStatus(String jobId) {
        return recRepository.findById(jobId).map(RecommendationEntity::getStatus);
    }

    public Optional<RecommendationReportDTO> getRecReport(String jobId) {
        return recRepository.findById(jobId).map(RecommendationEntity::getReport);
    }

    public Optional<RecommendationEntity> getJobById(String jobId) {
        return recRepository.findById(jobId);
    }

    public static enum RecStatus {
        CREATED,
        STARTED,
        RUNNING,
        FAILED,
        FINISHED
    }

}

