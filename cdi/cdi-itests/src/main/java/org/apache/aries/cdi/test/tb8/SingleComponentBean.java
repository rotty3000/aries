package org.apache.aries.cdi.test.tb8;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.SingleComponent;

@SingleComponent
@Service({Pojo.class, SingleComponentBean.class})
public class SingleComponentBean implements Pojo {

	@Override
	public String foo(String fooInput) {
		return getCount() + fooInput + getCount();
	}

	@Override
	public int getCount() {
		return 25;
	}

}
