package org.jboss.qa.tspresentation.test.jpa;

import java.io.File;
import java.io.IOException;

import org.jboss.qa.tspresentation.utils.FileLoader;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

public class JpaUtils {
    private static final String JTA_DS_PERSISTENCE_XML_FILE_NAME = "jta-ds-persistence.xml";
    private static final String JTA_JDBC_PERSISTENCE_XML_FILE_NAME = "jta-jdbc-persistence.xml";

    public static JavaArchive getShrinkWrapJar(final String deploymentName) {
        return ShrinkWrap.create(JavaArchive.class, deploymentName + ".jar")
                .addPackage("org.jboss.qa.tspresentation.jpa")
                .addPackage("org.jboss.qa.tspresentation.utils") // utilities to do sql queries
                .addClass(ProjectProperties.class); // constant definitions
    }

    public static String getFileContent(final String fileName) {
        try {
            File file = FileLoader.getFile(fileName);
            return new String(java.nio.file.Files.readAllBytes(file.toPath()));
        } catch (IOException ioe) {
            throw new IllegalStateException("Can't read file " + fileName, ioe);
        }
    }

    public static String getJtaPuWithJtaTagAndJtaDsPersistenceXml() {
        String content = getFileContent(JTA_DS_PERSISTENCE_XML_FILE_NAME);
        return changeForDatasource(content, true, ProjectProperties.NON_XA_DATASOURCE_JNDI);
    }

    public static String getJtaPuWithJtaTagAndNonJtaDsPersistenceXml() {
        String content = getFileContent(JTA_DS_PERSISTENCE_XML_FILE_NAME);
        return changeForDatasource(content, true, ProjectProperties.NON_XA_NON_JTA_DATASOURCE_JNDI);
    }

    public static String getJtaPuWithNonJtaTagAndJtaDsPersistenceXml() {
        String content = getFileContent(JTA_DS_PERSISTENCE_XML_FILE_NAME);
        return changeForDatasource(content, false, ProjectProperties.NON_XA_DATASOURCE_JNDI);
    }

    public static String getJtaPuWithNonJtaTagAndNonJtaDsPersistenceXml() {
        String content = getFileContent(JTA_DS_PERSISTENCE_XML_FILE_NAME);
        return changeForDatasource(content, false, ProjectProperties.NON_XA_NON_JTA_DATASOURCE_JNDI);
    }

    public static String getJtaPuWithJdbcPropertiesDsPersistenceXml() {
        String content = getFileContent(JTA_JDBC_PERSISTENCE_XML_FILE_NAME);
        return changeForJdbcProperties(content);
    }

    public static String setAsResourceLocal(final String content) {
        return changeTransactionType(content, "RESOURCE_LOCAL");
    }

    public static String setAsJTA(final String content) {
        return changeTransactionType(content, "JTA");
    }

    public static String addTag(final String content,final String textToAdd) {
        return content
                .replaceAll("(<persistence-unit.*)", "$1\n" + textToAdd);
    }

    /**
     * Changing for different datasource
     */
    private static String changeForDatasource(final String content, final boolean isJtaDatasource, final String datasourceJndi) {
        String tag = isJtaDatasource ? "jta-data-source" : "non-jta-data-source";
        String tagWhole = String.format("<%s>%s</%s>", tag, datasourceJndi, tag);
        return content.replaceAll(".*data-source>.*", tagWhole);
    }

    /**
     * Changing for different jdbc properties
     */
    private static String changeForJdbcProperties(final String content) {
        return content
                .replaceAll("(connection.url\" value=\")[^\"]*", "$1" + ProjectProperties.get(ProjectProperties.DB_URL))
                .replaceAll("(connection.driver_class\" value=\")[^\"]*", "$1" + ProjectProperties.get(ProjectProperties.JDBC_CLASS))
                .replaceAll("(connection.username\" value=\")[^\"]*", "$1" + ProjectProperties.get(ProjectProperties.DB_USERNAME))
                .replaceAll("(connection.password\" value=\")[^\"]*", "$1" + ProjectProperties.get(ProjectProperties.DB_PASSWORD));
    }

    private static String changeTransactionType(final String content, final String transactionType) {
        return content
                .replaceAll("(transaction-type=\")[^\"]*", "$1" + transactionType);
    }
}
