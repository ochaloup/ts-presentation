package org.jboss.qa.tspresentation.utils;

import java.util.Properties;

/**
 * Globaly used properties names in project.
 */
public class ProjectProperties {

    // Property file where properties could be load from
    public static final String PROPERTIES_FILE_PROPERTY = "property.file";
    public static final String DEFAULT_PROPERTIES_FILE_NAME = "resource.properties";
    // Property to specify folder where library jar files will be searched in
    public static final String JAR_LIBRARY = "jar.library";

    // Properties - merged from file and from system
    private static final Properties GLOBAL_PROPERTIES = PropertiesLoader.loadProperties();

    public static Properties getAllProperties() {
        return GLOBAL_PROPERTIES;
    }
    public static String get(final String propertyName) {
        return GLOBAL_PROPERTIES.getProperty(propertyName);
    }
}
