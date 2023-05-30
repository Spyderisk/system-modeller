/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2016
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
//      Created By :            Gianluca Correndo
//      Created Date :          27 Jul 2016
//      Modified by:            Gianluca Correndo
//      Created for Project :   5G-Ensure
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller;

import java.util.Map;
import io.restassured.RestAssured;
import io.restassured.config.RedirectConfig;
import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

import uk.ac.soton.itinnovation.security.systemmodeller.semantics.ModelObjectsHelper;
import uk.ac.soton.itinnovation.security.systemmodeller.semantics.StoreModelManager;

/**
 * Represents common setup for the REST controller tests.
 */
@TestPropertySource(properties = {"spring.data.mongodb.database=${test.spring.data.mongodb.database}",
                                    "reset.on.start=false",
                                    "knowledgebases.install.folder=/opt/spyderisk/knowledgebases-test",
                                    "check.installed.knowledgebases=false"
})
public abstract class CommonTestSetup {

    protected final Logger logger = LoggerFactory.getLogger(CommonTestSetup.class);

    //System objects to control model access
    @Autowired
    private StoreModelManager storeModelManager;

    @Autowired
    private ModelObjectsHelper modelObjectsHelper;

    //Test user details
    public String testUserName = "testuser";
    public String testUserPassword = "password";

    public String testAdminName = "testadmin";
    public String testAdminPassword = "password";

    //Logged in sessions
    protected SessionFilter userSession;
    protected SessionFilter adminSession;

    @Before
    public void setUp() throws Exception {
        storeModelManager.clearMgtGraph();
        modelObjectsHelper.syncUsers();
    }

    public void initAuth(String contextPath, int port) {
        RestAssured.baseURI = "http://localhost";
        RestAssured.basePath = contextPath;
        RestAssured.port = port;

        userSession = createSession(testUserName, testUserPassword);
        adminSession = createSession(testAdminName, testAdminPassword);
    }

    private SessionFilter createSession(String userName, String password) {
        SessionFilter session = new SessionFilter();

        String baseUri = RestAssured.baseURI;
        String basePath = RestAssured.basePath;

        // Needed to prevent double encoding of redirect URIs
        RestAssured.urlEncodingEnabled = false;

        // Need to manually follow redirects as we need to store the cookies.
        // This is only really need for the last step where we store the JSESSIONID in the SessionFilter.
        RestAssured.config = RestAssured.config().redirect(RedirectConfig.redirectConfig().followRedirects(false));

        try {
            // Try to login to the SSM.
            Response ssmFirstResponse =
                given().
                when().
                    post("sso/login").
                then().
                    assertThat().statusCode(HttpStatus.SC_MOVED_TEMPORARILY).
                    and().
                    extract().response();

            String keycloakRedirectUri = ssmFirstResponse.getHeader("Location");
            Map<String, String> ssmCookies = ssmFirstResponse.getCookies();

            // Need to clear these otherwise they get prepended to the redirect URIs.
            RestAssured.baseURI = "";
            RestAssured.basePath = "";

            // Follow the redirect to Keycloak.
            Response keycloakFirstResponse =
                given().
                when().
                    get(keycloakRedirectUri).
                then().
                    assertThat().statusCode(HttpStatus.SC_OK).
                    and().
                    extract().response();

            String loginPageContent = keycloakFirstResponse.getBody().asString();
            String loginFormAction = ((loginPageContent.split("action=\""))[1].split("\""))[0].replace("amp;", "");
            Map<String, String> keycloakCookies = keycloakFirstResponse.getCookies();

            // Try to login at Keycloak.
            Response loginResponse =
                given().
                    param("username", userName).
                    param("password", password).
                    cookies(keycloakCookies).
                when().
                    post(loginFormAction).
                then().
                    assertThat().statusCode(HttpStatus.SC_MOVED_TEMPORARILY).
                    and().
                    extract().response();

            String ssmRedirectUri = loginResponse.getHeader("Location");

            // Follow the redirect back to the SSM.
            // The redirect URI contains a code the SSM can exchange for a token.
            // We ignore the redirect to the SSM main landing page.
            // Instead we store the JSESSIONID in the SessionFilter.
            given().
                filter(session).
                cookies(ssmCookies).
            when().
                get(ssmRedirectUri).
            then().
                assertThat().statusCode(HttpStatus.SC_MOVED_TEMPORARILY);

            // We should be good to go now.
            assertTrue(session.hasSessionId());

            logger.debug("Session ID for {}: {}", userName, session.getSessionId());

        } catch (Error e) {
            String message = "No Session ID for " + userName + ": login via Keycloak failed";
            logger.error(message);
            throw new Error(message, e);
        }

        // Restore previous settings.
        RestAssured.baseURI = baseUri;
        RestAssured.basePath = basePath;
        RestAssured.urlEncodingEnabled = true;

        return session;
    }

    @After
    public void close() {
        SecurityContextHolder.clearContext();
    }

}
