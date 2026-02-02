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
package org.dbvr.cli.command.project;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.cli.AbstractCommandLineParameterHandler;
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.cli.CLIUtils;
import org.jkiss.dbeaver.model.cli.CommandLineContext;
import org.jkiss.dbeaver.model.rm.RMController;
import org.jkiss.dbeaver.model.rm.RMControllerProvider;
import org.jkiss.dbeaver.model.rm.RMProject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractProjectCommand extends AbstractCommandLineParameterHandler {

    @CommandLine.ParentCommand
    private ProjectManagementHandler parent;

    @NotNull
    @Override
    protected CommandLineContext context() {
        return parent.context();
    }

    @Nullable
    protected RMController getRMController(@NotNull DBPProject project) throws CLIException {
        RMControllerProvider rmControllerProvider = DBUtils.getAdapter(RMControllerProvider.class, project.getWorkspace());
        if (rmControllerProvider == null) {
            rmControllerProvider = DBUtils.getAdapter(RMControllerProvider.class, project);
        }
        if (rmControllerProvider == null) {
            return null;
        }
        return rmControllerProvider.getResourceController();
    }

    @NotNull
    protected String serializeProjectList() throws DBException {
        RMController rmController = null;
        DBPProject activeProject = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        if (activeProject != null) {
            rmController = getRMController(activeProject);
        }

        List<Map<String, String>> projectData = new ArrayList<>();

        if (rmController != null) {
            RMProject[] projects = rmController.listAccessibleProjects();
            for (RMProject project : projects) {
                projectData.add(collectProjectData(project.getId(), project.getName(), project.getDescription()));
            }
        } else {
            for (DBPProject project : DBWorkbench.getPlatform().getWorkspace().getProjects()) {
                projectData.add(collectProjectData(project.getId(), project.getName(), project.getDescription()));
            }
        }
        return CLIUtils.formatAsTable(projectData);
    }

    private Map<String, String> collectProjectData(String id, String name, String description) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("ID", id);
        row.put("NAME", name);
        row.put("DESCRIPTION", CommonUtils.notNull(description, ""));
        return row;
    }
}
