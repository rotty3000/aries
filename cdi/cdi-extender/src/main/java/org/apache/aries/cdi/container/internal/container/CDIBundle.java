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

package org.apache.aries.cdi.container.internal.container;

import org.apache.aries.cdi.container.internal.CCR;
import org.apache.aries.cdi.container.internal.phase.Phase;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.apache.felix.utils.extender.Extension;
import org.osgi.service.log.Logger;

public class CDIBundle extends Phase implements Extension {

	public CDIBundle(CCR ccr, ContainerState containerState, Phase next) {
		super(containerState, next);
		_ccr = ccr;
	}

	@Override
	public boolean close() {
		_log.debug(l -> l.debug("CCR Begin cdibundle CLOSE on {}", bundle()));

		next.ifPresent(
			next -> submit(Op.INIT_CLOSE, next::close).then(
				s -> {
					_log.debug(l -> l.debug("CCR Ended cdibundle CLOSE on {}", bundle()));

					return s;
				},
				f -> {
					_log.error(l -> l.error("CCR Error in cdibundle CLOSE on {}", bundle(), f.getFailure()));

					error(f.getFailure());
				}
			)
		);

		_ccr.remove(bundle());

		return true;
	}

	@Override
	public void destroy() throws Exception {
		close();
	}

	@Override
	public boolean open() {
		_log.debug(l -> l.debug("CCR Begin cdibundle OPEN on {}", bundle()));

		_ccr.add(containerState.bundle(), containerState);

		next.ifPresent(
			next -> submit(Op.INIT_OPEN, next::open).then(
				s -> {
					_log.debug(l -> l.debug("CCR Ended cdibundle OPEN on {}", bundle()));

					return s;
				},
				f -> {
					_log.error(l -> l.error("CCR Error in cdibundle OPEN on {}", bundle(), f.getFailure()));

					error(f.getFailure());
				}
			)
		);

		return false;
	}

	@Override
	public void start() throws Exception {
		open();
	}

	private static final Logger _log = Logs.getLogger(CDIBundle.class);

	private final CCR _ccr;

}
