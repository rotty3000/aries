package org.apache.aries.cdi.container.internal.util;

import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;

public class DTOs {

	private DTOs() {
		// no instances
	}

	public static ServiceReferenceDTO from(ServiceReference<?> reference) {
		for (ServiceReferenceDTO dto : reference.getBundle().adapt(ServiceReferenceDTO[].class)) {
			if (dto.id == id(reference)) {
				return dto;
			}
		}
		return null;
	}

	public static List<ServiceReferenceDTO> from(SortedSet<ServiceReference<?>> references) {
		return references.stream().map(r -> from(r)).collect(Collectors.toList());
	}

	static long id(ServiceReference<?> reference) {
		return (Long)reference.getProperty(Constants.SERVICE_ID);
	}

}
