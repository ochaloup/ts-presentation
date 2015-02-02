package org.jboss.qa.tspresentation.utils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

public class PropertiesLoader {
  private static final String PROPERTIES_FILE_PROPERTY = "property.file";
  private static final String DEFAULT_PROPERTIES_FILE_NAME = "resource.properties";
      
  private static final boolean THROW_ON_LOAD_FAILURE = true;
  private static final boolean LOAD_AS_RESOURCE_BUNDLE = false;
  private static final String SUFFIX = ".properties";

  public static Properties loadProperties() {
      String propertyFileName = System.getProperty(PROPERTIES_FILE_PROPERTY) != null ?
              System.getProperty(PROPERTIES_FILE_PROPERTY) : DEFAULT_PROPERTIES_FILE_NAME;
      // loading properties from file
      Properties properties = loadPropertiesFromFile(propertyFileName);
      // loading properties from system and in case overriding the previously loaded
      @SuppressWarnings({ "unchecked", "rawtypes" })
      Map<String, String> envProperties = new HashMap<String, String>((Map) System.getProperties());
      properties.putAll(envProperties);
      return properties;
  }
  
  /**
   * Loading properties from where classloader could see them.
   *  
   * Kindly borrowed from: 
   * http://www.javaworld.com/article/2077352/java-se/smartly-load-your-properties.html
   * 
   * @param name
   *          classpath resource name [may not be null]
   * @param loader
   *          classloader through which to load the resource [null is
   *          equivalent to the application loader]
   * @return resource converted to java.util.Properties [may be null if the
   *         resource was not found and THROW_ON_LOAD_FAILURE is false]
   * @throws IllegalArgumentException
   *           if the resource was not found and THROW_ON_LOAD_FAILURE is true
   */
  private static Properties loadPropertiesFromFile(String name, ClassLoader loader) {
    if (name == null)
      throw new IllegalArgumentException("null input: name");
    if (name.startsWith("/"))
      name = name.substring(1);
    if (name.endsWith(SUFFIX))
      name = name.substring(0, name.length() - SUFFIX.length());
    Properties result = null;
    InputStream in = null;
    try {
      if (loader == null)
        loader = ClassLoader.getSystemClassLoader();
      if (LOAD_AS_RESOURCE_BUNDLE) {
        name = name.replace('/', '.');
        // Throws MissingResourceException on lookup failures:
        final ResourceBundle rb = ResourceBundle.getBundle(name, Locale.getDefault(), loader);
        result = new Properties();
        for(String key: rb.keySet()) {
          final String value = rb.getString(key);
          result.put(key, value);
        }
      } else {
        name = name.replace('.', '/');
        if (!name.endsWith(SUFFIX))
          name = name.concat(SUFFIX);
        // Returns null on lookup failures:
        in = loader.getResourceAsStream(name);
        if (in != null) {
          result = new Properties();
          result.load(in);
          // Can throw IOException
        }
      }
    } catch (Exception e) {
      result = null;
    } finally {
      if (in != null)
        try {
          in.close();
        } catch (Throwable ignore) {
        }
    }
    if (THROW_ON_LOAD_FAILURE && (result == null)) {
      throw new IllegalArgumentException("could not load [" + name+ "]"
          + " as " + (LOAD_AS_RESOURCE_BUNDLE ? "a resource bundle" : "a classloader resource"));
    }
    return result;
  }

  /**
   * A convenience overload of {@link #loadProperties(String, ClassLoader)}
   * that uses the current thread's context classloader.
   */
  private static Properties loadPropertiesFromFile(final String name) {
    return loadPropertiesFromFile(name, Thread.currentThread().getContextClassLoader());
  }

}
