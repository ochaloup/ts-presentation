package org.jboss.qa.tspresentation.utils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;


/**
 * Merging properties from file and system properties.
 */
public class PropertiesLoader {
  private static final boolean THROW_ON_LOAD_FAILURE = true;
  private static final boolean LOAD_AS_RESOURCE_BUNDLE = false;
  private static final String SUFFIX = ".properties";

  /**
   * Loading properties from possible {@link #DEFAULT_PROPERTIES_FILE_NAME} and override
   * them with properties loaded from System. Such merged properties are returned back.
   */
  public static Properties loadProperties() {
      String propertyFileName = System.getProperty(ProjectProperties.PROPERTIES_FILE_PROPERTY) != null ?
              System.getProperty(ProjectProperties.PROPERTIES_FILE_PROPERTY) : ProjectProperties.DEFAULT_PROPERTIES_FILE_NAME;
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
   * Borrowed from:
   * http://www.javaworld.com/article/2077352/java-se/smartly-load-your-properties.html
   *
   * @param resourceName  classpath resource name [may not be null]
   * @param loader  classloader through which to load the resource [null is
   *                equivalent to the application loader]
   * @return  resource converted to java.util.Properties [may be null if the
   *          resource was not found and THROW_ON_LOAD_FAILURE is false]
   * @throws IllegalArgumentException  if the resource was not found and THROW_ON_LOAD_FAILURE is true
   */
  private static Properties loadPropertiesFromFile(final String resourceName, final ClassLoader loader) {
    if (resourceName == null) {
      throw new IllegalArgumentException("null input: name");
    }
    @SuppressWarnings("unused")
    String resourceNameToLoad = resourceName;
    if (resourceName.startsWith("/")) {
        resourceNameToLoad = resourceName.substring(1);
    }
    if (resourceName.endsWith(SUFFIX)) {
        resourceNameToLoad = resourceName.substring(0, resourceName.length() - SUFFIX.length());
    }
    Properties result = null;
    InputStream in = null;
    ClassLoader classLoader = loader;
    try {
      if (loader == null) {
          classLoader = ClassLoader.getSystemClassLoader();
      }
      if (LOAD_AS_RESOURCE_BUNDLE) {
          resourceNameToLoad = resourceName.replace('/', '.');
        // Throws MissingResourceException on lookup failures:
        final ResourceBundle rb = ResourceBundle.getBundle(resourceName, Locale.getDefault(), loader);
        result = new Properties();
        for(String key: rb.keySet()) {
          final String value = rb.getString(key);
          result.put(key, value);
        }
      } else {
          resourceNameToLoad = resourceName.replace('.', '/');
        if (!resourceName.endsWith(SUFFIX)) {
            resourceNameToLoad = resourceName.concat(SUFFIX);
        }
        // Returns null on lookup failures:
        in = classLoader.getResourceAsStream(resourceName);
        if (in != null) {
          result = new Properties();
          result.load(in); // this can throw IOException
        }
      }
    } catch (Exception e) {
      result = null;
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (Throwable ignore) {
            // ignoring the close exception
        }
      }
    }
    if (THROW_ON_LOAD_FAILURE && (result == null)) {
      throw new IllegalArgumentException("could not load [" + resourceName+ "]"
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
