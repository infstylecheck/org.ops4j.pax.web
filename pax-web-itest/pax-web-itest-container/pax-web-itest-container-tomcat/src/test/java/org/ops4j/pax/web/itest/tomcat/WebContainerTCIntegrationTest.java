/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.itest.tomcat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import javax.servlet.ServletContext;

import static org.junit.Assert.fail;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WebContainerTCIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Configuration
	public Option[] configure() {
		return configureTomcat();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		initWebListener();
		final String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-wc/"
				+ VersionUtil.getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
		waitForWebListener();
	}

	@After
	public void tearDown() throws BundleException {
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
	}


	@Test
	public void testWebContextPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8282/helloworld/wc");
	}

	@Test
	public void testWebContextPathWithServlet() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<title>Hello World (servlet name)</title>'",
						resp -> resp.contains("<title>Hello World (servlet name)</title>"))
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8282/helloworld/wc/sn");
	}

	/**
	 * The server-container must register each ServletContext as an OSGi service
	 */
	@Test
	public void testServletContextRegistration() throws Exception {


		String filter = String.format("(%s=%s)",
				WebContainerConstants.PROPERTY_SERVLETCONTEXT_PATH, "/");

		if(bundleContext.getServiceReferences(ServletContext.class, filter).size() == 0){
			fail("ServletContext was not registered as Service.");
		}
	}

	/**
	 * The server-container must unregister a ServletContext if the ServletContext got destroyed
	 */
	@Test
	public void testServletContextUnregistration() throws Exception {
		installWarBundle.stop();
		String filter = String.format("(%s=%s)",
				WebContainerConstants.PROPERTY_SERVLETCONTEXT_PATH, "/");

		if(bundleContext.getServiceReferences(ServletContext.class, filter).size() > 0){
			fail("ServletContext was not unregistered.");
		}
	}

}
