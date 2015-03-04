/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.qa.tspresentation.arquillian;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.adapters.jdbc.extensions.db2.DB2ExceptionSorter;
import org.jboss.jca.adapters.jdbc.extensions.db2.DB2ValidConnectionChecker;
// import org.jboss.jca.adapters.jdbc.extensions.mssql.MSSQLExceptionSorter;
import org.jboss.jca.adapters.jdbc.extensions.mssql.MSSQLValidConnectionChecker;
import org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLExceptionSorter;
import org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLValidConnectionChecker;
import org.jboss.jca.adapters.jdbc.extensions.novendor.NullExceptionSorter;
import org.jboss.jca.adapters.jdbc.extensions.novendor.NullValidConnectionChecker;
import org.jboss.jca.adapters.jdbc.extensions.oracle.OracleExceptionSorter;
import org.jboss.jca.adapters.jdbc.extensions.oracle.OracleValidConnectionChecker;
import org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter;
import org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker;
import org.jboss.jca.adapters.jdbc.extensions.sybase.SybaseExceptionSorter;
import org.jboss.jca.adapters.jdbc.extensions.sybase.SybaseValidConnectionChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

public class ManagementOperations {
    public static final Logger log = LoggerFactory.getLogger(ManagementOperations.class);

    public static final String JACORB_TRANSACTIONS_JTA = "spec";
    public static final String JACORB_TRANSACTIONS_JTS = "on";

    // supposed to be filled by composite operations
    private ModelNode batch = null;
    // definition whether the batch is in run
    private boolean isBatchMode = false;
    // says if successful management execution will be logged
    private boolean isLoggingExecutionSupress = false;

    private final ManagementClient managementClient;

    public ManagementOperations(final ManagementClient client) {
        this.managementClient = client;
    }

    // -----------------------------------------------------
    // ------------------- GETTERS/SETTERS -----------------
    // -----------------------------------------------------
    public void setLoggingExecutionSupress(final boolean isLoggingExecutionSupress) {
        this.isLoggingExecutionSupress = isLoggingExecutionSupress;
    }

    // -----------------------------------------------------
    // ---------------- HOME MADE CLI ADDR PARSER ----------
    // -----------------------------------------------------
    /**
     * Address in format /subsystem=web/something=something will be parsed to ModelNode
     * where "address" will be set an you are expected to add only operations on the
     * returned ModelNode.
     */
    public ModelNode parseAddressToOperation(final String address) {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(parseAddress(address));
        return operation;
    }
    /**
     * Address in "standart" format: /subsystem=web/something=something/...
     * It will return ModelNode which is expected to be put as "address"
     * part of ModelNode which will be executed afterwards.
     */
    public ModelNode parseAddress(final String address) {
        String[] splitAddrParts = address.split("/");
        List<PathElement> elements = new ArrayList<PathElement>();

        for (String addrPart: splitAddrParts) {
            addrPart = addrPart.trim();
            // slashes in values must come with replaced placeholder values
            addrPart = addrPart.replaceAll("#", "/");
            if (!addrPart.isEmpty()) {
                String[] nameVal = addrPart.split("=");
                if (nameVal.length == 1 ) {
                    elements.add(PathElement.pathElement(addrPart));
                } else if (nameVal.length == 2) {
                    elements.add(PathElement.pathElement(nameVal[0], nameVal[1]));
                } else {
                    throw new RuntimeException("There is problem with element " + addrPart
                            + " for address: " + address + ". Not able to parse.");
                }
            }
        }
        PathAddress pa = PathAddress.pathAddress(elements);
        ModelNode mdAddress = pa.toModelNode();

        return mdAddress;
    }

    // -----------------------------------------------------
    // ------------------------- JTS/JTA -------------------
    // -----------------------------------------------------
    /**
     * /subsystem=transactions:write-attribute(name=jts,value=false|true)
     */
    public void setJTS(final boolean enabled) throws IOException {
        ModelNode operation = parseAddressToOperation("/subsystem=transactions");
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get("name").set("jts");
        operation.get("value").set(enabled);
        executeOperation(operation);

        String transactionsOnJacorb = (enabled) ? JACORB_TRANSACTIONS_JTS : JACORB_TRANSACTIONS_JTA;
        operation = parseAddressToOperation("/subsystem=jacorb");
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get("name").set("transactions");
        operation.get("value").set(transactionsOnJacorb);
        executeOperation(operation);
    }


    // -----------------------------------------------------
    // -------------------- LOGGING ------------------------
    // -----------------------------------------------------
    public ModelNode setLogger(final String category, final String level) throws IOException {
        String address = "/subsystem=logging/logger=" + category;
        // logger is in the XML named category (with some level)
        if(isDefined("/subsystem=logging", new String[]{"logger", category})) {
            // existing - changing
            return writeAttribute(address, "level", level);
        } else {
            // not existing - adding
            Properties params = new Properties();
            params.put("level", level);
            return add(address, params);
        }
    }


    // -----------------------------------------------------
    // ----------------- DATASOURCES -----------------------
    // -----------------------------------------------------
    public void enable(final String address) throws IOException {
        ModelNode operation = parseAddressToOperation(address);
        operation.get(OP).set(ENABLE);
        executeOperation(operation);
    }

    public void disable(final String address) throws IOException {
        ModelNode operation = parseAddressToOperation(address);
        operation.get(OP).set(DISABLE);
        executeOperation(operation);
    }

    /**
     * @param dataSourceName  name of data source (not the jndi name)
     */
    public void enableDataSource(final String dataSourceName) throws IOException {
         enable("/subsystem=datasources/data-source=" + dataSourceName);
    }
    public void disableDataSource(final String dataSourceName) throws IOException {
        disable("/subsystem=datasources/data-source=" + dataSourceName);
    }
    /**
     *
     * @param xaDataSourceName  name of datasource (not the jndi name)
     */
    public void enableXADataSource(final String xaDataSourceName) throws IOException {
         enable("/subsystem=datasources/xa-data-source=" + xaDataSourceName);
    }
    public void disableXADataSource(final String xaDataSourceName) throws IOException {
        disable("/subsystem=datasources/xa-data-source=" + xaDataSourceName);
    }

    public void addXADataSourceProperty(final ModelNode address, final String name, final String value) throws IOException {
        final ModelNode propertyAddress = address.clone();
        propertyAddress.add("xa-datasource-properties", name);
        propertyAddress.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(propertyAddress);
        operation.get("value").set(value);

        executeOperation(operation);
    }

    public void removeXADataSource(final String datasourceName) throws Exception {
        final ModelNode operation = parseAddressToOperation("/subsystem=datasources/xa-data-source=" + datasourceName);
        operation.get(OP).set(REMOVE);
        executeOperation(operation);
    }

    public void removeDataSource(final String datasourceName) throws Exception {
        final ModelNode operation = parseAddressToOperation("/subsystem=datasources/data-source=" + datasourceName);
        operation.get(OP).set(REMOVE);
        executeOperation(operation);
    }

    public List<ModelNode> listDatasources() throws Exception {
        /*     /subsystem=datasources:read-children-resources(child-type=data-source) */
        final ModelNode operation = getListDatasourceOp();
        operation.get(CHILD_TYPE).set("data-source");
        return executeOperation(operation).asList();
    }

    public List<ModelNode> listXADatasources() throws Exception {
        /*     /subsystem=datasources:read-children-resources(child-type=xa-data-source) */
        final ModelNode operation = getListDatasourceOp();
        operation.get(CHILD_TYPE).set("xa-data-source");
        return executeOperation(operation).asList();
    }

    private ModelNode getListDatasourceOp() {
        final ModelNode operation = parseAddressToOperation("/subsystem=datasources");
        operation.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
        return operation;
    }

    // /subsystem=datasources/xa-data-source=CrashRecoveryDS
    //   :write-attribute(name=recovery-plugin-class-name, value="org.jboss.jca.core.recovery.ConfigurableRecoveryPlugin")
    // /subsystem=datasources/xa-data-source=CrashRecoveryDS
    //   :write-attribute(name=recovery-plugin-properties, value={EnableIsValid => true, IsValidOverride => false, EnableClose = false})
    //
    public void addRecoveryPlugin(final String datasourceName, final String className, final Map<String, String> properties) throws Exception {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.add("xa-data-source", datasourceName);
        address.protect();

        ModelNode nodeProperties = new ModelNode();
        for(Entry<String, String> property: properties.entrySet()) {
            nodeProperties.get(property.getKey()).set(property.getValue());
        }

        writeAttribute(address, "recovery-plugin-class-name", className);
        writeAttribute(address, "recovery-plugin-properties", nodeProperties);
    }

    /**
     * Adding standard datasource
     * /subsystem=datasources/data-source=...:add(params[])
     */
    public void addDatasource(final String datasourceName, final Properties params) throws IOException {
        add("/subsystem=datasources/data-source=" + datasourceName, params);
    }

    public void addDatasource(final String datasourceName, final String jndi, final String jdbcUrl, final String driver) throws IOException {
        addDatasource(datasourceName, jndi, jdbcUrl, driver, new Properties());
    }

    public void addDatasource(final String datasourceName, final String jndi, final String jdbcUrl, final String driver, final Properties paramsToOverride)
            throws IOException{
        addDatasource(datasourceName, jndi, jdbcUrl, driver, paramsToOverride, new Properties());
    }

    /**
     * A note:
     * from EAP 6.3.0.DR2 we have two types of datasources from point of view of the TM 2PC
     * 1) standard ds which will be processed by "old" LRCO algorithm
     * 2) connectable ds which will be processed by "new" CRM algorithm (see https://issues.jboss.org/browse/EAP6-17)
     *    parameter connectable has to be set to true (default is false)
     *
     * @param datasourceName  datasource name which will be added to standalone config
     * @param datasourceJndiName  jndi which will be used to lookup the datasource
     * @param jdbcUrl  url to database to know how to connect to it
     * @param user  username to connect with
     * @param pass  password to connect with
     * @param driver  jdbc driver - name of deployed jar file or name of module
     * @param paramsToOverride  bunch of properties which will override any defaultly used
     * @param connectionProperties  properties which will be added as <connection-property> tag for datasource
     * @throws IOException  throwing io exception when connection fails
     * @throws MgmtOperationException  some problem in execution of the cli dmr command occured
     */
    public void addDatasource(final String datasourceName,
                              final String datasourceJndiName,
                              final String jdbcUrl,
                              final String driver,
                              final Properties paramsToOverride,
                              final Properties connectionProperties) throws IOException{
        Properties params = new Properties();
        params.put("jndi-name", datasourceJndiName);
        params.put("connection-url", jdbcUrl);
        params.put("driver-name", driver);

        params.put("jta", "true");
        params.put("enabled", "true");
        params.put("use-java-context", "true");

        // putting paramsToOverride on top of params properties
        mergeProperties(params, paramsToOverride);

        addDatasource(datasourceName, params);

        // adding XA properties one by one as it's nice
        ModelNode address = parseAddress("/subsystem=datasources/data-source=" + datasourceName);
        for(String key : connectionProperties.stringPropertyNames()) {
            addDatasourceConnectionProperty(address, key, connectionProperties.getProperty(key));
           }

        // when we added it we will enable it
        // enable has to be made as the last step as after enabling there is not reflected any added property
        // (we would need to disable and enable datasource or restart server)
        // enableDataSource(datasourceName);
    }

    /**
     * Adding standard JDBC driver.
     * /subsystem=datasources/jdbc-driver=driverName:add(params[])
     */
    public void addJdbcDriver(final String driverName, final Properties params) throws IOException {
        if (!isResourceExists("/subsystem=datasources/jdbc-driver=" + driverName)) {
            add("/subsystem=datasources/jdbc-driver=" + driverName, params);
        }
    }

    /**
     * Remove JDBC driver from standalone.xml configuration.
     */
    public void removeJdbcDriver(final String driverName) throws IOException {
        final ModelNode operation = parseAddressToOperation("/subsystem=datasources/jdbc-driver=" + driverName);
        operation.get(OP).set(REMOVE);
        executeOperation(operation);
    }

    public void addDatasourceConnectionProperty(final ModelNode address, final String name, final String value) throws IOException {
        final ModelNode propertyAddress = address.clone();
        propertyAddress.add("connection-properties", name);
        propertyAddress.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(propertyAddress);
        operation.get("value").set(value);

        executeOperation(operation);
    }

    /**
     * @param userName  name that will be used to authenticate with db
     * @param password  password to authenticate with db
     *
     * @see #createXADataSource(String, String, DatabaseType, String, String, Properties, Properties)
     */
    public void addXADataSource(final String datasourceName,
                                final String datasourceJndiName,
                                final DatabaseType dbType,
                                final String databaseName,
                                final String userName,
                                final String password,
                                final String xaDatasourceClass,
                                final String driver,
                                final Properties datasourceProperties,
                                final Properties xaDatasourceProperties) throws Exception {
        Properties datasourcePropertiesToPass = datasourceProperties == null ? new Properties() : datasourceProperties;
        datasourceProperties.put("user-name", userName);
        datasourceProperties.put("password", password);
        addXADataSource(datasourceName, datasourceJndiName, dbType, xaDatasourceClass, driver, datasourcePropertiesToPass, xaDatasourceProperties);
    }

    /**
     * By type of the database constructing the properties which will be use to add XA datasource properties
     * in datasource.
     *
     * @param dbType  type of database
     * @param url  url (is used just for Oracle dbs)
     * @param hostName  host name where to connect to
     * @param port  port of db instance stands on
     * @param dbName  name of database where we will connect to
     * @return key value properties
     */
    public Properties prepareXADatasourceProperties(final DatabaseType dbType, final String url, final String hostName, final int port, final String dbName) {
        //-------- XADataSource properties
        Properties resultProperties = new Properties();

        // other XA database specific settings
        switch (dbType) {
            case MYSQL:
                break;
            case POSTGRESQL:
                break;
            case POSTGRESPLUS:
                break;
            case ORACLE:
                break;
            case DB2:
                resultProperties.setProperty("DriverType", "4");
                break;
            case SYBASE:
                resultProperties.setProperty("NetworkProtocol", "Tds");
                break;
            case MSSQL:
                resultProperties.setProperty("SelectMethod", "cursor");
                break;
            case UNKNOWN:
                break;
        }

        if (dbType == DatabaseType.ORACLE) {
            if(url == null) {
                throw new RuntimeException("Oracle database requires URL xa property being defined.");
            }
            resultProperties.setProperty("URL", url);
        } else {
            if(hostName == null) {
                throw new RuntimeException("It's required hostName xa property being defined.");
            }
            if(dbName == null) {
                throw new RuntimeException("It's required dbName xa property being defined.");
            }
            if(port <= 0) {
                throw new RuntimeException("Port has to be a positive integer value.");
            }

            resultProperties.setProperty("ServerName", hostName);
            resultProperties.setProperty("PortNumber", Integer.toString(port));
            resultProperties.setProperty("DatabaseName", dbName);
        }
        return resultProperties;
    }

    /**
     * @param datasourceJndiName  mame of jndi that the datasource will be bound to
     * @param xaDatasourceClass  fully quallified class which take cares of the xa datasource handling
     * @param driver  driver name - name of jar file or module name
     *
     * @see #createXADataSource(String, String, DatabaseType, String, String, Properties, Properties)
     */
    public void addXADataSource(final String datasourceName,
                                final String datasourceJndiName,
                                final DatabaseType dbType,
                                final String xaDatasourceClass,
                                final String driver,
                                final Properties datasourceProperties,
                                final Properties xaDatasourceProperties) throws Exception {
        //-------- DataSource properties
        datasourceProperties.put("jndi-name", datasourceJndiName);
        if (driver == null) {
            throw new RuntimeException("jndi-name cannot be null!");
        }
        datasourceProperties.put("driver-name", driver);

        if (xaDatasourceClass == null) {
            throw new RuntimeException("xa datasource class cannot be null!");
        }
        datasourceProperties.put("xa-datasource-class", xaDatasourceClass);

        // go and execute
        addXADataSource(datasourceName, dbType, datasourceProperties, xaDatasourceProperties);
    }

    public void setDatasourceConnectionValidation(final String datasourceName, final DatabaseType dbType) throws IOException {
        String subsystemDatasourcesAddress = "/subsystem=datasources";
        boolean isDatasourceDefined = isDefined(subsystemDatasourcesAddress, new String[]{"data-source", datasourceName});
        boolean isXaDatasourceDefined = isDefined(subsystemDatasourcesAddress, new String[]{"xa-data-source", datasourceName});

        if(isDatasourceDefined) {
            ModelNode address = parseAddress(subsystemDatasourcesAddress + "/data-source="+datasourceName);
            setValidationAttributes(address, dbType);
        }
        if(isXaDatasourceDefined) {
            ModelNode address = parseAddress(subsystemDatasourcesAddress + "/xa-data-source="+datasourceName);
            setValidationAttributes(address, dbType);
        }
    }

    /**
     * Setting validate-on-match to true + sets definition of connection checker class.
     * For more info on connection validation check https://access.redhat.com/site/solutions/156103
     */
    private void setValidationAttributes(final ModelNode address, final DatabaseType dbType) throws IOException {
        String connectionCheckerClass, exceptionSorterClass;

        switch (dbType) {
        case DB2:
            connectionCheckerClass = DB2ValidConnectionChecker.class.getName();
            exceptionSorterClass = DB2ExceptionSorter.class.getName();
            break;
        case MSSQL:
            connectionCheckerClass = MSSQLValidConnectionChecker.class.getName();
            exceptionSorterClass = NullExceptionSorter.class.getName();
            break;
        case MYSQL:
            connectionCheckerClass = MySQLValidConnectionChecker.class.getName();
            exceptionSorterClass = MySQLExceptionSorter.class.getName();
            break;
        case ORACLE:
            connectionCheckerClass = OracleValidConnectionChecker.class.getName();
            exceptionSorterClass = OracleExceptionSorter.class.getName();
            break;
        case POSTGRESQL:
        case POSTGRESPLUS:
            connectionCheckerClass = PostgreSQLValidConnectionChecker.class.getName();
            exceptionSorterClass = PostgreSQLExceptionSorter.class.getName();
            break;
        case SYBASE:
            connectionCheckerClass = SybaseValidConnectionChecker.class.getName();
            exceptionSorterClass = SybaseExceptionSorter.class.getName();
            break;
        default:
            connectionCheckerClass = NullValidConnectionChecker.class.getName();
            exceptionSorterClass = NullExceptionSorter.class.getName();
            break;
        }

        writeAttribute(address, "validate-on-match", "true");
        writeAttribute(address, "valid-connection-checker-class-name", connectionCheckerClass);
        writeAttribute(address, "exception-sorter-class-name", exceptionSorterClass);
        reload();
    }

    /**
     * This method is counting with the fact that needed properties will be already defined in Properties files.
     *
     * The specific values for databases cames from
     *  https://access.redhat.com/site/documentation/en-US/JBoss_Enterprise_Application_Platform/6/html/\
     *  Administration_and_Configuration_Guide/sect-Example_Datasources.html
     *  The values have to be hardcoded here as they are different for each DB.
     *
     * @param datasourceName  name of datasource that will be added to standalone.xml
     * @param dbType  type of the database - based on this there will be added special jca configurations
     * @param datasourceProperties  properties to define datasource (username, password etc.), properties which id defined here
     *                              will override properties defined in this method (baded on the same key property name)
     * @param xaDatasourceProperties  properties that will be used to set connection (DatabaseName, URL...), see method prepareXaConnectionProperties
     *           xaDatasourceProperties are not the same as datasourceProperties - both of them are used for datasource creation but each of them
     *        in other part of datasource specification
     * @throws Exception
     */
    public void addXADataSource(final String datasourceName,
                                final DatabaseType dbType,
                                final Properties datasourceProperties,
                                final Properties xaDatasourceProperties) throws Exception {
        if (datasourceProperties == null) {
            throw new NullPointerException("It's needed to have some properties here to start with");
        }

        // datasource is created in batch (expected for WildFly server)
        startBatch();

        ModelNode address = parseAddress("/subsystem=datasources/xa-data-source="+datasourceName);
        ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);

        // Database specific setting
        Properties propertiesToSet = new Properties();
        switch (dbType) {
            case MYSQL:
                break;
            case POSTGRESQL:
                break;
            case POSTGRESPLUS:
                break;
            case ORACLE:
                propertiesToSet.put("same-rm-override", "false");
                // maybe this settings is not needed anymore but it's in doc and as Jesper says:
                // "The only correct datasource configuration is that which is part of documentation"
                // so rather leving it as it is
                propertiesToSet.put("no-tx-separate-pool", "true");
                break;
            case DB2:
                propertiesToSet.put("same-rm-override", "false");
                // propertiesToSet.put("no-tx-separate-pool", "true");
                break;
            case SYBASE:
                // not setting same-rm-override causes trouble for JPAMultiXACMRCrashRecoveryTestCase, see doc bz#1185398
                propertiesToSet.put("same-rm-override", "false");
                break;
            case MSSQL:
                propertiesToSet.put("same-rm-override", "false");
                break;
            case UNKNOWN:
                // nothing to do here
                break;
        }

        mergeProperties(propertiesToSet, datasourceProperties);
        // datasource will be created at this point but without XA properties which defines connection data
        addPropertiesToModelNode(operation, propertiesToSet);
        executeOperation(operation);

        // adding XA properties one by one as it's nice
        for(String key : xaDatasourceProperties.stringPropertyNames()) {
            addXADataSourceProperty(address, key, xaDatasourceProperties.getProperty(key));
           }

        // Enable the datasource - this is needed for EAP6 but it causes problems for WildFly
        // enableXADataSource(datasourceName);

        // execute the batch - promote the changes to model
        runBatch();

        // database DB2 has special demands which has to be set by recovery plugin
        if(dbType == DatabaseType.DB2) {
            Map<String, String> db2PluginProperties = new HashMap<String, String>();
            db2PluginProperties.put("EnableIsValid", "true");
            db2PluginProperties.put("IsValidOverride", "false");
            db2PluginProperties.put("EnableClose", "false");
            addRecoveryPlugin(datasourceName, "org.jboss.jca.core.recovery.ConfigurableRecoveryPlugin", db2PluginProperties);
        }
    }


    // -----------------------------------------------------
    // ------------------ MESSAGING ------------------------
    // -----------------------------------------------------
    public void addJmsQueue(final String queueName, final String jndiName) throws IOException  {
        addJmsDestination("jms-queue", queueName, jndiName);
    }

    public void addJmsTopic(final String topicName, final String jndiName) throws IOException  {
        addJmsDestination("jms-topic", topicName, jndiName);
    }

    public void removeJmsQueue(final String queueName) throws IOException  {
        removeJmsDestination("jms-queue", queueName);
    }

    public void removeJmsTopic(final String topicName) throws IOException  {
        removeJmsDestination("jms-topic", topicName);
    }

    private void addJmsDestination(final String destinationType, final String destinationName, final String jndiName) throws IOException {
        ModelNode address = parseAddress("/subsystem=messaging/hornetq-server=default")
                .add(destinationType, destinationName);
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(ADD);
        operation.get("entries").add(jndiName);
        executeOperation(operation);
    }

    private void removeJmsDestination(final String destinationType, final String destinationName) throws IOException {
        ModelNode address = parseAddress("/subsystem=messaging/hornetq-server=default")
                .add(destinationType, destinationName);
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(REMOVE);
        executeOperation(operation);
    }












    // ------------------------------------------------------------
    // -------------------- RELOAD/RESTART ------------------------
    // ------------------------------------------------------------
    public boolean reload() throws IOException {
        /*      :reload()     */
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("reload");
        log.info("operation=" + operation);

        return executeServerReininitalization(true, operation);
    }

    public boolean stop(final boolean isRestart) throws IOException {
        /*      :shutdown(restart=true)     */
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("shutdown");
        operation.get("restart").set(Boolean.toString(isRestart));
        log.info("operation=" + operation);

        return executeServerReininitalization(isRestart, operation);
    }

    private boolean executeServerReininitalization(final boolean isReload, final ModelNode operation)
            throws IOException {
        ModelControllerClient clientToExecute = createInstanceOfRemoteClient();

        // running shutdown or reload operation
        try {
            executeOperation(operation, true, clientToExecute);
        } catch (Exception e) {
            log.error("Exception applying shutdown operation. This is probably fine, as the server probably shut down before the response was sent", e);
        } finally {
            clientToExecute.close();
        }

        boolean reloaded = !isReload;
        int i = 0;
        while (!reloaded) {
            try {
                Thread.sleep(2000);
                if (managementClient.isServerInRunningState()) {
                    reloaded = true;
                    log.info("Server was sucessfully restarted/reloaded");
                }
            } catch (Throwable t) {
                // nothing to do, just waiting
            } finally {
                if (!reloaded && i++ > 20) {
                    throw new RuntimeException("Server reloading failed");
                }
            }
        }
        return reloaded;
    }
    /**
     * There is  problem to restart server with shutdown(restart=true) cli command when controller is used and when the
     * restart should be provided in setup method. The container is not restarted - just stopped.
     * As workaround I need to use this method after deploy operation in @Before method.
     * This is probably workaround for some Arquillian connection issue as well.
     */
    public void stopAndStartServer(final ContainerController controller, final String containerName, final Map<String, String>  config)
            throws IOException, InterruptedException {
        ModelControllerClient controlClient = createInstanceOfRemoteClient();
        try {
            // stop server and ...
            log.info("Restarting server by stopAndStartServer method");
            stop(false);

            log.info("Waiting up to 30 seconds to stop container");
            boolean isStopped = false;
            for(int i = 0; i <= 30; i++) {
                Thread.sleep(1000);
                try {
                    controlClient.execute(new ModelNode());
                } catch (IOException ioe) {
                    // waiting till time when IO exception is got
                    // which means - connection fails and we can expect the server being stopped
                    isStopped = true;
                    break;
                }
            }
            if(!isStopped) {
                log.error("Server was not probably stopped in expected time of 30 seconds!");
            }
        } finally {
            controlClient.close();
        }

        // starting it server again
        log.info("After stopping server let's start container {}", containerName);
        controller.start(containerName, config);
    }

    /**
     * Creating new instance of {@link ManagementOperations} where used client is different
     * one from that which is passed to original object.
     *
     * It's expected that the original client is taken from arquillian and e.g.
     * for restarting server we need different client than that managed by arquillian.
     */
    private ModelControllerClient createInstanceOfRemoteClient() {
        try {
            return ModelControllerClient.Factory.create(
                    InetAddress.getByName(managementClient.getMgmtAddress()), managementClient.getMgmtPort());
        } catch (UnknownHostException uhe) {
            throw new RuntimeException(uhe);
        }
    }

    // ------------------------------------------------------------
    // --------------------- EXECUTION ----------------------------
    // ------------------------------------------------------------
    /**
     * Running operation like :reload etc.
     */
    public void runOp(final String op) throws IOException {
        final ModelNode operation = new ModelNode();
        operation.set(OP).set(op);
        executeOperation(operation);
    }

    /**
     * Executing operation - check the parameters.
     *
     * @param op  operation model node which will be executed
     * @param isUnwrapResult  boolean attribute defines whether the result shoud be unwrapped or not
     *      when it is unwrapped (param == true) then just result part will be returned
     *   when it is wrapped (param == false)  then the whole outcome will be returned
     * @param clientToExecute  what client use for execution
     * @return model node - unwrapped or wrapped
     */
    public ModelNode executeOperation(final ModelNode op, final boolean isUnwrapResult, final ModelControllerClient controllerClient)
            throws IOException {

        if(isBatchMode) {
            // adding to batch
            addToBatch(op);
            log.info("Operation " + op.asString() + " was added to batch (was not executed)");
            ModelNode retCode = new ModelNode();
            retCode.get(OUTCOME).set(SUCCESS);
            retCode.get(RESULT).set("NO EXECUTION! The op was added to batch");
            return retCode;
        } else {
            // operation execution
            ModelNode ret = controllerClient.execute(op);
            if (!isUnwrapResult) return ret;  // do not unwrap the result - return all outcome to me

            if(SUCCESS.equals(ret.get(OUTCOME).asString()) && !isLoggingExecutionSupress) {
                log.info("Succesful management operation {} with result {}", op, ret);
            }

            if (!SUCCESS.equals(ret.get(OUTCOME).asString())) {
                log.error("Management operation {} failed: {}", op, ret);
                throw new RuntimeException("Management operation failed: " + ret.get(FAILURE_DESCRIPTION) + " " + op + " " + ret);
            }

            return ret.get(RESULT);
        }
    }


    /**
     * @param op  op to execute, just RESULT part of outcome (put in model node ) will be returned
     */
    public ModelNode executeOperation(final ModelNode op) throws IOException {
        return executeOperation(op, true, managementClient.getControllerClient());
    }

    public ModelNode executeOperation(final ModelNode op, final boolean isUnwrapResult) throws IOException {
        return executeOperation(op, isUnwrapResult, managementClient.getControllerClient());
    }

    // ----------------------------------------------------------------
    // --------------------------- BATCH ------------------------------
    // ----------------------------------------------------------------
    public void startBatch() {
        if(isBatchMode) {
            throw new RuntimeException("Batch is already running. First cancel the current batch and then create a new one");
        }

        batch = new ModelNode();
        batch.get(OP).set(COMPOSITE);
        batch.get(OP_ADDR).setEmptyList();
        this.isBatchMode = true;
    }

    public void addToBatch(final ModelNode operationToBatch){
        if(!isBatchMode || batch == null) {
            throw new RuntimeException("No active batch. First start a batch then add operations");
        }

        batch.get(STEPS).add(operationToBatch);
    }

    public void runBatch() throws IOException{
        if(!isBatchMode || batch == null) {
            throw new RuntimeException("No active batch. First start a batch then you can run it");
        }

        this.isBatchMode = false;
        executeOperation(batch);
        batch = null;
    }

    public void cancelBatch() {
        this.isBatchMode = false;
        this.batch = null;
    }


    // ------------------------------------------------------------
    // ----------------- ATTRIBUTE HANDLING -----------------------
    // ------------------------------------------------------------
    public ModelNode setAttribute(final ModelNode addr, final String attrName, final String attrValue) throws Exception {
        return setAttribute(addr, attrName, attrValue, false);
    }

    public ModelNode setAttribute(final ModelNode addr, final String attrName, final String attrValue, final boolean isLog) throws Exception {
        ModelNode op = new ModelNode();
        op = new ModelNode();
        op.get(ClientConstants.OP).set(ClientConstants.WRITE_ATTRIBUTE_OPERATION);
        op.get(ClientConstants.OP_ADDR).set(addr);
        op.get(ClientConstants.NAME).set(attrName);
        op.get(ClientConstants.VALUE).set(attrValue);

        if(isLog) {
            log.info("Operation: " + op);
        }

        ModelNode result = executeOperation(op, true);

        if(isLog) {
            String logstring = "";
            if(result.has("outcome")) {
                logstring += "outcome: " + result.get("outcome") + ", ";
            }
            log.info("Operation: " + logstring +  "result: " + result);
        }
        return result;
    }

    public ModelNode add(final String address, final Properties params) throws IOException {
        ModelNode operation = parseAddressToOperation(address);
        operation.get(OP).set(ADD);
        // no attributes -> no params added
        addPropertiesToModelNode(operation, params == null ? new Properties() : params);
        return executeOperation(operation);
    }

    public ModelNode readAttribute(final ModelNode address, final String name) throws IOException {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(INCLUDE_DEFAULTS).set("true");
        operation.get(NAME).set(name);
        return executeOperation(operation);
    }

    public ModelNode readAttribute(final String address, final String name) throws IOException {
        ModelNode addressModelNode = parseAddress(address);
        return readAttribute(addressModelNode, name);
    }

    public boolean readAttributeAsBoolean(final String address, final String name) throws IOException {
        return readAttribute(address, name).asBoolean();
    }
    public boolean readAttributeAsBoolean(final ModelNode address, final String name) throws IOException {
        return readAttribute(address, name).asBoolean();
    }
    public int readAttributeAsInt(final String address, final String name) throws IOException {
        return readAttribute(address, name).asInt();
    }
    public int readAttributeAsInt(final ModelNode address, final String name) throws IOException {
        return readAttribute(address, name).asInt();
    }
    public String readAttributeAsString(final String address, final String name) throws IOException {
        return readAttribute(address, name).asString();
    }
    public String readAttributeAsString(final ModelNode address, final String name) throws IOException {
        return readAttribute(address, name).asString();
    }

    public ModelNode writeAttribute(final String address, final String name, final String value) throws IOException {
        ModelNode addressModelNode = parseAddress(address);
        return writeAttribute(addressModelNode, name, value);
    }

    public ModelNode writeAttribute(final ModelNode address, final String name, final String value) throws IOException {
        ModelNode operation = new ModelNode();
        operation.get(VALUE).set(value);
        return writeAttributeInternal(operation, address, name);
    }

    public ModelNode writeAttribute(final ModelNode address, final String name, final ModelNode value) throws IOException {
        ModelNode operation = new ModelNode();
        operation.get(VALUE).set(value);
        return writeAttributeInternal(operation, address, name);
    }

    private ModelNode writeAttributeInternal(final ModelNode operationWithValueSet, final ModelNode address, final String name) throws IOException {
        operationWithValueSet.get(OP_ADDR).set(address);
        operationWithValueSet.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operationWithValueSet.get(NAME).set(name);
        return executeOperation(operationWithValueSet);
    }

    /**
     * Adding properties as parameters for a model node.
     */
    public ModelNode addPropertiesToModelNode(final ModelNode mn, final Properties properties) {
        Enumeration<?> e = properties.propertyNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            mn.get(name).set(properties.getProperty(name));
        }
        return mn;
    }


    // ------------------------------------------------------------
    // -------------------- IS DEFINED ----------------------------
    // ------------------------------------------------------------
    /**
     * Checking existence of something on the specified address
     * @param address - use like /subsystem=datasources
     * @param checkPath use like [data-source, ExampleDS]
     */
    public boolean isDefined(final String address, final String[] checkPath) throws IOException {
        ModelNode modelNodeAddress = parseAddress(address);
        return isDefined(modelNodeAddress, checkPath);

    }

    public boolean isDefinedNoOutput(final String address, final String[] checkPath) throws IOException {
        boolean previouslySetSupressOption = isLoggingExecutionSupress;
        setLoggingExecutionSupress(true);
        try {
            return isDefined(address, checkPath);
        } finally {
            setLoggingExecutionSupress(previouslySetSupressOption);
        }
    }

    /**
     * Checking existence of something on the specified address
     * @param address - use ModelNode (in the way of /subsystem=datasource)
     * @param checkPath use array String like [data-source, ExampleDS]
     */
    public boolean isDefined(final ModelNode address, final String[] checkPath) throws IOException {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set("read-resource");

        operation.get(INCLUDE_DEFAULTS).set(true);
        // workaround for isDefined would work (BZ#1005131)
        operation.get(RECURSIVE).set(true);

        ModelNode resultCheck = new ModelNode();

        try {
            resultCheck = executeOperation(operation);
        } catch(RuntimeException e) {
            return false;
        }

        if(FAILED.equals(resultCheck.get(OUTCOME).asString())) {
            return false;
        }

        if (!resultCheck.isDefined()) {
            return false;
        }

        for(String checkStr: checkPath) {
            resultCheck = resultCheck.get(checkStr);
            if (!resultCheck.isDefined()) {
                return false;
            }
        }
        return true;
    }


    // ------------------------------------------------------------
    // --------------------- READ INFO ----------------------------
    // ------------------------------------------------------------
    /**
     * Returning result from the address where running read-resource on.
     */
    public ModelNode readResource(final String address) throws IOException {
        ModelNode operation = parseAddressToOperation(address);
        operation.get(OP).set("read-resource");
        operation.get(INCLUDE_DEFAULTS).set(true);
        operation.get(RECURSIVE).set(true);

        ModelNode resultCheck = executeOperation(operation, false);

        if(FAILED.equals(resultCheck.get(OUTCOME).asString())) {
            throw new RuntimeException("Outcome for operation" + operation + " result failed with" + resultCheck);
        }

        return resultCheck.get(RESULT);
    }

    /**
     * Checking is result failed or not from the address where running read-resource to check the existence of resource.
     */
    private boolean isResourceExists(final String address) throws IOException {
        ModelNode operation = parseAddressToOperation(address);
        operation.get(OP).set("read-resource");
        operation.get(INCLUDE_DEFAULTS).set(true);
        operation.get(RECURSIVE).set(true);

        ModelNode resultCheck = executeOperation(operation, false);

        return SUCCESS.equals(resultCheck.get(OUTCOME).asString());
    }


    // ------------------------------------------------------------
    // -------------------- SYSTEM PROPERTIES ---------------------
    // ------------------------------------------------------------
    public void removeSystemProperty(final String name) throws Exception {
        final ModelNode operation = parseAddressToOperation("/system-property=" + name);
        operation.get(OP).set(REMOVE);
        executeOperation(operation, false);
    }

    public void addSystemProperty(final String name, final String value) throws Exception {
        final ModelNode operation = parseAddressToOperation("/system-property=" + name);
        operation.get(OP).set(ADD);
        operation.get("value").set(value);
        executeOperation(operation);
    }


    // -----------------------------------------------------
    // -------------------- HELPERS ------------------------
    // -----------------------------------------------------
    /**
     * Adding string property - name=value - to existing Properties instance.
     * This method does not override existing property when name exist.
     * @throws RuntimeException  when props are null
     */
    @SuppressWarnings("unused")
    private void putNotOverride(final Properties props, final String name, final String value) {
        if(props == null) {
            throw new RuntimeException("Value props can't be null");
        }
        if(props.getProperty(name) == null) {
            props.setProperty(name, value);
        }
    }

    /**
     * Simple helper method which operates over properties baseProperties. It will change them!
     * It takes toOverride properties and merge them into the properties baseProperties.
     *
     * @param baseProperties  base properties which will be enhance by toOveriride ones
     * @param toOverride  properties which will be merge into baseProperties
     */
    private void mergeProperties(final Properties baseProperties, final Properties toOverride) {
        if(baseProperties == null) {
            log.warn("baseProperties were null - skipping this method");
            return;
        }
        if(toOverride == null) {
            log.debug("toOverride properties were null - no properties will be added to baseProperties");
            return;
        }

        for(Entry<Object, Object> entry: toOverride.entrySet()) {
            baseProperties.put(entry.getKey(), entry.getValue());
        }
    }
}
