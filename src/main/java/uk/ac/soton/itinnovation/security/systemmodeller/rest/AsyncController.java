
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

}

