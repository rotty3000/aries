package org.apache.aries.cdi.container.internal.container;

import java.util.function.Predicate;

import org.osgi.util.promise.Failure;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;

public interface CheckedCallback<T, R> extends Failure, Predicate<Op>, Success<T, R> {

	@Override
	@SuppressWarnings("unchecked")
	public default Promise<R> call(Promise<T> resolved) throws Exception {
		return (Promise<R>)resolved;
	}

	@Override
	public default void fail(Promise<?> resolved) throws Exception {
		resolved.getFailure().printStackTrace();
	}

}
