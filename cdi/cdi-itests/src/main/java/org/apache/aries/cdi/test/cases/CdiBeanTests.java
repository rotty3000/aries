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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.apache.aries.cdi.test.interfaces.BundleContextBeanQualifier;
import org.apache.aries.cdi.test.interfaces.FieldInjectedReference;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.ComponentType;
import org.osgi.service.cdi.runtime.CDIComponentRuntime;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.util.tracker.ServiceTracker;

@SuppressWarnings("rawtypes")
public class CdiBeanTests extends AbstractTestCase {

	@Test
	public void testConstructorInjectedService() throws Exception {
		CDIComponentRuntime runtime = runtimeTracker.waitForService(timeout);
		assertNotNull(runtime);

		ServiceTracker<BeanService, BeanService> tracker = track(
			"(&(objectClass=%s)(objectClass=*.%s))",
			BeanService.class.getName(),
			"ConstructorInjectedService");

		BeanService beanService = tracker.waitForService(timeout);

		assertNotNull(beanService);
		assertEquals("PREFIXCONSTRUCTOR", beanService.doSomething());
	}

	@Test
	public void testFieldInjectedReference_BundleScoped() throws Exception {
		CDIComponentRuntime runtime = runtimeTracker.waitForService(timeout);
		assertNotNull(runtime);

		ServiceTracker<FieldInjectedReference, FieldInjectedReference> tracker = track(
			"(&(objectClass=%s)(objectClass=*.%s))",
			FieldInjectedReference.class.getName(),
			"FieldInjectedBundleScopedImpl");

		FieldInjectedReference fieldInjectedReference = tracker.waitForService(timeout);

		assertNotNull(fieldInjectedReference);
		assertNotNull(fieldInjectedReference.getProperties());
		assertNotNull(fieldInjectedReference.getGenericReference());
		assertNotNull(fieldInjectedReference.getRawReference());
		assertNotNull(fieldInjectedReference.getService());
		assertEquals("value", fieldInjectedReference.getProperties().get("key"));
		assertEquals("value", fieldInjectedReference.getGenericReference().getProperty("key"));
		assertEquals("value", fieldInjectedReference.getRawReference().getProperty("key"));
	}

	@Test
	public void testFieldInjectedReference_PrototypeScoped() throws Exception {
		CDIComponentRuntime runtime = runtimeTracker.waitForService(timeout);
		assertNotNull(runtime);

		ServiceTracker<FieldInjectedReference, FieldInjectedReference> tracker = track(
			"(&(objectClass=%s)(objectClass=*.%s))",
			FieldInjectedReference.class.getName(),
			"FieldInjectedPrototypeScopedImpl");

		FieldInjectedReference fieldInjectedReference = tracker.waitForService(timeout);

		assertNotNull(fieldInjectedReference);
		assertNotNull(fieldInjectedReference.getProperties());
		assertNotNull(fieldInjectedReference.getGenericReference());
		assertNotNull(fieldInjectedReference.getRawReference());
		assertNotNull(fieldInjectedReference.getService());
		assertEquals("value", fieldInjectedReference.getProperties().get("key"));
		assertEquals("value", fieldInjectedReference.getGenericReference().getProperty("key"));
		assertEquals("value", fieldInjectedReference.getRawReference().getProperty("key"));
	}

	@Test
	public void testFieldInjectedService() throws Exception {
		CDIComponentRuntime runtime = runtimeTracker.waitForService(timeout);
		assertNotNull(runtime);

		ServiceTracker<BeanService, BeanService> tracker = track(
			"(&(objectClass=%s)(objectClass=*.%s))",
			BeanService.class.getName(),
			"FieldInjectedService");

		BeanService beanService = tracker.waitForService(timeout);

		assertNotNull(beanService);
		assertEquals("PREFIXFIELD", beanService.doSomething());
	}

	@Test
	public void testMethodInjectedService() throws Exception {
		CDIComponentRuntime runtime = runtimeTracker.waitForService(timeout);
		assertNotNull(runtime);

		ServiceTracker<BeanService, BeanService> tracker = track(
			"(&(objectClass=%s)(objectClass=*.%s))",
			BeanService.class.getName(),
			"MethodInjectedService");

		BeanService beanService = tracker.waitForService(timeout);

		assertNotNull(beanService);
		assertEquals("PREFIXMETHOD", beanService.doSomething());

		ContainerDTO containerDTO = runtime.getContainerDTO(cdiBundle);
		assertNotNull(containerDTO);

		ComponentDTO containerComponentDTO = containerDTO.components.stream().filter(
			c -> c.template.type == ComponentType.CONTAINER).findFirst().orElse(null);
		assertNotNull(containerComponentDTO);
		assertEquals(7, containerComponentDTO.template.beans.size());

		// There's only one instance of the Container component
		ComponentInstanceDTO componentInstanceDTO = containerComponentDTO.instances.get(0);
		assertNotNull(componentInstanceDTO);

		assertEquals(0, componentInstanceDTO.configurations.size());
		assertNotNull("should have properties", componentInstanceDTO.properties);
	}

	@Test
	public void testBeanAsServiceWithProperties() throws Exception {
		CDIComponentRuntime runtime = runtimeTracker.waitForService(timeout);
		assertNotNull(runtime);

		ServiceTracker<BeanService, BeanService> tracker = track(
			"(&(objectClass=%s)(objectClass=*.%s))",
			BeanService.class.getName(),
			"ServiceWithProperties");

		BeanService beanService = tracker.waitForService(timeout);

		assertNotNull(beanService);

		ServiceReference<BeanService> serviceReference = tracker.getServiceReference();

		assertEquals("test.value.b2", serviceReference.getProperty("test.key.b2"));

		assertTrue(serviceReference.getProperty("p.Boolean") instanceof Boolean);
		assertTrue(serviceReference.getProperty("p.Boolean.array") instanceof boolean[]);
		assertEquals(2, ((boolean[])serviceReference.getProperty("p.Boolean.array")).length);

		assertTrue(serviceReference.getProperty("p.Byte") instanceof Byte);
		assertTrue(serviceReference.getProperty("p.Byte.array") instanceof byte[]);
		assertEquals(2, ((byte[])serviceReference.getProperty("p.Byte.array")).length);

		assertTrue(serviceReference.getProperty("p.Character") instanceof Character);
		assertTrue(serviceReference.getProperty("p.Character.array") instanceof char[]);
		assertEquals(2, ((char[])serviceReference.getProperty("p.Character.array")).length);

		assertTrue(serviceReference.getProperty("p.Double") instanceof Double);
		assertTrue(serviceReference.getProperty("p.Double.array") instanceof double[]);
		assertEquals(2, ((double[])serviceReference.getProperty("p.Double.array")).length);

		assertTrue(serviceReference.getProperty("p.Float") instanceof Float);
		assertTrue(serviceReference.getProperty("p.Float.array") instanceof float[]);
		assertEquals(2, ((float[])serviceReference.getProperty("p.Float.array")).length);

		assertTrue(serviceReference.getProperty("p.Integer") instanceof Integer);
		assertTrue(serviceReference.getProperty("p.Integer.array") instanceof int[]);
		assertEquals(2, ((int[])serviceReference.getProperty("p.Integer.array")).length);

		assertTrue(serviceReference.getProperty("p.Long") instanceof Long);
		assertTrue(serviceReference.getProperty("p.Long.array") instanceof long[]);
		assertEquals(2, ((long[])serviceReference.getProperty("p.Long.array")).length);

		assertTrue(serviceReference.getProperty("p.Short") instanceof Short);
		assertTrue(serviceReference.getProperty("p.Short.array") instanceof short[]);
		assertEquals(2, ((short[])serviceReference.getProperty("p.Short.array")).length);

		assertTrue(serviceReference.getProperty("p.String") instanceof String);
		assertTrue(serviceReference.getProperty("p.String.array") instanceof String[]);
		assertEquals(2, ((String[])serviceReference.getProperty("p.String.array")).length);

		// glubInteger = 45, gooString = "green"
		assertTrue(serviceReference.getProperty("glub.integer") instanceof Integer);
		assertEquals(45, ((Integer)serviceReference.getProperty("glub.integer")).intValue());
		assertTrue(serviceReference.getProperty("goo.string") instanceof String);
		assertEquals("green", (serviceReference.getProperty("goo.string")));
	}

	@Test
	public void testBundleContextInjection() throws Exception {
		BeanManager beanManager = getBeanManager(cdiBundle);

		assertNotNull(beanManager);

		@SuppressWarnings("serial")
		Set<Bean<?>> beans = beanManager.getBeans(Object.class, new AnnotationLiteral<BundleContextBeanQualifier>() {});
		Bean<?> bean = beanManager.resolve(beans);
		CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
		Object bcb = beanManager.getReference(bean, Object.class, ctx);
		assertNotNull(bcb);
		@SuppressWarnings("unchecked")
		BeanService<BundleContext> bti = (BeanService<BundleContext>)bcb;
		assertNotNull(bti.get());
		assertTrue(bti.get() instanceof BundleContext);
	}

	@Ignore // This test doesn't make sense because there's only a single bean for the reference!!!
	public void _testInstanceProperties() throws Exception {
		Iterator<ServiceReference<BeanService>> iterator = bundleContext.getServiceReferences(
			BeanService.class, String.format("(objectClass=*.%s)","Instance_ServiceProperties")).iterator();

		assertTrue(iterator.hasNext());

		ServiceReference<BeanService> serviceReference = iterator.next();

		assertNotNull(serviceReference);

		@SuppressWarnings("unchecked")
		BeanService<Map<String, Object>> bean = bundleContext.getService(serviceReference);

		assertNotNull(bean);
		assertEquals(3, Integer.decode(bean.doSomething()).intValue());
		Map<String, Object> map = bean.get();
		assertNotNull(map);
	}

	@Ignore // This test doesn't make sense because there's only a single bean for the reference!!!
	public void _testInstanceServiceReference() throws Exception {
		Iterator<ServiceReference<BeanService>> iterator = bundleContext.getServiceReferences(
			BeanService.class, String.format("(objectClass=*.%s)","Instance_ServiceReference")).iterator();

		assertTrue(iterator.hasNext());

		ServiceReference<BeanService> serviceReference = iterator.next();

		assertNotNull(serviceReference);

		@SuppressWarnings("unchecked")
		BeanService<ServiceReference<?>> bean = bundleContext.getService(serviceReference);

		assertNotNull(bean);
		assertEquals(3, Integer.decode(bean.doSomething()).intValue());
		ServiceReference<?> sr = bean.get();
		assertNotNull(sr);
	}

	@Ignore // This test doesn't make sense because there's only a single bean for the reference!!!
	public void _testInstance_Optional() throws Exception {
		Iterator<ServiceReference<BeanService>> iterator = bundleContext.getServiceReferences(
			BeanService.class, String.format("(objectClass=*.%s)","Instance_Optional")).iterator();

		assertTrue(iterator.hasNext());

		ServiceReference<BeanService> serviceReference = iterator.next();

		assertNotNull(serviceReference);

		@SuppressWarnings("unchecked")
		BeanService<Callable<String>> bean = bundleContext.getService(serviceReference);

		assertNotNull(bean);
		assertEquals(0, Integer.decode(bean.doSomething()).intValue());
		assertNull(bean.get());
	}

}