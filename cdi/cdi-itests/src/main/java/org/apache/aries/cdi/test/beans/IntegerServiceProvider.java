package org.apache.aries.cdi.test.beans;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.apache.aries.cdi.extra.propertytypes.ServiceRanking;
import org.osgi.service.cdi.annotations.Service;

@ApplicationScoped
public class IntegerServiceProvider {

	@Produces
	@Service(Integer.class)
	@ServiceRanking(5000)
	Integer int1() {
		return new Integer(Double.valueOf(Math.random()).intValue());
	}

	@Produces
	@Service(Integer.class)
	@ServiceRanking(12000)
	Integer int2() {
		return new Integer(Double.valueOf(Math.random()).intValue());
	}

	@Produces
	@Service(Integer.class)
	@ServiceRanking(1000)
	Integer int3() {
		return new Integer(Double.valueOf(Math.random()).intValue());
	}

	@Produces
	@Service(Integer.class)
	@ServiceRanking(100000)
	Integer int4 = new Integer(Double.valueOf(Math.random()).intValue());

}
