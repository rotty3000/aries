package org.apache.aries.cdi.container.internal.model;

import org.osgi.service.cdi.runtime.dto.ReferenceDTO;
import org.osgi.util.tracker.ServiceTracker;

public class ExtendedReferenceDTO extends ReferenceDTO {

	public ServiceTracker<Object, Object> serviceTracker;

}
