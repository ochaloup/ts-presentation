package org.jboss.qa.tspresentation.arquillian;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Utility methods for handling JDBC urls and so.
 */
public class DatasourceSetupUtil {
    private static final Logger log = LoggerFactory.getLogger(DatasourceSetupUtil.class);

    public static final String REGEXP_PATTERN_SYBASE = "(jdbc:sybase:Tds:)(.*?):(\\d+)/(.*)";
    public static final String REGEXP_PATTERN_OTHER = "(.*://)(.*?):(\\d+)[;/](.*)";
    public static final String REGEXP_PATTERN_ORACLE = "([^@]*@)([^\\]]*]|[^:]*):(\\d+):(.*)";
    public static final String REGEXP_PATTERN_ORACLE_RAC = "(HOST=)([^)]*)\\)\\(PORT=([^)]*)";
    public static final String REGEXP_PATTERN_ORACLE_RAC_DBNAME = "(SERVICE)_(NAME)(=)([^)]*)";
    public static final String REGEXP_PATTERN_MSSQL_DBNAME = "(;)(DatabaseName)([^=]*=)(.*)";

    public static String getDBTypeJDBCUrlRegexPattern(final DatabaseType dbType, final String jdbcUrl) {
        String pattern;
        if(dbType == DatabaseType.ORACLE) {
            pattern = REGEXP_PATTERN_ORACLE;
            if(!Pattern.compile(pattern).matcher(jdbcUrl).find()) {
                pattern = REGEXP_PATTERN_ORACLE_RAC;
            }
        } else if(dbType == DatabaseType.SYBASE) {
            pattern = REGEXP_PATTERN_SYBASE;
        } else {
            pattern = REGEXP_PATTERN_OTHER;
        }
        return pattern;
    }

    /**
     * Parsing JDBC URL to get connection credentials
     *
     * @param matcherIndex  2 = dbHost, 3 = dbPort, 4 = dbName
     */
    private static List<String> getHostPortMatcherNumber(final DatabaseType dbType, final String jdbcUrl, final int matcherIndex) {
        String pattern = DatasourceSetupUtil.getDBTypeJDBCUrlRegexPattern(dbType, jdbcUrl);

        // if searching for dbName (meaning index 4)
        if(matcherIndex == 4) {
            if(dbType == DatabaseType.MSSQL) {
                pattern = REGEXP_PATTERN_MSSQL_DBNAME;
            } else if (dbType == DatabaseType.ORACLE && Pattern.compile(REGEXP_PATTERN_ORACLE_RAC_DBNAME).matcher(jdbcUrl).find()) {
                pattern = REGEXP_PATTERN_ORACLE_RAC_DBNAME;
            }
        }

        List<String> matcherResults = new ArrayList<String>();
        Matcher matcher = Pattern.compile(pattern).matcher(jdbcUrl);

        // dbHost = matcher.group(2);
        // dbPort = Integer.valueOf(matcher.group(3));
        // dbName = matcher.group(4)
        try {
            while(matcher.find()) {
                matcherResults.add(matcher.group(matcherIndex));
            }
            if(matcherResults.size() <= 0) {
                // throwing our own iex which will be then processed by catch block
                throw new IllegalStateException("Haven't found any match on " + jdbcUrl);
            }
        } catch (IllegalStateException ise) {
            log.error("{} can't recognize jdbc url '{}' for database type '{}': {}", DatasourceSetupUtil.class.getName(), jdbcUrl, dbType, ise);
            throw ise;
        }

        // list to array
        return matcherResults;
    }

    /**
     * Changing host and port in the JDBC url for the provided address of intentional proxy.
     * Done by way of extracting all host and port string in url and then replacing them by new values.
     */
    public static String injectProxy(final DatabaseType dbType, final String jdbcUrl, final String proxyHost, final int[] proxyPorts) {
        String newJdbcUrl = jdbcUrl;
        try {
            for(String host: getHosts(dbType, jdbcUrl)) {
                newJdbcUrl = newJdbcUrl.replaceAll(host, proxyHost);
            }
            int i = 0;
            for(String port: getPorts(dbType, jdbcUrl)) {
                newJdbcUrl = newJdbcUrl.replaceFirst(port, Integer.toString(proxyPorts[i++]));
            }
        } catch (Exception e) {
            log.error("Error on trying to inject proxy host '{}' and port '%s' to jdbc url '{}': {}",
                    proxyHost, proxyPorts, jdbcUrl, e);
            throw new IllegalStateException("Error happens on injecting proxy data to jdbc url", e);
        }
        return newJdbcUrl;
    }

    public static String injectDatabaseName(final DatabaseType dbType, final String jdbcUrl, final String newDatabaseName) {
        return jdbcUrl.replaceAll(getDatabaseName(dbType, jdbcUrl), newDatabaseName);
    }

    public static List<String> getHosts(final DatabaseType dbType, final String jdbcUrl) {
        return getHostPortMatcherNumber(dbType, jdbcUrl, 2);
    }

    public static String getHost(final DatabaseType dbType, final String jdbcUrl) {
        return getHosts(dbType, jdbcUrl).get(0);
    }

    public static List<String> getPorts(final DatabaseType dbType, final String jdbcUrl) {
        return getHostPortMatcherNumber(dbType, jdbcUrl, 3);
    }

    public static int getPort(final DatabaseType dbType, final String jdbcUrl) {
        return Integer.valueOf(getPorts(dbType, jdbcUrl).get(0));
    }

    public static String getDatabaseName(final DatabaseType dbType, final String jdbcUrl) {
        return getHostPortMatcherNumber(dbType, jdbcUrl, 4).get(0);
    }

    /**
     * Check if datasource exists and in case it removes it.
     * The datasource binding is checked on datasources subsystem at xa-data-source and data-source paths.
     */
    public static void removeDatasourceConfigurations(final ManagementOperations operations, final String datasourceName) throws Exception {
        // First we will remove the old datasources if exists
        boolean isDatasourceExists = operations.isDefined(SUBSYSTEM + "=datasources", new String[] {"xa-data-source", datasourceName});
        if(isDatasourceExists) {
            log.debug("XA Datasource '{}' is going to be removed", datasourceName);
            operations.removeXADataSource(datasourceName);
        } else {
            log.debug("Verified that XA Datasource '{}' does not exists in configuration. Remove operation skipped.", datasourceName);
        }
        isDatasourceExists = operations.isDefined(SUBSYSTEM + "=datasources", new String[] {"data-source", datasourceName});
        if(isDatasourceExists) {
            log.debug("Datasource (normal non-xa) '{}' is going to be removed", datasourceName);
            operations.removeDataSource(datasourceName);
        } else {
            log.debug("Verified that Datasource (normal non-xa) '{}' does not exists in configuration. Remove operation skipped.", datasourceName);
        }
    }
}
