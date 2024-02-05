
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.soton.itinnovation.security.model.system.RiskCalculationMode;
import uk.ac.soton.itinnovation.security.modelquerier.JenaQuerierDB;
import uk.ac.soton.itinnovation.security.modelvalidator.Progress;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.RecommendationsAlgorithmConfig;
import uk.ac.soton.itinnovation.security.modelvalidator.attackpath.dto.RecommendationReportDTO;
import uk.ac.soton.itinnovation.security.semanticstore.AStoreWrapper;
import uk.ac.soton.itinnovation.security.semanticstore.JenaTDBStoreWrapper;
import uk.ac.soton.itinnovation.security.systemmodeller.attackpath.AsyncService.RecStatus;
import uk.ac.soton.itinnovation.security.systemmodeller.attackpath.AsyncService;
import uk.ac.soton.itinnovation.security.systemmodeller.auth.KeycloakAdminClient;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.ModelFactory;
import uk.ac.soton.itinnovation.security.systemmodeller.model.WebKeyRole;
import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.IModelRepository;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.BadRequestErrorException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.BadRiskModeException;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.exceptions.InternalServerErrorException;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;
import uk.ac.soton.itinnovation.security.systemmodeller.util.SecureUrlHelper;

@RestController
@RequestMapping("/async")
public class AsyncController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private IModelRepository modelRepository;

	@Autowired
	private KeycloakAdminClient keycloakAdminClient;

	@Autowired
	private ModelFactory modelFactory;

    @Autowired
    private StoreModelManager storeModelManager;

    @Autowired
    private SecureUrlHelper secureUrlHelper;

    @Value("${admin-role}")
    public String adminRole;

    @Autowired
    private AsyncService asyncService;

	private final ModelObjectsHelper modelObjectsHelper;

    public static class JobResponseDTO {
        private String jobId;
        private String message;
        public JobResponseDTO(String jobId, String msg) {
            this.jobId = jobId;
            this.message = msg;
        }
        public String getJobId() { return this.jobId; }
        public void setJobId(String jobid) { this.jobId = jobid; }
        public String getMessage() { return this.message; }
        public void setMessage(String msg) { this.message = msg; }
    }

    @Autowired
    public AsyncController(ModelObjectsHelper modelObjectsHelper) {
        this.modelObjectsHelper = modelObjectsHelper;
    }

	/**
	 * This REST method generates a recommendation report
	 *
	 * @param modelId the String representation of the model object to seacrh
	 * @param riskMode string indicating the prefered risk calculation mode
	 * @return A JSON report containing recommendations
     * @throws InternalServerErrorException   if an error occurs during report generation
	 */
	@GetMapping(value = "/models/{modelId}/recommendations")
    public ResponseEntity<JobResponseDTO> startRecommendationsTask(
            @PathVariable String modelId,
            @RequestParam (defaultValue = "CURRENT") String riskMode) {

        logger.info("Calculating recommendations for model {}", modelId);
		riskMode = riskMode.replaceAll("[\n\r]", "_");
        logger.info(" riskMode: {}",riskMode);

		try {
            RiskCalculationMode.valueOf(riskMode);
		} catch (IllegalArgumentException e) {
			throw new BadRiskModeException(riskMode);
		}

        final Model model = secureUrlHelper.getModelFromUrlThrowingException(modelId, WebKeyRole.READ);
        Progress progress = modelObjectsHelper.getTaskProgressOfModel("Recommendations", model);
        progress.updateProgress(0d, "Recommendations starting");

		String mId = model.getId();

        AStoreWrapper store = storeModelManager.getStore();

        try {
            JenaQuerierDB querierDB = new JenaQuerierDB(((JenaTDBStoreWrapper) store).getDataset(),
                    model.getModelStack(), true);

            querierDB.init();

            logger.info("Calculating Recommendations");

            logger.info("Creating async job for {}", modelId);

            String jobId = UUID.randomUUID().toString();
            logger.info("submitting async job with id: {}", jobId);

			RecommendationsAlgorithmConfig recaConfig = new RecommendationsAlgorithmConfig(querierDB, mId, riskMode);
            CompletableFuture.runAsync(() -> asyncService.startRecommendationTask(jobId, recaConfig, progress));

            JobResponseDTO response = new JobResponseDTO(jobId, "CREATED");

            return ResponseEntity.ok(response);

        } catch (BadRequestErrorException e) {
            logger.error("mismatch between the stored and requested risk calculation mode, please run the risk calculation");
            throw e;
        } catch (Exception e) {
            logger.error("Threat path failed due to an error", e);
            throw new InternalServerErrorException(
                    "Finding recommendations failed. Please contact support for further assistance.");
        }

    }

    @GetMapping("/models/{modelId}/recommendations/status/{jobId}")
    public ResponseEntity<RecStatus> checkRecJobStatus(
            @PathVariable String modelId, @PathVariable String jobId) {

        logger.info("got request for jobId {} status", jobId);

        return asyncService.getRecStatus(jobId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/models/{modelId}/recommendations/result/{jobId}")
    public ResponseEntity<RecommendationReportDTO> downloadRecommendationsReport(
            @PathVariable String modelId, @PathVariable String jobId) {

        logger.debug("got download request for jobId: {}", jobId);

        return asyncService.getRecReport(jobId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

