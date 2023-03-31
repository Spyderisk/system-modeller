/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2018
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
//      Created By :            Anna Brown 
//      Created Date :          7-09-2021
//      Created for Project :   Spyderisk-Accelerator
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestContextManager;

import uk.ac.soton.itinnovation.security.systemmodeller.CommonTestSetup;
import uk.ac.soton.itinnovation.security.systemmodeller.SystemModellerApplication;
import uk.ac.soton.itinnovation.security.systemmodeller.TestHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.auth.KeycloakAdminClient;
import uk.ac.soton.itinnovation.security.systemmodeller.model.Model;
import uk.ac.soton.itinnovation.security.systemmodeller.model.ModelFactory;
import uk.ac.soton.itinnovation.security.systemmodeller.model.WebKeyRole;
import uk.ac.soton.itinnovation.security.systemmodeller.mongodb.IModelRepository;
import uk.ac.soton.itinnovation.security.systemmodeller.rest.dto.ModelDTO;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;
import uk.ac.soton.itinnovation.security.systemmodeller.util.SecureUrlHelper;

//@RunWith(SpringRunner.class)
@RunWith(Parameterized.class)
@SpringBootTest(classes = SystemModellerApplication.class, webEnvironment = RANDOM_PORT)
public class ModelControllerParameterisedTest extends CommonTestSetup{

	private static final Logger logger = LoggerFactory.getLogger(ModelControllerTest.class);

	private TestContextManager testContextManager;

	@Parameter(value = 0)
    public String methodname;

	@Parameter(value = 1)
    public String url;
	
	@Parameter(value = 2)
    public RestType restType;

	@Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> params = new ArrayList<Object[]>();
        params.add(new Object[]{"getModel", "/", RestType.GET});
        params.add(new Object[]{"getModelAndRisks", "/risks", RestType.GET});
        params.add(new Object[]{"getModelRiskVector", "/riskvector", RestType.GET});
        //params.add(new Object[]{"getPalette", "/palette", RestType.GET});
        //params.add(new Object[]{"updateModel", "/", RestType.PUT});
        //params.add(new Object[]{"checkoutModel", "/checkout", RestType.POST});
        //params.add(new Object[]{"checkinModel", "/checkin", RestType.POST});
        //params.add(new Object[]{"validateModel", "/validated", RestType.GET});
        //params.add(new Object[]{"calculateRisks", "/calc_risks", RestType.GET});
        //params.add(new Object[]{"deleteModel", "/", RestType.DELETE});
        params.add(new Object[]{"export", "/export", RestType.GET});
        params.add(new Object[]{"exportAsserted", "/exportAsserted", RestType.GET});
        params.add(new Object[]{"getValidationProgress", "/validationprogress", RestType.GET});
        params.add(new Object[]{"getRiskCalcProgress", "/riskcalcprogress", RestType.GET});
        //params.add(new Object[]{"getLoadingProgress", "/loadingprogress", RestType.GET});
        params.add(new Object[]{"generateReport", "/report", RestType.GET});

        return params;
    }


	//System objects, spying on secureUrlHelper to control model access

	@Autowired
	private ModelFactory modelFactory;

	@Autowired
	private StoreModelManager storeModelManager;

	@SpyBean
	private SecureUrlHelper secureUrlHelper;

	@Autowired
	private IModelRepository modelRepository;

	@Autowired
	private KeycloakAdminClient keycloakAdminClient;

	//Auth variables
	@Value("${server.servlet.contextPath}")
	private String contextPath;

	@LocalServerPort
	int port;

	//Allows automatic logging of test names
	@Rule
	public TestName name = new TestName();

	//Provides model control and access
	private Model testModel;
	private String testUserId;
	private static TestHelper testHelper;

	@BeforeClass
	public static void beforeClass() {
		logger.info("Setting up ModelControllerTest class");
		testHelper = new TestHelper("jena-tdb");
		testHelper.setUp();
	}

	@Before
	public void setUp() throws Exception {
		logger.info("Executing {}", name.getMethodName());
		this.testContextManager = new TestContextManager(getClass());
        this.testContextManager.prepareTestInstance(this);
		super.setUp();
		initAuth(contextPath, port);
		testUserId = keycloakAdminClient.getUserByUsername(testUserName).getId();
		modelRepository.deleteAll();
	}

	//Utilities

	private void switchToSystemModel(int modelIndex) {
		testHelper.switchModels(0, modelIndex);

		//model graph(s) already in Jena - but model not in Mongo or the management graph
		testModel = modelFactory.getModel(testHelper.getModel(), testHelper.getStore());

		//insert the model into Mongo to generate a model ID
		testModel.setUserId(testUserId);
		testModel.save();

		//insert the model into the management graph
		storeModelManager.createSystemModel(
			testModel.getUri(),
			testModel.getId(),
			testModel.getUserId(),
			testModel.getDomainGraph()
		);

		//bypass looking model up in Mongo via WebKey (the WebKey is NOT the model ID)
		when(secureUrlHelper.getModelFromUrl("testModel")).thenReturn(testModel);
		when(secureUrlHelper.getRoleFromUrl("testModel")).thenReturn(WebKeyRole.NONE);
		when(secureUrlHelper.getWriteModelFromUrl("testModel")).thenReturn(testModel);
		when(secureUrlHelper.getReadModelFromUrl("testModel")).thenReturn(testModel);

		new ModelDTO(testModel);
	}

	//Tests

	/**
	 * Test of getting model that does not exist for all urls listed in parameters
	 * Asserts NOT FOUND 404 status
	 */
	@Test
	public void testGetResourceInvalidModel() {
		switch(restType){
			case GET:
				given().
					// log in testUser
					filter(userSession).
				when().
					get("/models/notATestModel" + url).
				then().
					assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
				break;
			case PUT:
				given().
					// log in testUser
					filter(userSession).
				when().
					put("/models/notATestModel" + url).
				then().
					assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
				break;
			case POST:
				given().
					// log in testUser
					filter(userSession).
				when().
					post("/models/notATestModel" + url).
				then().
					assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
				break;
			case DELETE:
				given().
					// log in testUser
					filter(userSession).
				when().
					delete("/models/notATestModel" + url).
				then().
					assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
				break;
		}
	}

	@Test
	public void testGetResourceUserNotFound() {
		// enable testModel
		switchToSystemModel(0);

		switch(restType){
			case GET:
				given().
					// don't log a user in
				when().
					get("/models/testModel" + url).
				then().
					assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED).
					and().
					assertThat().body("message", is("Attempting access without a valid username. Are you logged in?"));
				break;
			case PUT:
				given().
					// don't log a user in
				when().
					put("/models/testModel" + url).
				then().
					assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED).
					and().
					assertThat().body("message", is("Attempting access without a valid username. Are you logged in?"));
				break;
			case POST:
				given().
					// don't log a user in
					//Header forces a 401 rather than a 302
					header("X-Requested-With", "XMLHttpRequest").
				when().
					post("/models/testModel" + url).
				then().
					assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED).
					and().
					assertThat().body("message", is("Attempting access without a valid username. Are you logged in?"));
				break;
			case DELETE:
				given().
					// don't log a user in
					//Header forces a 401 rather than a 302
					header("X-Requested-With", "XMLHttpRequest").
				when().
					delete("/models/testModel" + url).
				then().
					assertThat().statusCode(HttpStatus.SC_UNAUTHORIZED).
					and().
					assertThat().body("message", is("Attempting access without a valid username. Are you logged in?"));
				break;

		}
	}
	@Test
	public void testGetResourceUserForbidden() {
		// enable testModel
		switchToSystemModel(0);
		// set the model to be owned by someone else
		testModel.setUserId(keycloakAdminClient.getUserByUsername("testadmin").getId());

		switch(restType){
			case GET:
				given().
					// log in testUser
					filter(userSession).
				when().
					get("/models/testModel" + url).
				then().
					assertThat().statusCode(HttpStatus.SC_FORBIDDEN).
					and().
					assertThat().body("message", is("You do not have permission to access this resource"));
				break;
			case PUT:
				given().
					// log in testUser
					filter(userSession).
				when().
					put("/models/testModel" + url).
				then().
					assertThat().statusCode(HttpStatus.SC_FORBIDDEN).
					and().
					assertThat().body("message", is("You do not have permission to access this resource"));
				break;
			case POST:
				given().
					// log in testUser
					filter(userSession).
				when().
					post("/models/testModel" + url).
				then().
					assertThat().statusCode(HttpStatus.SC_FORBIDDEN).
					and().
					assertThat().body("message", is("You do not have permission to access this resource"));
				break;
			case DELETE:
				given().
					// log in testUser
					filter(userSession).
				when().
					delete("/models/testModel" + url).
				then().
					assertThat().statusCode(HttpStatus.SC_FORBIDDEN).
					and().
					assertThat().body("message", is("You do not have permission to access this resource"));
				break;

		}
	}

	@Test
	public void testGetResourceUserOnAcl() {
		// enable testModel
		switchToSystemModel(1);
		// set the model to be owned by someone else
		testModel.setUserId(keycloakAdminClient.getUserByUsername("testadmin").getId());
		// add the testUser to the ACL
		Set<String> ownerUsernames = new HashSet<>(Arrays.asList("testuser"));
		testModel.setOwnerUsernames(ownerUsernames);

		switch(restType){
			case GET:
				given().
					// log in testUser
					filter(userSession).
				when().
					get("/models/testModel" + url).
				then().
					assertThat().statusCode(HttpStatus.SC_OK);
				break;
			case PUT:
				given().
					// log in testUser
					filter(userSession).
				when().
					put("/models/testModel" + url).
				then().
					assertThat().statusCode(HttpStatus.SC_OK);
				break;
			case POST:
				given().
					// log in testUser
					filter(userSession).
				when().
					post("/models/testModel" + url).
				then().
					assertThat().statusCode(HttpStatus.SC_CREATED);
				break;
			case DELETE:
				given().
					// log in testUser
					filter(userSession).
				when().
					delete("/models/testModel" + url).
				then().
					assertThat().statusCode(HttpStatus.SC_OK);
				break;

		}
	}
}
