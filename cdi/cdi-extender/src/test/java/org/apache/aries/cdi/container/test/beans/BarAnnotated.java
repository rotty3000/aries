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

package org.apache.aries.cdi.container.test.beans;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.service.cdi.annotations.PID;
import org.osgi.service.cdi.annotations.PID.Policy;
import org.osgi.service.cdi.annotations.Prototype;
import org.osgi.service.cdi.annotations.Reference;

public class BarAnnotated {

	@Inject
	@Reference
	Foo foo;

	@Inject
	@Reference
	Optional<Foo> fooOptional;

	@Inject
	@Reference
	Provider<Collection<Foo>> dynamicFoos;

	@Inject
	@Reference
	Collection<Map.Entry<Map<String, Object>, Foo>> tupleFoos;

	@Inject
	@Prototype
	@Reference
	Collection<ServiceReference<Foo>> serviceReferencesFoos;

	@Inject
	@Reference(Foo.class)
	Collection<Map<String, Object>> propertiesFoos;

	@Inject
	@PID(value = "foo.config", policy = Policy.REQUIRED)
	@Configuration
	Config config;

}
