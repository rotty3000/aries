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

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.container.internal.bean.BundleContextBean;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.Logger;

public class BundleContextExtension implements Extension {

	public BundleContextExtension(BundleContext bundleContext) {
		_bundleContext = bundleContext;
	}

	void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager manager) {
		if (_log.isDebugEnabled()) {
			_log.debug("CCR Adding BundleContext {}", _bundleContext);
		}

		abd.addBean(new BundleContextBean(_bundleContext));

		if (_log.isDebugEnabled()) {
			_log.debug("CCR BundleContext added {}", _bundleContext);
		}
	}

	private static final Logger _log = Logs.getLogger(BundleContextExtension.class);

	private final BundleContext _bundleContext;
}
