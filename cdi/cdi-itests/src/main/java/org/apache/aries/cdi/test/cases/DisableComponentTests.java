package org.apache.aries.cdi.test.cases;

import static org.junit.Assert.*;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class DisableComponentTests extends AbstractTestCase {

	@Before
	@Override
	public void setUp() throws Exception {
		adminTracker = new ServiceTracker<>(bundleContext, ConfigurationAdmin.class, null);
		adminTracker.open();
		configurationAdmin = adminTracker.getService();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		adminTracker.close();
	}

	@Test
	public void testDisableContainerComponent() throws Exception {
		Bundle tb8Bundle = installBundle("tb8.jar");

		ServiceTracker<Pojo, Pojo> tracker = track(
			"(&(objectClass=%s)(objectClass=*.%s))",
			Pojo.class.getName(),
			"ContainerBean");

		Pojo pojo = tracker.waitForService(timeout);

		assertNotNull(pojo);

		Configuration configurationA = null;

		try {
			configurationA = configurationAdmin.getConfiguration("osgi.cdi.cdi-itests.tb8", "?");

			Dictionary<String, Object> p1 = new Hashtable<>();
			p1.put("containerBean.enabled", false);

			int trackingCount = tracker.getTrackingCount();

			configurationA.update(p1);

			for (int i = 20; (i > 0) && (tracker.getTrackingCount() == trackingCount); i--) {
				Thread.sleep(20);
			}

			pojo = tracker.getService();

			assertNull(pojo);

			p1 = new Hashtable<>();
			p1.put("containerBean.enabled", true);

			trackingCount = tracker.getTrackingCount();

			configurationA.update(p1);

			for (int i = 20; (i > 0) && (tracker.getTrackingCount() == trackingCount); i--) {
				Thread.sleep(20);
			}

			pojo = tracker.getService();

			assertNotNull(pojo);
		}
		finally {
			if (configurationA != null) {
				try {
					configurationA.delete();
				}
				catch (Exception e) {
					// ignore
				}
			}
			tb8Bundle.uninstall();
		}
	}

	@Test
	public void testDisableSingleComponent() throws Exception {
		Bundle tb8Bundle = installBundle("tb8.jar");

		ServiceTracker<Pojo, Pojo> tracker = track(
			"(&(objectClass=%s)(objectClass=*.%s))",
			Pojo.class.getName(),
			"SingleComponentBean");

		Pojo pojo = tracker.waitForService(timeout);

		assertNotNull(pojo);

		Configuration configurationA = null;

		try {
			configurationA = configurationAdmin.getConfiguration("osgi.cdi.cdi-itests.tb8", "?");

			Dictionary<String, Object> p1 = new Hashtable<>();
			p1.put("singleComponentBean.enabled", false);

			int trackingCount = tracker.getTrackingCount();

			configurationA.update(p1);

			for (int i = 20; (i > 0) && (tracker.getTrackingCount() == trackingCount); i--) {
				Thread.sleep(20);
			}

			pojo = tracker.getService();

			assertNull(pojo);

			p1 = new Hashtable<>();
			p1.put("singleComponentBean.enabled", true);

			trackingCount = tracker.getTrackingCount();

			configurationA.update(p1);

			for (int i = 10; (i > 0) && (tracker.getTrackingCount() == trackingCount); i--) {
				Thread.sleep(20);
			}

			pojo = tracker.getService();

			assertNotNull(pojo);
		}
		finally {
			if (configurationA != null) {
				try {
					configurationA.delete();
				}
				catch (Exception e) {
					// ignore
				}
			}
			tb8Bundle.uninstall();
		}
	}

	private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> adminTracker;
	private ConfigurationAdmin configurationAdmin;

}
