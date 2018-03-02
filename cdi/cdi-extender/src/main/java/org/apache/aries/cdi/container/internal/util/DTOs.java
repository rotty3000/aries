package org.apache.aries.cdi.container.internal.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;

public class DTOs {

	private DTOs() {
		// no instances
	}

	public static <T> ServiceReferenceDTO from(ServiceReference<T> reference) {
		for (ServiceReferenceDTO dto : reference.getBundle().adapt(ServiceReferenceDTO[].class)) {
			if (dto.id == id(reference)) {
				return dto;
			}
		}
		return null;
	}

	@SafeVarargs
	public static <T> List<ServiceReferenceDTO> from(ServiceReference<T>[] references, ServiceReference<T>... more) {
		if (references == null) return Arrays.stream(more).sorted().map(
			r -> from(r)
		).collect(Collectors.toList());

		return Stream.concat(Arrays.stream(references), Arrays.stream(more)).sorted().map(
			r -> from(r)
		).collect(Collectors.toList());
	}

	@SafeVarargs
	public static <T> List<ServiceReferenceDTO> from(Collection<ServiceReference<T>> references, ServiceReference<T>... more) {
		return Stream.concat(references.stream(), Arrays.stream(more)).sorted().map(
			r -> from(r)
		).collect(Collectors.toList());
	}

	static <T> long id(ServiceReference<T> reference) {
		return (Long)reference.getProperty(Constants.SERVICE_ID);
	}

}
