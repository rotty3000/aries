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

package org.apache.aries.cdi.container.internal.model;

import static org.apache.aries.cdi.container.internal.model.Constants.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.cdi.container.internal.component.OSGiBean;
import org.apache.aries.cdi.container.internal.exception.BeanElementException;
import org.apache.aries.cdi.container.internal.exception.BlacklistQualifierException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OSGiBeansHandler extends DefaultHandler {

	public OSGiBeansHandler(List<URL> beanDescriptorURLs, ClassLoader classLoader) {
		_beanDescriptorURLs = beanDescriptorURLs;
		_classLoader = classLoader;
	}

	public BeansModel createBeansModel() {
		return new BeansModel(_beans, _qualifierBlackList, _errors, _beanDescriptorURLs);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (matches(BEAN_ELEMENT, uri, localName)) {
			String className = getValue(CDI10_URI, CLASS_ATTRIBUTE, attributes);

			try {
				Class<?> clazz = _classLoader.loadClass(className);

				_beanModel = new OSGiBean.Builder(clazz);
			}
			catch (ReflectiveOperationException roe) {
				_errors.add(
					new BeanElementException(
						String.format("Error loading class for <cdi:bean class=\"%s\">", className),
						roe));
			}
		}
		if (matches(QUALIFIER_ELEMENT, uri, localName)) {
			String className = getValue(CDI10_URI, NAME_ATTRIBUTE, attributes);

			try {
				_qualifier = _classLoader.loadClass(className);
			}
			catch (ReflectiveOperationException roe) {
				_errors.add(
					new BlacklistQualifierException(
						String.format("Error loading class for <cdi:qualifier name=\"%s\">", className),
						roe));
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (matches(BEAN_ELEMENT, uri, localName) && (_beanModel != null)) {
			OSGiBean osgiBean = _beanModel.build();
			_beans.put(osgiBean.getBeanClass().getName(), osgiBean);
			_beanModel = null;
		}
		if (matches(QUALIFIER_ELEMENT, uri, localName) && (_qualifier != null)) {
			_qualifierBlackList.add(_qualifier);
			_qualifier = null;
		}
	}

	private boolean matches(String elementName, String uri, String localName) {
		if (localName.equals(elementName) && ("".equals(uri) || CDI_URIS.contains(uri))) {
			return true;
		}
		return false;
	}
	public static boolean getBoolean(String uri, String localName, Attributes attributes, boolean defaultValue) {
		String value = getValue(uri, localName, attributes);

		if (value == null) {
			return defaultValue;
		}

		return Boolean.parseBoolean(value);
	}

	public static String getValue(String uri, String localName, Attributes attributes) {
		return getValue(uri, localName, attributes, "");
	}

	public static String getValue(String uri, String localName, Attributes attributes, String defaultValue) {
		String value = attributes.getValue(uri, localName);

		if (value == null) {
			value = attributes.getValue("", localName);
		}

		if (value != null) {
			value = value.trim();
		}

		if (value == null) {
			return defaultValue;
		}

		return value;
	}

	public static String[] getValues(String uri, String localName, Attributes attributes) {
		return getValues(uri, localName, attributes, new String[0]);
	}

	public static String[] getValues(String uri, String localName, Attributes attributes, String[] defaultValue) {
		String value = getValue(uri, localName, attributes, "");

		if (value.length() == 0) {
			return defaultValue;
		}

		return value.split("\\s+");
	}

	private final ClassLoader _classLoader;
	private final Map<String, OSGiBean> _beans = new HashMap<>();
	private final List<URL> _beanDescriptorURLs;
	private OSGiBean.Builder _beanModel;
	private List<Throwable> _errors = new ArrayList<>();
	private Class<?> _qualifier;
	private final List<Class<?>> _qualifierBlackList = new ArrayList<>();

}
