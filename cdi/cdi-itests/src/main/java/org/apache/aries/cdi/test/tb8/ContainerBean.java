package org.apache.aries.cdi.test.tb8;

import javax.enterprise.context.ApplicationScoped;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.service.cdi.annotations.Service;

@ApplicationScoped
@Service({Pojo.class, ContainerBean.class})
public class ContainerBean implements Pojo {

	@Override
	public String foo(String fooInput) {
		return getCount() + fooInput + getCount();
	}

	@Override
	public int getCount() {
		return 50;
	}

}
