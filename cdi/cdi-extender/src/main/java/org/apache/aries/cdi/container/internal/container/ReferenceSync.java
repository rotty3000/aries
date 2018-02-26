package org.apache.aries.cdi.container.internal.container;

import org.apache.aries.cdi.container.internal.model.ExtendedComponentInstanceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceDTO;
import org.apache.aries.cdi.container.internal.util.DTOs;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ReferenceSync implements ServiceTrackerCustomizer<Object, Object> {

	public ReferenceSync(
		ExtendedReferenceDTO referenceDTO,
		ExtendedComponentInstanceDTO componentInstanceDTO) {

		_referenceDTO = referenceDTO;
		_componentInstanceDTO = componentInstanceDTO;
	}

	@Override
	public Object addingService(ServiceReference<Object> reference) {
		_referenceDTO.references.add(reference);
		_referenceDTO.matches = DTOs.from(_referenceDTO.references);
		_componentInstanceDTO.activate(reference);

		return new Object();
	}

	@Override
	public void modifiedService(ServiceReference<Object> reference, Object service) {
	}

	@Override
	public void removedService(ServiceReference<Object> reference, Object service) {
		_referenceDTO.references.remove(reference);
		_referenceDTO.matches = DTOs.from(_referenceDTO.references);
		_componentInstanceDTO.deactivate(reference);
	}

	private final ExtendedComponentInstanceDTO _componentInstanceDTO;
	private final ExtendedReferenceDTO _referenceDTO;

}
