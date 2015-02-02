package org.jboss.qa.tspresentation.utils;

import java.util.Properties;

/**
 * Globaly used properties names in project.
 */
public class ProjectProperties {
    public static final Properties GLOBAL_PROPERTIES = PropertiesLoader.loadProperties();

    public static final String PROPERTIES_FILE_PROPERTY = "property.file";
    public static final String DEFAULT_PROPERTIES_FILE_NAME = "resource.properties";

    public static final String JAR_LIBRARY = "jar.library";
}
