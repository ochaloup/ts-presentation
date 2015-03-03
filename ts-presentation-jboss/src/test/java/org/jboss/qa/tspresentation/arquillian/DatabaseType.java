package org.jboss.qa.tspresentation.arquillian;

/**
 * Databases that could be used in tests.
 */
public enum DatabaseType {
    POSTGRESQL, POSTGRESPLUS, MYSQL, ORACLE, DB2, SYBASE, MSSQL, UNKNOWN;

    /**
     * TODO: not working properly :)
     */
    public static DatabaseType convert(final String dbStringToEvaluate) {
        if(dbStringToEvaluate.toUpperCase().contains(POSTGRESQL.name())) {
            return POSTGRESQL;
        } else {
            return UNKNOWN;
        }
    }
}
