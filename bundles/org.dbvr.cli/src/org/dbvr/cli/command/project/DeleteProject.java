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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.cli.CLIUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import picocli.CommandLine;

@CommandLine.Command(name = "delete", description = "Delete project")
public class DeleteProject extends AbstractProjectCommand {

    @CommandLine.Parameters(index = "0", description = "Project ID or name")
    private String projectId;

    @Override
    public void run() throws CLIException {
        try {
            DBPProject project = CLIUtils.findProject(projectId, context());
            DBWorkbench.getPlatform().getWorkspace().deleteProject(project);
            context().addResult("Project '" + projectId + "' deleted.");
        } catch (DBException e) {
            throw new CLIException("Error deleting project: " + e.getMessage(), e, CLIConstants.EXIT_CODE_ERROR);
        }
    }
}
