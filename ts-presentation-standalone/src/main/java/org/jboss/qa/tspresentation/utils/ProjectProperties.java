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


    // Connection configuration properties
    public static final String JDBC_DRIVER_FILEPATH = "db.jdbc_driver";
    public static final String JDBC_CLASS = "db.jdbc_class";
    public static final String DB_URL = "db.jdbc_url";
    public static final String DB_PASSWORD = "db.password";
    public static final String DB_USERNAME = "db.username";
    public static final String XA_DATASOURCE_CLASS = "datasource.class.xa";

    // -----------------------------
    // ----- LOADED PROPERTIES -----
    // -----------------------------
    private static final Properties GLOBAL_PROPERTIES = PropertiesLoader.loadProperties();

    public static Properties getAllProperties() {
        return GLOBAL_PROPERTIES;
    }
    public static String get(final String propertyName) {
        return GLOBAL_PROPERTIES.getProperty(propertyName);
    }
}
