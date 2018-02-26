package org.apache.aries.cdi.container.internal.model;

import java.util.SortedSet;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.runtime.dto.ReferenceDTO;
import org.osgi.util.tracker.ServiceTracker;

public class ExtendedReferenceDTO extends ReferenceDTO {

	public ServiceTracker<?, ?> serviceTracker;

	public SortedSet<ServiceReference<?>> references;

}
