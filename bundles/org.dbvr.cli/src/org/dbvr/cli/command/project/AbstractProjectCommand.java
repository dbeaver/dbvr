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
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.cli.AbstractCommandLineParameterHandler;
import org.jkiss.dbeaver.model.cli.CLIContext;
import org.jkiss.dbeaver.model.cli.CLIUtils;
import org.jkiss.dbeaver.model.impl.app.BaseProjectImpl;
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
    protected String serializeProjectList() {
        List<Map<String, String>> projectData = new ArrayList<>();
        for (DBPProject project : DBWorkbench.getPlatform().getWorkspace().getProjects()) {
            String name = project.getName();
            if (BaseProjectImpl.isHiddenProjectName(name)) {
                continue;
            }
            projectData.add(collectProjectData(project.getId(), name, project.getDescription()));
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

    @NotNull
    protected CLIContext context() {
        return parent.context();
    }
}
