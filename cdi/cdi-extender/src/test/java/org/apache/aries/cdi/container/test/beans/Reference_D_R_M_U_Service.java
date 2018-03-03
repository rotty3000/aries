package org.apache.aries.cdi.container.test.beans;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;

import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.log.Logger;

// dynamic, reluctant, mandatory, unary
public class Reference_D_R_M_U_Service {
	@Inject
	@Reference
	Provider<Foo> foo;

	@Inject
	Logger logger;

	@PostConstruct
	void init() {

	}
}
