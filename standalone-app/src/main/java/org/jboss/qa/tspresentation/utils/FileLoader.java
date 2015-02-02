package org.jboss.qa.tspresentation.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Trying to load a file.
 */
public class FileLoader {

    /**
     * Loading a jar.
     *
     * @param jarName  it could be absolute path to file or the file could be on
     *                 class path or at lib folder to be loaded from
     * @return  class loader where the jar is loaded in
     */
    public static ClassLoader loadJar(final String jarName) {
        URL url = getFileURL("file:foo.jar");
        final URLClassLoader loader = new URLClassLoader (new URL[] {url});
        // Class cl = Class.forName ("Foo", true, loader);
        // Runnable foo = (Runnable) cl.newInstance();

        // adding shutdown hook to close loader kindly
        Runtime.getRuntime().addShutdownHook(new Thread("Class-loader-close") {
            @Override
            public void run() {
                try {
                    loader.close();
                } catch (IOException ignore) {
                }
            }
        });

        return loader;
    }

    private static File urlToFile(final URL url) {
        File file;
        try {
          file = new File(url.toURI());
        } catch(URISyntaxException e) {
          file = new File(url.getPath());
        }
        return file;
    }

    /**
     * Searching for file
     * 1. as absolute path
     * 2. as resource of class loader
     * 3. at {@link ProjectProperties#JAR_LIBRARY} path
     */
    public static URL getFileURL(final String fileName) {
        File file = new File(fileName);

        try {
            // if url then convert to file
            if(!file.exists() && fileName.startsWith("file:")) {
                URL url = new URL(fileName);
                file = urlToFile(url);
            }
            // load as resource of class loader
            if(!file.exists()) {
                URL url = ClassLoader.getSystemClassLoader().getResource(file.getPath());
                if(url != null) {
                    file = urlToFile(url);
                }
            }
            // try to load from jar library path
            if(!file.exists()) {
                file = new File(ProjectProperties.GLOBAL_PROPERTIES.getProperty(ProjectProperties.JAR_LIBRARY), fileName);
            }

/*            if(!file.exists()) {
                throw new IllegalStateException("Can't load file " + fileName);
            }
*/
            return file.toURI().toURL();
        } catch (MalformedURLException mue) {
            throw new RuntimeException(mue);
        }
    }
}
