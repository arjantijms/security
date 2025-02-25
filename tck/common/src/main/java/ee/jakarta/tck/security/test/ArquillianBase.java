/*
 * Copyright (c) 2022, 2025 Contributors to the Eclipse Foundation.
 * Copyright (c) 2015, 2020 Oracle and/or its affiliates. All rights reserved.
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
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package ee.jakarta.tck.security.test;

import static java.util.logging.Level.SEVERE;
import static org.apache.http.HttpStatus.SC_MULTIPLE_CHOICES;
import static org.apache.http.HttpStatus.SC_OK;
import static org.jsoup.Jsoup.parse;
import static org.jsoup.parser.Parser.xmlParser;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import org.htmlunit.DefaultCssErrorHandler;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.Page;
import org.htmlunit.WebClient;
import org.htmlunit.WebResponse;

public class ArquillianBase {

    private static final Logger logger = Logger.getLogger(ArquillianBase.class.getName());

    private WebClient webClient;
    private String response;
    private String responsePath;

	@ArquillianResource
    private URL base;

    @Rule
    public TestWatcher ruleExample = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            super.failed(e, description);

            logger.log(SEVERE,
                "\n\nTest failed: " +
                description.getClassName() + "." + description.getMethodName() +

                "\nMessage: " + e.getMessage() +

                "\nLast response: " +

                "\n\n"  + formatHTML(response) + "\n\n");

        }
    };

    @Before
    public void setUp() {
        Logger logger = Logger.getLogger(DefaultCssErrorHandler.class.getName());
        logger.setLevel(SEVERE);

        response = null;
        webClient = new WebClient() {

            private static final long serialVersionUID = 1L;

            @Override
            public void printContentIfNecessary(WebResponse webResponse) {
                int statusCode = webResponse.getStatusCode();
                if (getOptions().isPrintContentOnFailingStatusCode() && !(statusCode >= SC_OK && statusCode < SC_MULTIPLE_CHOICES)) {
                    logger.log(SEVERE, webResponse.getWebRequest().getUrl().toExternalForm());
                }
                super.printContentIfNecessary(webResponse);
            }
        };
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        if (System.getProperty("glassfish.suspend") != null) {
            webClient.getOptions().setTimeout(0);
        }
    }

    @After
    public void tearDown() {
        webClient.getCookieManager().clearCookies();
        webClient.close();
    }

    protected String readFromServer(String path) {
        response = "";
        WebResponse localResponse = responseFromServer(path);
        if (localResponse != null) {
            response = localResponse.getContentAsString();
        }

    	return response;
    }

    protected WebResponse responseFromServer(String path) {

        WebResponse webResponse = null;

        Page page = pageFromServer(path);
        if (page != null) {
            webResponse = page.getWebResponse();
            if (webResponse != null) {
                response = webResponse.getContentAsString();
            }
        }

        return webResponse;
    }

    protected <P extends Page> P pageFromServer(String path) {
    	if (base.toString().endsWith("/") && path.startsWith("/")) {
    		path = path.substring(1);
    	}

        try {
            response = "";

            P page = webClient.getPage(base + path);

            if (page != null) {
                WebResponse localResponse = page.getWebResponse();
                responsePath = page.getUrl().toString();
                if (localResponse != null) {
                    response = localResponse.getContentAsString();

                    if (System.getProperty("tck.log.response") != null) {
                        printLastResponse();
                    }
                }
            }

            return page;

        } catch (FailingHttpStatusCodeException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void printLastResponse() {
        logger.info(
            "\n\n" +
            "Requested path:\n" + responsePath +
            "\n\n" +

            "Response :\n" + formatHTML(response) +
            "\n\n\n");
    }

    protected void printPage(Page page) {
        if (page != null) {
            WebResponse localResponse = page.getWebResponse();
            responsePath = page.getUrl().toString();
            if (localResponse != null) {
                response = localResponse.getContentAsString();
            }

            printLastResponse();
        }
    }

    protected WebClient getWebClient() {
 		return webClient;
 	}

    public static String formatHTML(String html) {
        try {
            return parse(html, "", xmlParser()).toString();
        } catch (Exception e) {
            return html;
        }
    }

}
