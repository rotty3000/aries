package org.apache.aries.cdi.container.internal.container;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.configurator.BeanConfigurator;

import org.osgi.service.log.FormatterLogger;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;

public class LoggerExtension implements Extension {

	public LoggerExtension(ContainerState containerState) {
		_containerState = containerState;
	}

	void afterBeanDiscovery(@Observes AfterBeanDiscovery abd) {
		LoggerFactory lf = _containerState.loggerTracker().getService();

		BeanConfigurator<FormatterLogger> formatterLoggerBean = abd.addBean();
		formatterLoggerBean.addType(FormatterLogger.class);
		formatterLoggerBean.produceWith(
			i -> {
				InjectionPoint ip = i.select(InjectionPoint.class).get();
				return lf.getLogger(
					_containerState.bundle(),
					ip.getMember().getDeclaringClass().getName(),
					FormatterLogger.class);
			}
		);

		BeanConfigurator<Logger> loggerBean = abd.addBean();
		loggerBean.addType(Logger.class);
		loggerBean.produceWith(
			i -> {
				InjectionPoint ip = i.select(InjectionPoint.class).get();
				return lf.getLogger(
					_containerState.bundle(),
					ip.getMember().getDeclaringClass().getName(),
					Logger.class);
			}
		);
	}

	private final ContainerState _containerState;

}
