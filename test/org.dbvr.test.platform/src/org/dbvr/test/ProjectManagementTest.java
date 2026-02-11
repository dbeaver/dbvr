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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.impl.app.BaseProjectImpl;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProjectManagementTest extends DBVRTest {

    private static final Log log = Log.getLog(ProjectManagementTest.class);

    private final List<DBPProject> projectsToDelete = new ArrayList<>();

    @After
    public void tearDown() {
        for (DBPProject project : projectsToDelete) {
            try {
                if (DBWorkbench.getPlatform().getWorkspace().getProjectById(project.getId()) != null) {
                    DBWorkbench.getPlatform().getWorkspace().deleteProject(project);
                }
            } catch (Exception e) {
                log.error("Error deleting test project: " + e.getMessage());
            }
        }
        projectsToDelete.clear();
    }

    @Test
    public void testCreateProject() throws Exception {
        String name = "test_prj_" + UUID.randomUUID();
        String desc = "Test description";
        String[] args = {
            "project", "create", "-n", name, "-d", desc
        };

        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        cmd.executeCommandLineCommands(null, false, false, args);

        DBPProject project = DBWorkbench.getPlatform().getWorkspace().getProject(name);
        Assert.assertNotNull(project);
        projectsToDelete.add(project);
        Assert.assertEquals(desc, project.getDescription());
    }

    @Test
    public void testRenameProject() throws Exception {
        String name = "test_prj_" + UUID.randomUUID();
        DBPProject project = DBWorkbench.getPlatform().getWorkspace().createProject(name, null);
        projectsToDelete.add(project);

        String newName = name + "_renamed";
        String newDesc = "New description";
        String[] args = {
            "project", "rename", project.getId(), newName, "-d", newDesc
        };

        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        cmd.executeCommandLineCommands(null, false, false, args);

        DBPProject renamedProject = DBWorkbench.getPlatform().getWorkspace().getProject(newName);
        Assert.assertNotNull(renamedProject);
        if (renamedProject != project) {
            projectsToDelete.add(renamedProject);
        }
        Assert.assertEquals(newDesc, renamedProject.getDescription());
    }

    @Test
    public void testDeleteProject() throws Exception {
        String name = "test_prj_" + UUID.randomUUID();
        DBPProject project = DBWorkbench.getPlatform().getWorkspace().createProject(name, null);
        projectsToDelete.add(project);

        String[] args = {
            "project", "delete", project.getId()
        };

        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        cmd.executeCommandLineCommands(null, false, false, args);

        Assert.assertNull(DBWorkbench.getPlatform().getWorkspace().getProject(name));
    }

    @Test
    public void testListProjects() throws Exception {
        String name = "test_prj_" + UUID.randomUUID();
        DBPProject project = DBWorkbench.getPlatform().getWorkspace().createProject(name, "List test");
        projectsToDelete.add(project);

        String[] args = {
            "project", "list"
        };

        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);

        Assert.assertNotNull(result.getOutput());
        boolean found = false;
        String allOutput = String.join("\n", result.getOutput());
        if (allOutput.contains(name)) {
            found = true;
        }
        Assert.assertTrue("Project " + name + " not found in output: " + result.getOutput(), found);
    }

    @Test
    public void testListProjectsNoDescription() throws Exception {
        String name = "test_prj_" + UUID.randomUUID();
        DBPProject project = DBWorkbench.getPlatform().getWorkspace().createProject(name, null);
        projectsToDelete.add(project);

        String[] args = {
            "project", "list"
        };

        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);

        Assert.assertNotNull(result.getOutput());
        boolean found = false;
        String allOutput = String.join("\n", result.getOutput());
        if (allOutput.contains(name)) {
            found = true;
        }
        Assert.assertTrue("Project " + name + " (no desc) not found in output: " + result.getOutput(), found);
    }

    @Test
    public void testCreateHiddenProjectForbidden() throws Exception {
        String name = ".test_prj_hidden";
        String[] args = {
            "project", "create", "-n", name
        };

        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);

        Assert.assertTrue("Error message expected for hidden project creation",
            String.join("\n", result.getOutput()).contains("Resource name '.test_prj_hidden' can't start with dot"));
    }

    @Test
    public void testProjectFileCreation() throws Exception {
        String name = "test_prj_file_" + UUID.randomUUID();
        DBPProject project = DBWorkbench.getPlatform().getWorkspace().createProject(name, null);
        projectsToDelete.add(project);

        Path projectFile = project.getAbsolutePath().resolve(BaseProjectImpl.PROJECT_FILE);
        Assert.assertTrue("Project file must exist", Files.exists(projectFile));
        String content = Files.readString(projectFile);
        Assert.assertTrue("Project file must contain project name", content.contains("<name>" + name + "</name>"));
        Assert.assertTrue("Project file must contain DBeaver nature", content.contains("org.jkiss.dbeaver.DBeaverNature"));

        // Test rename
        String newName = name + "_ren";
        DBWorkbench.getPlatform().getWorkspace().renameProject(project, newName);
        Path newProjectFile = project.getAbsolutePath().resolve(BaseProjectImpl.PROJECT_FILE);
        Assert.assertTrue("Project file must exist after rename", Files.exists(newProjectFile));
        String newContent = Files.readString(newProjectFile);
        Assert.assertTrue("Project file must contain new project name", newContent.contains("<name>" + newName + "</name>"));
    }
}
