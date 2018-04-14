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

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.service.cdi.CDIConstants;
import org.osgi.service.cdi.runtime.CDIComponentRuntime;
import org.osgi.util.promise.PromiseFactory;
import org.osgi.util.tracker.ServiceTracker;

@Requirement(
	effective = "active",
	filter = "(objectClass=org.osgi.service.cm.ConfigurationAdmin)",
	namespace = ServiceNamespace.SERVICE_NAMESPACE
)
public class AbstractTestCase {

	@Rule public TestName testName = new TestName();

	@BeforeClass
	public static void beforeClass() throws Exception {
		runtimeTracker = new ServiceTracker<>(
				bundleContext, CDIComponentRuntime.class, null);
		runtimeTracker.open();
		servicesBundle = installBundle("services-one.jar");
		servicesBundle.start();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		runtimeTracker.close();
		servicesBundle.uninstall();
	}

	void testHeader() {
		System.out.println("--------- TEST: " + getClass().getSimpleName() + "#" +testName.getMethodName());
	}

	@Before
	public void setUp() throws Exception {
		testHeader();

		cdiRuntime = runtimeTracker.waitForService(timeout);
		cdiBundle = installBundle("basic-beans.jar");
		cdiBundle.start();
	}

	@After
	public void tearDown() throws Exception {
		cdiBundle.uninstall();
	}

	void assertBeanExists(Class<?> clazz, BeanManager beanManager) {
		Set<Bean<?>> beans = beanManager.getBeans(clazz, Any.Literal.INSTANCE);

		assertFalse(beans.isEmpty());
		Iterator<Bean<?>> iterator = beans.iterator();
		Bean<?> bean = iterator.next();
		assertTrue(clazz.isAssignableFrom(bean.getBeanClass()));
		assertFalse(iterator.hasNext());

		bean = beanManager.resolve(beans);
		CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
		Object pojo = clazz.cast(beanManager.getReference(bean, clazz, ctx));
		assertNotNull(pojo);
	}

	static InputStream getBundle(String name) {
		ClassLoader classLoader = AbstractTestCase.class.getClassLoader();

		return classLoader.getResourceAsStream(name);
	}

	Bundle getCdiExtenderBundle() {
		BundleWiring bundleWiring = cdiBundle.adapt(BundleWiring.class);

		List<BundleWire> requiredWires = bundleWiring.getRequiredWires(ExtenderNamespace.EXTENDER_NAMESPACE);

		for (BundleWire wire : requiredWires) {
			Map<String, Object> attributes = wire.getCapability().getAttributes();
			String extender = (String)attributes.get(ExtenderNamespace.EXTENDER_NAMESPACE);

			if (CDIConstants.CDI_CAPABILITY_NAME.equals(extender)) {
				return wire.getProvider().getBundle();
			}
		}

		return null;
	}

	public static Bundle installBundle(String url) throws Exception {
		return installBundle(url, true);
	}

	public static Bundle installBundle(String bundleName, boolean start) throws Exception {
		Bundle b = bundleContext.installBundle(bundleName, getBundle(bundleName));

		if (start) {
			b.start();
		}

		return b;
	}

	Filter filter(String pattern, Object... objects) {
		try {
			return FrameworkUtil.createFilter(String.format(pattern, objects));
		}
		catch (InvalidSyntaxException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public <S,T> ServiceTracker<S, T> track(Filter filter) {
		ServiceTracker<S, T> tracker = new ServiceTracker<>(bundleContext, filter, null);
		tracker.open();
		return tracker;
	}

	public <S,T> ServiceTracker<S, T> track(String pattern, Object... objects) {
		return track(filter(pattern, objects));
	}

	BeanManager getBeanManager(Bundle bundle) throws Exception {
		return getServiceTracker(bundle).waitForService(timeout);
	}

	ServiceTracker<BeanManager, BeanManager> getServiceTracker(Bundle bundle) throws Exception {
		ServiceTracker<BeanManager, BeanManager> serviceTracker = new ServiceTracker<>(
			bundle.getBundleContext(),
			filter(
				"(&(objectClass=%s)(service.bundleid=%d))",
				BeanManager.class.getName(),
				bundle.getBundleId()),
			null);
		serviceTracker.open();
		return serviceTracker;
	}

	long getChangeCount(ServiceReference<?> reference) {
		return Optional.ofNullable(
			reference.getProperty(Constants.SERVICE_CHANGECOUNT)
		).map(
			v -> (Long)v
		).orElse(
			new Long(-1l)
		).longValue();
	}

	static final Bundle bundle = FrameworkUtil.getBundle(CdiBeanTests.class);
	static final BundleContext bundleContext = bundle.getBundleContext();
	static final long timeout = 5000;
	static Bundle servicesBundle;
	static ServiceTracker<CDIComponentRuntime, CDIComponentRuntime> runtimeTracker;

	Bundle cdiBundle;
	CDIComponentRuntime cdiRuntime;
	final PromiseFactory promiseFactory = new PromiseFactory(null);

}