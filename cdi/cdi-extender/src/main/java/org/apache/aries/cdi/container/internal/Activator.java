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

import static org.osgi.namespace.extender.ExtenderNamespace.EXTENDER_NAMESPACE;
import static org.osgi.service.cdi.CdiConstants.CDI_CAPABILITY_NAME;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.CDI;

import org.apache.aries.cdi.container.internal.command.CdiCommand;
import org.apache.aries.cdi.provider.CDIProvider;
import org.apache.felix.utils.extender.AbstractExtender;
import org.apache.felix.utils.extender.Extension;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends AbstractExtender {

	static {
		CDI.setCDIProvider(new CDIProvider());
	}

	public Activator() {
		setSynchronous(true);
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		if (_log.isDebugEnabled()) {
			_log.debug("CDIe - starting {}", bundleContext.getBundle());
		}

		_bundleContext = bundleContext;

		registerCdiCommand();

		super.start(bundleContext);

		if (_log.isDebugEnabled()) {
			_log.debug("CDIe - started {}", bundleContext.getBundle());
		}
	}

	private void registerCdiCommand() {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("osgi.command.scope", "cdi");
		properties.put("osgi.command.function", new String[] {"list", "info"});

		_command = new CdiCommand();
		_commandRegistration = _bundleContext.registerService(Object.class, _command, properties);
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		if (_log.isDebugEnabled()) {
			_log.debug("CDIe - stoping {}", bundleContext.getBundle());
		}

		super.stop(bundleContext);

		if (_log.isDebugEnabled()) {
			_log.debug("CDIe - stoped {}", bundleContext.getBundle());
		}

		if (_commandRegistration != null) {
			_commandRegistration.unregister();
		}
	}

	@Override
	protected Extension doCreateExtension(Bundle bundle) throws Exception {
		if (!requiresCdiExtender(bundle)) {
			return null;
		}

		return new CdiBundle(_bundleContext.getBundle(), bundle, _command);
	}

	@Override
	protected void debug(Bundle bundle, String msg) {
	}

	@Override
	protected void warn(Bundle bundle, String msg, Throwable t) {
		if (_log.isWarnEnabled()) {
			_log.warn(msg, t);
		}
	}

	@Override
	protected void error(String msg, Throwable t) {
		if (_log.isErrorEnabled()) {
			_log.error(msg, t);
		}
	}

	private boolean requiresCdiExtender(Bundle bundle) {
		BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
		List<BundleWire> requiredBundleWires = bundleWiring.getRequiredWires(EXTENDER_NAMESPACE);

		for (BundleWire bundleWire : requiredBundleWires) {
			Map<String, Object> attributes = bundleWire.getCapability().getAttributes();

			if (attributes.containsKey(EXTENDER_NAMESPACE) &&
				attributes.get(EXTENDER_NAMESPACE).equals(CDI_CAPABILITY_NAME)) {

				Bundle providerWiringBundle = bundleWire.getProviderWiring().getBundle();

				if (providerWiringBundle.equals(_bundleContext.getBundle())) {
					return true;
				}
			}
		}

		return false;
	}

	private static final Logger _log = LoggerFactory.getLogger(Activator.class);

	private BundleContext _bundleContext;
	private CdiCommand _command;
	private ServiceRegistration<?> _commandRegistration;

}