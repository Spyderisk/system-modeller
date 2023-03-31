
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
//      Created By :            Vadims Krivcovs
//      Created Date :          2016-11-10
//      Created for Project :   5G-Ensure/ASSURED
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.security.systemmodeller.rest;

import static org.junit.Assert.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.servlet.ModelAndView;

import uk.ac.soton.itinnovation.security.systemmodeller.CommonTestSetup;
import uk.ac.soton.itinnovation.security.systemmodeller.SystemModellerApplication;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SystemModellerApplication.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("scratch")
@TestPropertySource(properties = {"reset.on.start=false"})
public class JsonErrorControllerTest extends CommonTestSetup {

    // need to autowire it as application.properties context need to be properly loaded with the class
    @Autowired
    private JsonErrorController errorController;

    @Test
    public void testException() {

        // create model and view that will contain example error details (empty message and one type of error)
        ModelAndView modelAndView1 = errorController.error(new RuntimeException());

        // create model and view that will contain example error details (not empty message and )
        ModelAndView modelAndView2 = errorController.error(new Exception("test message"));

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        JsonErrorController.ErrorResponse errorResponse = errorController.error(servletRequest, servletResponse);

        // test the results
        // make sure that we are dealing with GlobalErrorController
        assertEquals(errorController.getClass(), JsonErrorController.class);

        // make sure that returned objects from exception method is ModelAndView
        assertEquals(ModelAndView.class, modelAndView1.getClass());
        assertEquals(ModelAndView.class, modelAndView2.getClass());
        assertEquals(JsonErrorController.ErrorResponse.class, errorResponse.getClass());
        
        assertEquals("None", errorResponse.error);

        // check ModelAndView1 specific details
        assertEquals(null, modelAndView1.getModel().get("name"));
        assertEquals("Please check your request and try again later", modelAndView1.getModel().get("message"));

        // check ModelAndView2 specific details
        assertEquals("test message", modelAndView2.getModel().get("name"));
        assertEquals("Please check your request and try again later", modelAndView2.getModel().get("message"));
    }
}
