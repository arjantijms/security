/*
 * Copyright (c) 2021, 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * Contributors:
 *   2021 : Payara Foundation and/or its affiliates
 */
package ee.jakarta.tck.security.test;

import static jakarta.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import org.htmlunit.TextPage;
import org.htmlunit.WebClient;

import ee.jakarta.tck.security.test.client.CallbackServlet;
import ee.jakarta.tck.security.test.client.UnsecuredServlet;
import ee.jakarta.tck.security.test.client.UserNameServlet;
import ee.jakarta.tck.security.test.server.ApplicationConfig;
import ee.jakarta.tck.security.test.server.OidcProvider;

/**
 * @author Gaurav Gupta
 * @author Jonathan
 * @author Rudy De Busscher
 */
public class OpenIdTestUtil {

    public static WebArchive createServerDeployment() {
        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "openid-server.war")
                .addClass(OidcProvider.class)
                .addClass(ApplicationConfig.class)
                .addAsResource("openid-configuration.json")
                .addAsResource("jsonwebkeys.json")
                .addAsWebInfResource("beans.xml")
                .addAsLibraries(nimbus())
                ;

        if (Boolean.parseBoolean(System.getProperty("oidcProviderUsesHttps"))) {
            String httpsPort = System.getProperty("oidcProviderHttpsPort");
            System.out.println("Profile requested using HTTPS endpoints with port " + httpsPort + " for the server deployment.");

            if (httpsPort != null && !httpsPort.isEmpty()) {
                String content = "oidcProviderHttpsPort=" + httpsPort;
                war.add(new StringAsset(content), "/oidcProviderHttpsPort.properties");
            }
        }
        return war;
    }

    public static JavaArchive[] nimbus() {
        return Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("com.nimbusds:nimbus-jose-jwt")
                .withTransitivity()
                .as(JavaArchive.class);
    }

    public static WebArchive createClientDeployment(Class<?>... additionalClasses) {
        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "openid-client.war")
                .addClass(CallbackServlet.class)
                .addClass(UnsecuredServlet.class)
                .addClass(UserNameServlet.class)
                .addClasses(additionalClasses)
                .addAsWebInfResource("beans.xml");

        return war;
    }

    public static void testOpenIdConnect(WebClient webClient, URL base) throws IOException {
        // Unsecure page should be accessible for an unauthenticated user
        TextPage unsecuredPage = webClient.getPage(base + "Unsecured");
        assertEquals(OK.getStatusCode(), unsecuredPage.getWebResponse().getStatusCode());
        assertEquals("This is an unsecured web page", unsecuredPage.getContent().trim());

        // Access to secured web page authenticates the user and instructs to redirect to the callback URL
        TextPage securedPage = webClient.getPage(base + "Secured");
        assertEquals(OK.getStatusCode(), securedPage.getWebResponse().getStatusCode());
        assertEquals(String.format("%sCallback", base.getPath()), securedPage.getUrl().getPath());

        // Access secured web page as an authenticated user
        securedPage = webClient.getPage(base + "Secured");
        assertEquals(OK.getStatusCode(), securedPage.getWebResponse().getStatusCode());
        assertEquals("This is a secured web page", securedPage.getContent().trim());

        //Finally, access should still be allowed to an unsecured web page when already logged in
        unsecuredPage = webClient.getPage(base + "Unsecured");
        assertEquals(OK.getStatusCode(), unsecuredPage.getWebResponse().getStatusCode());
        assertEquals("This is an unsecured web page", unsecuredPage.getContent().trim());
    }

}
