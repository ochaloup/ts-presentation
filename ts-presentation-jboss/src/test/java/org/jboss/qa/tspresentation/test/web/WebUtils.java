package org.jboss.qa.tspresentation.test.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

public class WebUtils {

    public static String readUrl(final String url) {
        try {
            return readUrl(new URL(url));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Can't get url from string " + url);
        }
    }

    public static String readUrl(final URL url) {
        StringBuffer resultData = new StringBuffer();
        BufferedReader in = null;

        try {
            URLConnection urlConnection = url.openConnection();
             in = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null)
                resultData.append(inputLine);
        } catch (Exception e) {
            throw new RuntimeException("Can't read  data from url " + url, e);
        } finally {
            if(in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        return resultData.toString();
    }

    public static URL concatUrl(final URL baseUrl, final String extraPath) {
        try {
            URI uri = baseUrl.toURI();
            String newPath = uri.getPath() + (uri.getPath().endsWith("/") ? "" : "/") + extraPath;
            URI newUri = uri.resolve(newPath);
            return newUri.toURL();
        } catch (Exception e) {
            throw new RuntimeException("Can't add extra path " + extraPath + " to url " + baseUrl, e);
        }
    }
}
