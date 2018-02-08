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

package org.apache.aries.cdi.container.internal.phase;

import org.apache.aries.cdi.container.internal.container.ContainerDiscovery;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.log.Logs;
import org.apache.aries.cdi.container.internal.util.Syncro;
import org.osgi.service.log.Logger;

public class InitPhase implements Phase {

	public InitPhase(ContainerState containerState, Phase next) {
		_containerState = containerState;
		_nextPhase = next;
	}

	@Override
	public void close() {
		try (Syncro syncro = _lock.open()) {
			if (_log.isDebugEnabled()) {
				_log.debug("CDIe - Begin init CLOSE on {}", _containerState);
			}

			if (_nextPhase != null) {
				_nextPhase.close();
			}

			if (_log.isDebugEnabled()) {
				_log.debug("CDIe - Ended init CLOSE on {}", _containerState);
			}
		}
	}

	@Override
	public void open() {
		try (Syncro syncro = _lock.open()) {
			if (_log.isDebugEnabled()) {
				_log.debug("CDIe - Begin init OPEN on {}", _containerState);
			}

			if (_log.isDebugEnabled()) {
				_log.debug("CDIe - Begin discovery on {}", _containerState);
			}

			new ContainerDiscovery(_containerState);

			if (_log.isDebugEnabled()) {
				_log.debug("CDIe - Ended discovery on {}", _containerState);
			}

			if (_nextPhase != null) {
				_nextPhase.open();
			}

			if (_log.isDebugEnabled()) {
				_log.debug("CDIe - Ended init OPEN on {}", _containerState);
			}
		}
	}

	private static final Logger _log = Logs.getLogger(InitPhase.class);

	private final ContainerState _containerState;
	private final Phase _nextPhase;
	private final Syncro _lock = new Syncro(true);

}