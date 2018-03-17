package org.apache.aries.cdi.container.internal.util;

import java.util.function.Predicate;

import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cm.ConfigurationEvent;

public class Predicates {

	private Predicates() {
		// no instances
	}

	public static Predicate<ConfigurationTemplateDTO> isMatchingConfiguration(ConfigurationEvent event) {
		return new MatchingConfigurationPredicate(event);
	}

	private static class MatchingConfigurationPredicate implements Predicate<ConfigurationTemplateDTO> {

		public MatchingConfigurationPredicate(ConfigurationEvent event) {
			this.event = event;
		}

		@Override
		public boolean test(ConfigurationTemplateDTO t) {
			if (((t.maximumCardinality == MaximumCardinality.MANY) && t.pid.equals(event.getFactoryPid())) ||
					((t.maximumCardinality == MaximumCardinality.ONE) && t.pid.equals(event.getPid()))) {
				return true;
			}
			return false;
		}

		private final ConfigurationEvent event;

	}
}
