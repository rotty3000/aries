package org.apache.aries.cdi.container.internal.util;

import java.util.Observable;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerConsumer;
import org.osgi.service.log.LoggerFactory;
import org.osgi.util.tracker.ServiceTracker;

public class Logs {

	public static Logger getLogger(Class<?> clazz) {
/*		if (INSTANCE._tracker != null) {
			LoggerFactory service = INSTANCE._tracker.getService();

			if (service != null) {
				return service.getLogger(clazz);
			}
		}
*/
		Logger logger = new NoopLogger(clazz);

		try {
			logger = new Sfl4jLogger(clazz);
		}
		catch (Exception e) {
			logger.error(l -> l.error("Couldn't load slf4j classes", e));
		}

		return logger;
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
	}

	private static final Logs INSTANCE = new Logs();

	private volatile ServiceTracker<LoggerFactory, LoggerFactory> _tracker;

	private static abstract class BaseLogger extends Observable implements Logger {

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

	private static class NoopLogger extends BaseLogger {

		private final String name;

		public NoopLogger(Class<?> clazz) {
			this.name = clazz.getName();
		}

		@Override
		public void debug(String message) {
			setChanged();
			notifyObservers(new Object[] {LogLevel.DEBUG, message});
		}

		@Override
		public void debug(String format, Object arg) {
			setChanged();
			notifyObservers(new Object[] {LogLevel.DEBUG, format, arg});
		}

		@Override
		public void debug(String format, Object arg1, Object arg2) {
			setChanged();
			notifyObservers(new Object[] {LogLevel.DEBUG, format, arg1, arg2});
		}

		@Override
		public void debug(String format, Object... arguments) {
			setChanged();
			notifyObservers(new Object[] {LogLevel.DEBUG, format, arguments});
		}

		@Override
		public void error(String message) {
			setChanged();
			notifyObservers(new Object[] {LogLevel.ERROR, message});
		}

		@Override
		public void error(String format, Object arg) {
			setChanged();
			notifyObservers(new Object[] {LogLevel.ERROR, format, arg});
		}

		@Override
		public void error(String format, Object arg1, Object arg2) {
			setChanged();
			notifyObservers(new Object[] {LogLevel.ERROR, format, arg1, arg2});
		}

		@Override
		public void error(String format, Object... arguments) {
			setChanged();
			notifyObservers(new Object[] {LogLevel.ERROR, format, arguments});
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void info(String message) {
			setChanged();
			notifyObservers(new Object[] {LogLevel.INFO, message});
		}

		@Override
		public void info(String format, Object arg) {
			setChanged();
			notifyObservers(new Object[] {LogLevel.INFO, format, arg});
		}

		@Override
		public void info(String format, Object arg1, Object arg2) {
			setChanged();
			notifyObservers(new Object[] {LogLevel.INFO, format, arg1, arg2});
		}

		@Override
		public void info(String format, Object... arguments) {
			setChanged();
			notifyObservers(new Object[] {LogLevel.INFO, format, arguments});
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
			setChanged();
			notifyObservers(new Object[] {LogLevel.TRACE, message});
		}

		@Override
		public void trace(String format, Object arg) {
			setChanged();
			notifyObservers(new Object[] {LogLevel.TRACE, format, arg});
		}

		@Override
		public void trace(String format, Object arg1, Object arg2) {
			setChanged();
			notifyObservers(new Object[] {LogLevel.TRACE, format, arg1, arg2});
		}

		@Override
		public void trace(String format, Object... arguments) {
			setChanged();
			notifyObservers(new Object[] {LogLevel.TRACE, format, arguments});
		}

		@Override
		public void warn(String message) {
			setChanged();
			notifyObservers(new Object[] {LogLevel.WARN, message});
		}

		@Override
		public void warn(String format, Object arg) {
			setChanged();
			notifyObservers(new Object[] {LogLevel.WARN, format, arg});
		}

		@Override
		public void warn(String format, Object arg1, Object arg2) {
			setChanged();
			notifyObservers(new Object[] {LogLevel.WARN, format, arg1, arg2});
		}

		@Override
		public void warn(String format, Object... arguments) {
			setChanged();
			notifyObservers(new Object[] {LogLevel.WARN, format, arguments});
		}

	};

	private static class Sfl4jLogger extends NoopLogger {

		private final org.slf4j.Logger _logger;

		public Sfl4jLogger(Class<?> clazz) throws Exception {
			super(clazz);
			_logger = org.slf4j.LoggerFactory.getLogger(clazz);
		}

		@Override
		public void debug(String message) {
			super.debug(message);
			_logger.debug(message);
		}

		@Override
		public void debug(String format, Object arg) {
			super.debug(format, arg);
			_logger.debug(format, arg);
		}

		@Override
		public void debug(String format, Object arg1, Object arg2) {
			super.debug(format, arg1, arg2);
			_logger.debug(format, arg1, arg2);
		}

		@Override
		public void debug(String format, Object... arguments) {
			super.debug(format, arguments);
			_logger.debug(format, arguments);
		}

		@Override
		public void error(String message) {
			super.error(message);
			_logger.error(message);
		}

		@Override
		public void error(String format, Object arg) {
			super.error(format, arg);
			_logger.error(format, arg);
		}

		@Override
		public void error(String format, Object arg1, Object arg2) {
			super.error(format, arg1, arg2);
			_logger.error(format, arg1, arg2);
		}

		@Override
		public void error(String format, Object... arguments) {
			super.error(format, arguments);
			_logger.error(format, arguments);
		}

		@Override
		public String getName() {
			return _logger.getName();
		}

		@Override
		public void info(String message) {
			super.info(message);
			_logger.info(message);
		}

		@Override
		public void info(String format, Object arg) {
			super.info(format, arg);
			_logger.info(format, arg);
		}

		@Override
		public void info(String format, Object arg1, Object arg2) {
			super.info(format, arg1, arg2);
			_logger.info(format, arg1, arg2);
		}

		@Override
		public void info(String format, Object... arguments) {
			super.info(format, arguments);
			_logger.info(format, arguments);
		}

		@Override
		public boolean isDebugEnabled() {
			return _logger.isDebugEnabled();
		}

		@Override
		public boolean isErrorEnabled() {
			return _logger.isErrorEnabled();
		}

		@Override
		public boolean isInfoEnabled() {
			return _logger.isInfoEnabled();
		}

		@Override
		public boolean isTraceEnabled() {
			return _logger.isTraceEnabled();
		}

		@Override
		public boolean isWarnEnabled() {
			return _logger.isWarnEnabled();
		}

		@Override
		public void trace(String message) {
			super.trace(message);
			_logger.trace(message);
		}

		@Override
		public void trace(String format, Object arg) {
			super.trace(format, arg);
			_logger.trace(format, arg);
		}

		@Override
		public void trace(String format, Object arg1, Object arg2) {
			super.trace(format, arg1, arg2);
			_logger.trace(format, arg1, arg2);
		}

		@Override
		public void trace(String format, Object... arguments) {
			super.trace(format, arguments);
			_logger.trace(format, arguments);
		}

		@Override
		public void warn(String message) {
			super.warn(message);
			_logger.warn(message);
		}

		@Override
		public void warn(String format, Object arg) {
			super.warn(format, arg);
			_logger.warn(format, arg);
		}

		@Override
		public void warn(String format, Object arg1, Object arg2) {
			super.warn(format, arg1, arg2);
			_logger.warn(format, arg1, arg2);
		}

		@Override
		public void warn(String format, Object... arguments) {
			super.warn(format, arguments);
			_logger.warn(format, arguments);
		}

	}

}