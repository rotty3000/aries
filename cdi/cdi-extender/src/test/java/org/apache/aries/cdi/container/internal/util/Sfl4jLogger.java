package org.apache.aries.cdi.container.internal.util;

import org.apache.aries.cdi.container.internal.util.Logs.SysoutLogger;

public class Sfl4jLogger extends SysoutLogger {

	private final org.slf4j.Logger _logger;

	public Sfl4jLogger(String name) throws Exception {
		super(name);
		_logger = org.slf4j.LoggerFactory.getLogger(name);
	}

	@Override
	public void debug(String message) {
		_logger.debug(message);
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
	public void error(String message) {
		_logger.error(message);
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
	public String getName() {
		return _logger.getName();
	}

	@Override
	public void info(String message) {
		_logger.info(message);
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
	public void warn(String message) {
		_logger.warn(message);
	}

	@Override
	public void warn(String format, Object arg) {
		_logger.warn(format, arg);
	}

	@Override
	public void warn(String format, Object arg1, Object arg2) {
		_logger.warn(format, arg1, arg2);
	}

	@Override
	public void warn(String format, Object... arguments) {
		_logger.warn(format, arguments);
	}

}