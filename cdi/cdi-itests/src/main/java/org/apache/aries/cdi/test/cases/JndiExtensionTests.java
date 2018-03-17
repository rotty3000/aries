/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.test.cases;

import java.util.Hashtable;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.naming.InitialContext;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.service.jndi.JNDIConstants;
import org.osgi.util.tracker.ServiceTracker;

public class JndiExtensionTests extends AbstractTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testGetBeanManagerThroughJNDI() throws Exception {
		Hashtable<String, Object> env = new Hashtable<>();
		env.put(JNDIConstants.BUNDLE_CONTEXT, cdiBundle.getBundleContext());
		InitialContext context = new InitialContext(env);

		BeanManager beanManager = (BeanManager)context.lookup("java:comp/BeanManager");

		assertNotNull(beanManager);
		assertBeanExists(Pojo.class, beanManager);
	}

	public void testDisableExtensionAndCDIContainerWaits() throws Exception {
		Filter filter = filter(
			"(&(objectClass=%s)(osgi.cdi.extension=aries.cdi.jndi))",
			Extension.class.getName());
		ServiceTracker<Extension, Extension> et = new ServiceTracker<>(
			bundleContext, filter, null);

		et.open();

		assertFalse(et.isEmpty());

		Bundle extensionBundle = et.getServiceReference().getBundle();

		// TODO Check that everything is ok...

		extensionBundle.stop();

		// TODO check that CDI bundles dependent on the extension are not not OK

		extensionBundle.start();

		// TODO check that they are ok again!
	}

}