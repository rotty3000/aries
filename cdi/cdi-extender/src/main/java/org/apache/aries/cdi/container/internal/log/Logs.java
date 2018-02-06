package org.apache.aries.cdi.container.internal.log;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerConsumer;
import org.osgi.service.log.LoggerFactory;
import org.osgi.util.tracker.ServiceTracker;

public class Logs {

	public static Logger getLogger(Class<?> clazz) {
		if (INSTANCE._tracker != null) {
			LoggerFactory service = INSTANCE._tracker.getService();

			if (service != null) {
				return service.getLogger(clazz);
			}
		}

		return INSTANCE._fallbackLogger;
	}

	private Logs() {
		Bundle bundle = FrameworkUtil.getBundle(getClass());

		if (bundle != null) {
			_tracker = new ServiceTracker<>(bundle.getBundleContext(), LoggerFactory.class, null);

			_tracker.open();
		}
		else {
			_tracker = null;
		}

		Logger fallbackLogger = new NoopLogger();

		try {
			fallbackLogger = new Sfl4jLogger(getClass());
		}
		catch (Exception e) {
		}

		_fallbackLogger = fallbackLogger;
	}

	private static final Logs INSTANCE = new Logs();

	private final Logger _fallbackLogger;
	private volatile ServiceTracker<LoggerFactory, LoggerFactory> _tracker;

	private class NoopLogger implements Logger {

		@Override
		public String getName() {
			return null;
		}

		@Override
		public boolean isTraceEnabled() {
			return false;
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
		public <E extends Exception> void trace(LoggerConsumer<E> consumer) throws E {
		}

		@Override
		public boolean isDebugEnabled() {
			return false;
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
		public <E extends Exception> void debug(LoggerConsumer<E> consumer) throws E {
		}

		@Override
		public boolean isInfoEnabled() {
			return false;
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
		public <E extends Exception> void info(LoggerConsumer<E> consumer) throws E {
		}

		@Override
		public boolean isWarnEnabled() {
			return false;
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

		@Override
		public <E extends Exception> void warn(LoggerConsumer<E> consumer) throws E {
		}

		@Override
		public boolean isErrorEnabled() {
			return false;
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
		public <E extends Exception> void error(LoggerConsumer<E> consumer) throws E {
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

	};

	private static class Sfl4jLogger implements Logger {

		private final org.slf4j.Logger _logger;

		public Sfl4jLogger(Class<?> clazz) throws Exception {
			_logger = org.slf4j.LoggerFactory.getLogger(clazz);
		}

		@Override
		public String getName() {
			return _logger.getName();
		}

		@Override
		public boolean isTraceEnabled() {
			return _logger.isTraceEnabled();
		}

		@Override
		public void trace(String message) {
			_logger.trace(message);
		}

		@Override
		public void trace(String format, Object arg) {
			_logger.trace(format, arg);
		}

		@Override
		public void trace(String format, Object arg1, Object arg2) {
			_logger.trace(format, arg1, arg2);
		}

		@Override
		public void trace(String format, Object... arguments) {
			_logger.trace(format, arguments);
		}

		@Override
		public boolean isDebugEnabled() {
			return _logger.isDebugEnabled();
		}

		@Override
		public void debug(String msg) {
			_logger.debug(msg);
		}

		@Override
		public void debug(String format, Object arg) {
			_logger.debug(format, arg);
		}

		@Override
		public void debug(String format, Object arg1, Object arg2) {
			_logger.debug(format, arg1, arg2);
		}

		@Override
		public void debug(String format, Object... arguments) {
			_logger.debug(format, arguments);
		}

		@Override
		public boolean isInfoEnabled() {
			return _logger.isInfoEnabled();
		}

		@Override
		public void info(String msg) {
			_logger.info(msg);
		}

		@Override
		public void info(String format, Object arg) {
			_logger.info(format, arg);
		}

		@Override
		public void info(String format, Object arg1, Object arg2) {
			_logger.info(format, arg1, arg2);
		}

		@Override
		public void info(String format, Object... arguments) {
			_logger.info(format, arguments);
		}

		@Override
		public boolean isWarnEnabled() {
			return _logger.isWarnEnabled();
		}

		@Override
		public void warn(String msg) {
			_logger.warn(msg);
		}

		@Override
		public void warn(String format, Object arg) {
			_logger.warn(format, arg);
		}

		@Override
		public void warn(String format, Object... arguments) {
			_logger.warn(format, arguments);
		}

		@Override
		public void warn(String format, Object arg1, Object arg2) {
			_logger.warn(format, arg1, arg2);
		}

		@Override
		public boolean isErrorEnabled() {
			return _logger.isErrorEnabled();
		}

		@Override
		public void error(String msg) {
			_logger.error(msg);
		}

		@Override
		public void error(String format, Object arg) {
			_logger.error(format, arg);
		}

		@Override
		public void error(String format, Object arg1, Object arg2) {
			_logger.error(format, arg1, arg2);
		}

		@Override
		public void error(String format, Object... arguments) {
			_logger.error(format, arguments);
		}

		@Override
		public <E extends Exception> void trace(LoggerConsumer<E> consumer) throws E {
			if (_logger.isTraceEnabled())
				consumer.accept(this);
		}

		@Override
		public <E extends Exception> void debug(LoggerConsumer<E> consumer) throws E {
			if (_logger.isDebugEnabled())
				consumer.accept(this);
		}

		@Override
		public <E extends Exception> void info(LoggerConsumer<E> consumer) throws E {
			if (_logger.isInfoEnabled())
				consumer.accept(this);
		}

		@Override
		public <E extends Exception> void warn(LoggerConsumer<E> consumer) throws E {
			if (_logger.isWarnEnabled())
				consumer.accept(this);
		}

		@Override
		public <E extends Exception> void error(LoggerConsumer<E> consumer) throws E {
			if (_logger.isErrorEnabled())
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

}