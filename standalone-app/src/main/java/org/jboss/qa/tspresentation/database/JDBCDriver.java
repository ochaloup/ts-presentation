package org.jboss.qa.tspresentation.database;

import java.net.URL;
import java.util.Properties;

import org.jboss.logging.Logger;
import org.jboss.qa.tspresentation.utils.FileLoader;
import org.jboss.qa.tspresentation.utils.PropertiesLoader;

public class JDBCDriver {
    private static final Logger log = Logger.getLogger(JDBCDriver.class);

    public void loadProperties() {
        Properties properties = PropertiesLoader.loadProperties();
        StringBuffer buf = new StringBuffer();
        for(String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            buf.append(key).append(" => ").append(value).append("\n");
        }
        // log.info(buf.toString());

        URL url = ClassLoader.getSystemClassLoader().getResource("resource.properties");
//        log.info("loaded resource as: " + url);

        log.info(FileLoader.getFileURL("abc"));
        log.info(FileLoader.getFileURL("file:/resource.properties"));
        log.info(FileLoader.getFileURL("file:///resource.properties"));
    }
}
