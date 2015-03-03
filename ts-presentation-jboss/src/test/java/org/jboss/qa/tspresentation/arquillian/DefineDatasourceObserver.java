package org.jboss.qa.tspresentation.arquillian;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.event.container.AfterStart;
import org.jboss.arquillian.container.spi.event.container.BeforeStart;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.qa.tspresentation.utils.FileLoader;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefineDatasourceObserver {
    private static final Logger log = LoggerFactory.getLogger(DefineDatasourceObserver.class);

    @Inject
    private Instance<ManagementClient> managementClient;

    private static final String JDBC_DRIVER_NAME = "database-jdbc-driver.jar";

    public synchronized void handleBeforeSetup(@Observes final BeforeStart event, final Container container) throws Exception {
        log.debug("handleBeforeSetup for container " + container.getName());

        // This handling probably needs to have configured jbossHome in arquillian.xml otherwise this won't work
        // it should be better way how to do this - somelike get class org.jboss.as.arquillian.container.DistributionContainter probably
        String jbossHome = container.getContainerConfiguration().getContainerProperties().get("jbossHome");
        File jbossDeploymentsDirectory = new File(jbossHome, "standalone" + File.separator + "deployments");
        File jdbcDriverTargetJarFile = new File(jbossDeploymentsDirectory, JDBC_DRIVER_NAME);
        File jdbcDriverJarFile = FileLoader.getFile(ProjectProperties.get(ProjectProperties.JDBC_DRIVER_FILEPATH));

        Files.copy(jdbcDriverJarFile.toPath(), jdbcDriverTargetJarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public void handleAfterStart(@Observes final AfterStart event, final Container container) throws Exception {
        log.debug("handleAfterStart for container " + container.getName());
        ManagementOperations operations = new ManagementOperations(managementClient.get());

        boolean wasChangeDone = false;

        // if not exist - then create the datasource
        if(!operations.isDefined("/subsystem=datasources", new String[] {"data-source", ProjectProperties.NON_XA_DATASOURCE})) {
            Properties connectionProperties = new Properties();
            connectionProperties.setProperty("user-name", ProjectProperties.get(ProjectProperties.DB_USERNAME));
            connectionProperties.setProperty("password", ProjectProperties.get(ProjectProperties.DB_PASSWORD));
            connectionProperties.setProperty("spy", "true");

            operations.addDatasource(ProjectProperties.NON_XA_DATASOURCE, ProjectProperties.NON_XA_DATASOURCE_JNDI,
                    ProjectProperties.get(ProjectProperties.DB_URL),
                    JDBC_DRIVER_NAME,
                    new Properties(),
                    connectionProperties);
            wasChangeDone = true;
        }

        // if not exist - then create the xa-datasource
        if(!operations.isDefined("/subsystem=datasources", new String[] {"xa-data-source", ProjectProperties.XA_DATASOURCE})) {

            Properties connectionProperties = new Properties();
            connectionProperties.setProperty("user-name", ProjectProperties.get(ProjectProperties.DB_USERNAME));
            connectionProperties.setProperty("password", ProjectProperties.get(ProjectProperties.DB_PASSWORD));
            connectionProperties.setProperty("spy", "true");

            DatabaseType dbType = DatabaseType.convert(ProjectProperties.get(ProjectProperties.DB_URL));
            String jdbcUrl = ProjectProperties.get(ProjectProperties.DB_URL);
            String dbHost = DatasourceSetupUtil.getHost(dbType, jdbcUrl);
            int dbPort = DatasourceSetupUtil.getPort(dbType, jdbcUrl);
            String dbName = DatasourceSetupUtil.getDatabaseName(dbType, jdbcUrl);

            Properties xaDatasourceProperties = operations.prepareXADatasourceProperties(
                    DatabaseType.convert(ProjectProperties.get(ProjectProperties.DB_URL)),
                    jdbcUrl, dbHost, dbPort, dbName);

            operations.addXADataSource(
                    ProjectProperties.XA_DATASOURCE,
                    ProjectProperties.XA_DATASOURCE_JNDI,
                    dbType,
                    ProjectProperties.get(ProjectProperties.DB_JDBC_XA_CLASS),
                    JDBC_DRIVER_NAME,
                    connectionProperties,
                    xaDatasourceProperties);
            wasChangeDone = true;
        }

        if(wasChangeDone) {
            operations.reload();
        }
    }

}
