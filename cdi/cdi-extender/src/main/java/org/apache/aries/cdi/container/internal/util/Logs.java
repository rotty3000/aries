package org.apache.aries.cdi.container.internal.util;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerConsumer;
import org.osgi.service.log.LoggerFactory;
import org.osgi.util.tracker.ServiceTracker;

public class Logs {

	public static class Builder {

		public Builder(BundleContext bundleContext) {
			_bundleContext = bundleContext;
		}

		public Logs build() {
			return new Logs(_bundleContext);
		}

		private final BundleContext _bundleContext;

	}

	private Logs(BundleContext bundleContext) {
		LoggerFactory loggerFactory = null;

		if (bundleContext != null) {
			ServiceTracker<LoggerFactory, LoggerFactory> tracker = new ServiceTracker<>(bundleContext, LoggerFactory.class, null);

			tracker.open();

			loggerFactory = tracker.getService();
		}

		_loggerFactory = loggerFactory;
	}

	public Logger getLogger(Class<?> clazz) {
		return getLogger(clazz.getName());
	}

	public Logger getLogger(String name) {
		if (_loggerFactory != null) {
			return _loggerFactory.getLogger(name);
		}

		return new SysoutLogger(name);
	}

	public LoggerFactory getLoggerFactory() {
		return _loggerFactory;
	}

	private final LoggerFactory _loggerFactory;

	private static abstract class BaseLogger implements Logger {

		@Override
		public <E extends Exception> void debug(LoggerConsumer<E> consumer) throws E {
			if (isDebugEnabled())
				consumer.accept(this);
		}

		@Override
		public <E extends Exception> void error(LoggerConsumer<E> consumer) throws E {
			if (isErrorEnabled())
				consumer.accept(this);
		}

		@Override
		public <E extends Exception> void info(LoggerConsumer<E> consumer) throws E {
			if (isInfoEnabled())
				consumer.accept(this);
		}

		@Override
		public <E extends Exception> void trace(LoggerConsumer<E> consumer) throws E {
			if (isTraceEnabled())
				consumer.accept(this);
		}

		@Override
		public <E extends Exception> void warn(LoggerConsumer<E> consumer) throws E {
			if (isWarnEnabled())
				consumer.accept(this);
		}

		@Override
		public void audit(String message) {
		}

		@Override
		public void audit(String format, Object arg) {
		}

		@Override
		public void audit(String format, Object arg1, Object arg2) {
		}

		@Override
		public void audit(String format, Object... arguments) {
		}

	}

	public static class SysoutLogger extends BaseLogger {

		private final String name;

		public SysoutLogger(String name) {
			this.name = name;
		}

		@Override
		public void debug(String message) {
		}

		@Override
		public void debug(String format, Object arg) {
		}

		@Override
		public void debug(String format, Object arg1, Object arg2) {
		}

		@Override
		public void debug(String format, Object... arguments) {
		}

		@Override
		public void error(String message) {
		}

		@Override
		public void error(String format, Object arg) {
		}

		@Override
		public void error(String format, Object arg1, Object arg2) {
		}

		@Override
		public void error(String format, Object... arguments) {
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void info(String message) {
		}

		@Override
		public void info(String format, Object arg) {
		}

		@Override
		public void info(String format, Object arg1, Object arg2) {
		}

		@Override
		public void info(String format, Object... arguments) {
		}

		@Override
		public boolean isDebugEnabled() {
			return true;
		}

		@Override
		public boolean isErrorEnabled() {
			return true;
		}

		@Override
		public boolean isInfoEnabled() {
			return true;
		}

		@Override
		public boolean isTraceEnabled() {
			return true;
		}

		@Override
		public boolean isWarnEnabled() {
			return true;
		}

		@Override
		public void trace(String message) {
		}

		@Override
		public void trace(String format, Object arg) {
		}

		@Override
		public void trace(String format, Object arg1, Object arg2) {
		}

		@Override
		public void trace(String format, Object... arguments) {
		}

		@Override
		public void warn(String message) {
		}

		@Override
		public void warn(String format, Object arg) {
		}

		@Override
		public void warn(String format, Object arg1, Object arg2) {
		}

		@Override
		public void warn(String format, Object... arguments) {
		}

	}

}