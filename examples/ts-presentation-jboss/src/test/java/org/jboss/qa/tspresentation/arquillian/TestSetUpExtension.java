package org.jboss.qa.tspresentation.arquillian;

import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * Linked from src/test/resources/META-INF/services/org.jboss.arquillian.core.spi.LoadableExtension
 */
public class TestSetUpExtension implements LoadableExtension {
    public void register(final ExtensionBuilder builder) {
        builder.observer(TestSetUpObserver.class);
    }
}
