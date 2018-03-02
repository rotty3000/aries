package org.apache.aries.cdi.container.test.beans;

import javax.inject.Inject;

import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.log.Logger;

// static, reluctant, mandatory, unary
public class Reference_S_R_M_U_Service {
	@Inject
	@Reference
	Foo foo;

	@Inject
	Logger logger;
}
