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

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.osgi.service.log.Logger;

public class ConfigurationPhase extends Phase {

	public ConfigurationPhase(ContainerState containerState, Phase next) {
		super(containerState, next);
	}

	@Override
	public boolean close() {
		_log.debug(l -> l.debug("CCR Begin configuration CLOSE on {}", bundle()));

		next.ifPresent(
			next -> submit(Op.CONFIGURATION_LISTENER_CLOSE, next::close).then(
				s -> {
					_log.debug(l -> l.debug("CCR Ended configuration CLOSE on {}", bundle()));

					return s;
				},
				f -> {
					_log.error(l -> l.error("CCR Error in configuration CLOSE on {}", bundle(), f.getFailure()));

					error(f.getFailure());
				}
			)
		);

		return true;
	}

	@Override
	public boolean open() {
		_log.debug(l -> l.debug("CCR Begin configuration OPEN on {}", bundle()));

		next.ifPresent(
			next -> submit(Op.CONFIGURATION_LISTENER_OPEN, next::open).then(
				s -> {
					_log.debug(l -> l.debug("CCR Ended configuration OPEN on {}", bundle()));

					return s;
				},
				f -> {
					_log.error(l -> l.error("CCR Error in configuration OPEN on {}", bundle(), f.getFailure()));

					error(f.getFailure());
				}
			)
		);

		return true;
	}

	private static final Logger _log = Logs.getLogger(ConfigurationPhase.class);

}