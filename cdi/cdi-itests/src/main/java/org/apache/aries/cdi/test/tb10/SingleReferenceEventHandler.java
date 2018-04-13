package org.apache.aries.cdi.test.tb10;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.enterprise.event.Observes;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.SingleComponent;
import org.osgi.service.cdi.reference.ReferenceEvent;

@Service
@SingleComponent
public class SingleReferenceEventHandler implements Pojo {

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

	private final Map<ServiceReference<Integer>, String> _services = new ConcurrentSkipListMap<>();

}
