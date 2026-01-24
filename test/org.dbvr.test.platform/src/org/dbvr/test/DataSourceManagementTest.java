/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dbvr.test;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class DataSourceManagementTest extends DBVRTest {

    @Test
    public void testCreate() throws Exception {
        String uniqName = "test_create" + UUID.randomUUID();
        var args = new String[] {
            "datasource", "create",
            "--driver=h2_embedded_v2",
            "--database=cloudbeaver",
            "--host=localhost",
            "--name=" + uniqName,
            "-u", "postgres",
            "-p", "postgres",
            "-net", "ssh.host=test_host",
            "-net", "ssh.authType=PUBLIC_KEY",
            "-net", "ssh.user=test_user",
            "-net", "ssh.keyPath=/opt/test/path",
            "-net", "ssh.password=dsdas123"
        };

        var cmd = DBVRTestSuite.getApplication().createCommandLine();

        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);
        Assert.assertNotNull(result.getOutput());
        Assert.assertEquals(1, result.getOutput().size());
        if (result.getExitCode() == CLIConstants.EXIT_CODE_ERROR) {
            Assert.fail("Error during datasource creation: " + String.join("\n", result.getOutput()));
        }
        Assert.assertTrue(result.getOutput().get(0).contains(uniqName));

        DBPProject project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        Assert.assertNotNull(project);
        DBPDataSourceContainer ds = project.getDataSourceRegistry().findDataSourceByName(uniqName);
        Assert.assertNotNull(ds);
        DBWHandlerConfiguration sshConf = ds.getConnectionConfiguration().getHandler(DBWUtils.SSH_TUNNEL);
        Assert.assertNotNull(sshConf);
        Assert.assertEquals("test_host", sshConf.getProperty("host"));
        Assert.assertEquals("/opt/test/path", sshConf.getProperty("keyPath"));
        Assert.assertEquals("test_user", sshConf.getUserName());
        Assert.assertEquals("dsdas123", sshConf.getPassword());
        Assert.assertEquals("PUBLIC_KEY", sshConf.getProperty("authType"));
        project.getDataSourceRegistry().removeDataSource(ds);
    }


    @Test
    public void testDelete() throws Exception {
        String uniqName = "test_delete" + UUID.randomUUID();
        createFakeDataSource(uniqName);
        var registry = DBWorkbench.getPlatform().getWorkspace().getActiveProject()
            .getDataSourceRegistry();
        Assert.assertNotNull(registry.findDataSourceByName(uniqName));
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var args = new String[] {
            "datasource",
            "delete", uniqName
        };
        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);
        Assert.assertNotNull(result.getOutput());
        Assert.assertEquals(1, result.getOutput().size());
        Assert.assertTrue(result.getOutput().get(0).contains(uniqName));
        Assert.assertNull(registry.findDataSourceByName(uniqName));
    }

    public void testUpdate() throws Exception {
        String uniqName = "test_update" + UUID.randomUUID();

        DBPDataSourceContainer ds = createFakeDataSource(uniqName);
        var registry = DBWorkbench.getPlatform().getWorkspace().getActiveProject()
            .getDataSourceRegistry();
        Assert.assertNotNull(registry.findDataSourceByName(uniqName));
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        String newRandomHost = "host" + UUID.randomUUID();
        Assert.assertNotEquals(newRandomHost, ds.getConnectionConfiguration().getHostName());
        var args = new String[] {
            "datasource",
            "update",
            "--host=" + newRandomHost,
        };
        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);

        ds = registry.findDataSourceByName(uniqName);

        Assert.assertNotNull(ds);
        Assert.assertEquals(newRandomHost, ds.getConnectionConfiguration().getHostName());

        Assert.assertNotNull(result.getOutput());
        Assert.assertEquals(1, result.getOutput().size());
        Assert.assertTrue(result.getOutput().get(0).contains(uniqName));

        registry.removeDataSource(ds);
    }

    @NotNull
    private static DBPDataSourceContainer createFakeDataSource(@NotNull String uniqName) throws DBException {
        DBPDriver driver = DBWorkbench.getPlatform().getDataSourceProviderRegistry()
            .findDriver("h2_embedded_v2");
        var connectionConfiguration = new DBPConnectionConfiguration();
        connectionConfiguration.setUrl(UUID.randomUUID().toString());
        connectionConfiguration.setHostName(UUID.randomUUID().toString());
        connectionConfiguration.setHostPort("123");
        connectionConfiguration.setServerName("test_delete");
        connectionConfiguration.setDatabaseName("test_delete");
        var registry = DBWorkbench.getPlatform().getWorkspace().getActiveProject()
            .getDataSourceRegistry();

        var ds = registry.createDataSource(driver, connectionConfiguration);
        ds.setName(uniqName);
        registry.addDataSource(ds);
        return ds;
    }
}
