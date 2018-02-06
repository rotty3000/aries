/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.container.internal;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.log.Logs;
import org.apache.aries.cdi.container.internal.phase.Phase;
import org.apache.aries.cdi.container.internal.util.Throw;
import org.apache.felix.utils.extender.Extension;
import org.osgi.service.log.Logger;

public class CDIBundle implements Extension {

	public CDIBundle(CCR ccr, ContainerState containerState, Phase phase) {
		_ccr = ccr;
		_containerState = containerState;
		_nextPhase = phase;
	}

	@Override
	public void start() throws Exception {
		boolean acquired = false;

		try {
			try {
				acquired = _lock.tryLock(DEFAULT_STOP_TIMEOUT, TimeUnit.MILLISECONDS);
			}
			catch ( InterruptedException e ) {
				Thread.currentThread().interrupt();

				_log.warn(
					"The wait for bundle {0}/{1} being destroyed before starting has been interrupted.",
					_containerState.bundle().getSymbolicName(),
					_containerState.bundle().getBundleId(), e );
			}

			if (_log.isDebugEnabled()) {
				_log.debug("CDIe - bundle detected {}", _containerState.bundle());
			}

			_ccr.add(_containerState.bundle(), _containerState);

			if (_nextPhase != null) {
				try {
					_nextPhase.open();
				}
				catch (Throwable t) {
					_nextPhase.close();

					_containerState.containerDTO().errors.add(Throw.toString(t));
				}
			}
		}
		finally {
			if (acquired) {
				_lock.unlock();
			}
		}
	}

	@Override
	public void destroy() throws Exception {
		boolean acquired = false;

		try {
			try {
				acquired = _lock.tryLock(DEFAULT_STOP_TIMEOUT, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();

				_log.warn(
					"The wait for bundle {0}/{1} being started before destruction has been interrupted.",
					_containerState.bundle().getSymbolicName(),
					_containerState.bundle().getBundleId(), e);
			}

			if (_nextPhase != null) {
				_nextPhase.close();
			}


			if (_log.isDebugEnabled()) {
				_log.debug("CDIe - bundle removed {}", _containerState.bundle());
			}
		}
		finally {
			if (acquired) {
				_lock.unlock();

				_ccr.remove(_containerState.bundle());
			}
		}
	}

	private static final long DEFAULT_STOP_TIMEOUT = 60000; // TODO make this configurable

	private static final Logger _log = Logs.getLogger(CDIBundle.class);

	private final CCR _ccr;
	private final ContainerState _containerState;
	private final Lock _lock = new ReentrantLock();
	private final Phase _nextPhase;

}
