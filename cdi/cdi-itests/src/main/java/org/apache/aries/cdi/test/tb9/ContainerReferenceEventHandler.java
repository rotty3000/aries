package org.apache.aries.cdi.test.tb9;

import java.util.TreeMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.reference.ReferenceEvent;

@ApplicationScoped
@Service
public class ContainerReferenceEventHandler implements Pojo {

	void integers(@Observes ReferenceEvent<Integer> event) {
		event.onAddingServiceObjects(
			so -> {
				ServiceReference<Integer> serviceReference = so.getServiceReference();
				System.out.println("=====ADDING==>>> " + serviceReference);

				_services.put(so.getServiceReference(), "ADDED");
			}
		);
		event.onUpdateServiceObjects(
			so -> {
				System.out.println("=====UPDATING==>>> " + so.getServiceReference());

				_services.put(so.getServiceReference(), "UPDATED");
			}
		);
		event.onRemoveServiceReference(
			sr -> {
				System.out.println("=====REMOVING==>>> " + sr);

				_services.remove(sr);
			}
		);
	}

	@Override
	public String foo(String fooInput) {
		return _services.values().toString();
	}

	@Override
	public int getCount() {
		return _services.size();
	}

	private final TreeMap<ServiceReference<Integer>, String> _services = new TreeMap<>();

}
