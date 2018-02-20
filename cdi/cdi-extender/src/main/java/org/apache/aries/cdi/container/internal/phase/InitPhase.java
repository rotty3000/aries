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

import java.util.concurrent.atomic.AtomicReference;

import org.apache.aries.cdi.container.internal.container.ContainerDiscovery;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.MaximumCardinality;
import org.osgi.service.log.Logger;

public class InitPhase extends Phase {

	public InitPhase(ContainerState containerState, Phase next) {
		super(containerState, next);
	}

	@Override
	public boolean close() {
		_log.debug(l -> l.debug("CCR Begin init CLOSE on {}", bundle()));

		next.ifPresent(
			next -> {
				submit(next::close).then(
					s -> {
						_log.debug(l -> l.debug("CCR Ended init CLOSE on {}", bundle()));

						return s;
					},
					f -> {
						_log.error(l -> l.error("CCR Error in init CLOSE on {}", bundle(), f.getFailure()));

						error(f.getFailure());
					}
				);
			}
		);

		return true;
	}

	@Override
	public boolean open() {
		_log.debug(l -> l.debug("CCR Begin init OPEN on {}", bundle()));

		discover();

		next.ifPresent(
			next -> {
				submit(next::open).then(
					s -> {
						_log.debug(l -> l.debug("CCR Ended init OPEN on {}", bundle()));

						return s;
					},
					f -> {
						_log.error(l -> l.error("CCR Error in init OPEN on {}", bundle(), f.getFailure()));

						error(f.getFailure());
					}
				);
			}
		);

		return true;
	}

	private void discover() {
		_log.debug(l -> l.debug("CCR Begin discovery on {}", bundle()));

		new ContainerDiscovery(containerState);

		// fix up the configuration templates so factory configs are always at the end
		containerState.containerDTO().template.components.stream().forEach(
			c -> {
				final AtomicReference<ConfigurationTemplateDTO> factoryConfig = new AtomicReference<>();

				if (c.configurations.removeIf(
						c2 -> {
							if (c2.maximumCardinality == MaximumCardinality.MANY) {
								factoryConfig.set(c2);
								return true;
							}
							return false;
						}
					)
				) {
					c.configurations.add(factoryConfig.get());
				};
			}
		);

		_log.debug(l -> l.debug("CCR Ended discovery on {}", bundle()));
	}

	private static final Logger _log = Logs.getLogger(InitPhase.class);

}