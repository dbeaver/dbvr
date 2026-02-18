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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerDescriptor;
import org.jkiss.dbeaver.model.net.DBWUtils;
import org.jkiss.dbeaver.registry.network.NetworkHandlerRegistry;
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
            "-p", "postgres"
        };

        var cmd = DBVRTestSuite.getApplication().createCommandLine();

        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);
        Assert.assertNotNull(result.getOutput());
        Assert.assertEquals(1, result.getOutput().size());
        String createdId = result.getOutput().getFirst();

        DBPProject project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        Assert.assertNotNull(project);
        DBPDataSourceContainer ds = project.getDataSourceRegistry().getDataSource(createdId);
        Assert.assertNotNull(ds);
        Assert.assertEquals(uniqName, ds.getName());
        project.getDataSourceRegistry().removeDataSource(ds);
    }

    @Test
    public void testCreateWithSSH() throws Exception {
        String uniqName = "test_create_ssh" + UUID.randomUUID();
        String uniqPwd = "test_ssh_pwd" + UUID.randomUUID();
        String uniqUser = "test_ssh_username" + UUID.randomUUID();
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
            "-net", "ssh.user=" + uniqUser,
            "-net", "ssh.keyPath=/opt/test/path",
            "-net", "ssh.password=" + uniqPwd
        };

        var cmd = DBVRTestSuite.getApplication().createCommandLine();

        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);
        Assert.assertNotNull(result.getOutput());
        Assert.assertEquals(1, result.getOutput().size());
        if (result.getExitCode() == CLIConstants.EXIT_CODE_ERROR) {
            Assert.fail("Error during datasource creation: " + String.join("\n", result.getOutput()));
        }
        String createdId = result.getOutput().get(0);

        DBPProject project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        Assert.assertNotNull(project);
        DBPDataSourceContainer ds = project.getDataSourceRegistry().getDataSource(createdId);
        Assert.assertNotNull(ds);
        Assert.assertEquals(uniqName, ds.getName());
        DBWHandlerConfiguration sshConf = ds.getConnectionConfiguration().getHandler(DBWUtils.SSH_TUNNEL);
        Assert.assertNotNull(sshConf);
        Assert.assertEquals("test_host", sshConf.getProperty("host"));
        Assert.assertEquals("/opt/test/path", sshConf.getProperty("keyPath"));
        Assert.assertEquals(uniqUser, sshConf.getUserName());
        Assert.assertEquals(uniqPwd, sshConf.getPassword());
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

    @Test
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
            "update", ds.getId(),
            "--host=" + newRandomHost,
        };
        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);

        ds = registry.findDataSourceByName(uniqName);

        Assert.assertNotNull(ds);
        Assert.assertEquals(newRandomHost, ds.getConnectionConfiguration().getHostName());

        Assert.assertNotNull(result.getOutput());
        Assert.assertEquals(1, result.getOutput().size());
        String output = result.getOutput().getFirst();
        Assert.assertTrue(output.contains(newRandomHost));
        Assert.assertTrue(output.contains(uniqName));

        registry.removeDataSource(ds);
    }

    @Test
    public void testView() throws Exception {
        String uniqName = "test_view" + UUID.randomUUID();
        String uniqUser = "user" + UUID.randomUUID();
        String uniqPwd = "password" + UUID.randomUUID();
        DBPDataSourceContainer ds = createFakeDataSource(uniqName, uniqUser, uniqPwd);
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var args = new String[] {
            "datasource", "view", ds.getId()
        };
        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);
        Assert.assertNotNull(result.getOutput());
        Assert.assertEquals(uniqUser, ds.getConnectionConfiguration().getUserName());
        Assert.assertEquals(uniqPwd, ds.getConnectionConfiguration().getUserPassword());
        Assert.assertEquals(1, result.getOutput().size());

        String output = result.getOutput().getFirst();
        Assert.assertTrue(output.contains(uniqName));
        Assert.assertFalse(output.contains(uniqUser));
        Assert.assertFalse(output.contains(uniqPwd));

        var sshConf = ds.getConnectionConfiguration().getHandler(DBWUtils.SSH_TUNNEL);
        Assert.assertNotNull(sshConf);
        Assert.assertNotNull(sshConf.getPassword());
        Assert.assertNotNull(sshConf.getUserName());

        Assert.assertFalse(output.contains(sshConf.getPassword()));
        Assert.assertFalse(output.contains(sshConf.getUserName()));
        Assert.assertFalse(output.contains("turbo_secure_prop"));

        Assert.assertNotNull(sshConf.getSecureProperty("turbo_secure_prop"));
        Assert.assertFalse(output.contains(sshConf.getSecureProperty("turbo_secure_prop")));


        var registry = DBWorkbench.getPlatform().getWorkspace().getActiveProject()
            .getDataSourceRegistry();
        registry.removeDataSource(ds);
    }

    @Test
    public void testList() throws Exception {
        String uniqName = "test_list" + UUID.randomUUID();
        DBPDataSourceContainer ds = createFakeDataSource(uniqName);
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var args = new String[] {
            "datasource", "list"
        };
        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);
        Assert.assertNotNull(result.getOutput());
        Assert.assertEquals(1, result.getOutput().size());
        String output = result.getOutput().getFirst();
        Assert.assertTrue(output.contains("ID"));
        Assert.assertTrue(output.contains("NAME"));
        Assert.assertTrue(output.contains("DRIVER"));
        Assert.assertTrue(output.contains(ds.getId()));
        Assert.assertTrue(output.contains(uniqName));

        var registry = DBWorkbench.getPlatform().getWorkspace().getActiveProject()
            .getDataSourceRegistry();
        registry.removeDataSource(ds);
    }

    @Test
    public void testHelpWhenWhenCommandWithNoParams() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var args = new String[] {
            "datasource"
        };
        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);
        Assert.assertNotNull(result.getOutput());
        Assert.assertFalse(result.getOutput().isEmpty());
        String output = result.getOutput().getFirst();
        Assert.assertTrue(output.contains("Usage: dbvr datasource"));
        Assert.assertTrue(output.contains("Commands:"));
        Assert.assertTrue(output.contains("create"));
        Assert.assertTrue(output.contains("list"));
    }

    @NotNull
    private static DBPDataSourceContainer createFakeDataSource(
        @NotNull String uniqName
    ) throws DBException {
        return createFakeDataSource(uniqName, null, null);
    }

    @NotNull
    private static DBPDataSourceContainer createFakeDataSource(
        @NotNull String uniqName,
        @Nullable String username,
        @Nullable String password
    ) throws DBException {
        DBPDriver driver = DBWorkbench.getPlatform().getDataSourceProviderRegistry()
            .findDriver("h2_embedded_v2");
        var connectionConfiguration = new DBPConnectionConfiguration();
        connectionConfiguration.setUrl(UUID.randomUUID().toString());
        connectionConfiguration.setHostName(UUID.randomUUID().toString());
        connectionConfiguration.setHostPort("123");
        connectionConfiguration.setServerName("test_delete");
        connectionConfiguration.setDatabaseName("test_delete");
        connectionConfiguration.setDatabaseName("test_delete");
        if (username != null) {
            connectionConfiguration.setUserName(username);
        }
        if (password != null) {
            connectionConfiguration.setUserPassword(password);
        }

        var registry = DBWorkbench.getPlatform().getWorkspace().getActiveProject()
            .getDataSourceRegistry();

        var ds = registry.createDataSource(driver, connectionConfiguration);
        ds.setName(uniqName);
        DBWHandlerDescriptor sshDesc = NetworkHandlerRegistry.getInstance().getDescriptor(DBWUtils.SSH_TUNNEL);
        DBWHandlerConfiguration sshConfig = new DBWHandlerConfiguration(sshDesc, ds);
        sshConfig.setSavePassword(true);
        sshConfig.setUserName("ssh_user " + UUID.randomUUID());
        sshConfig.setPassword("ssh_pwd " + UUID.randomUUID());
        sshConfig.setSecureProperty("turbo_secure_prop", "secure_value" + UUID.randomUUID());
        connectionConfiguration.updateHandler(sshConfig);
        registry.addDataSource(ds);
        return ds;
    }
}
