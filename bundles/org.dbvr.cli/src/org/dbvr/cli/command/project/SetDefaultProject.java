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

import org.dbvr.cli.app.CLIWorkspace;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.cli.CLIUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import picocli.CommandLine;

@CommandLine.Command(name = "default", description = "Set default project")
public class SetDefaultProject extends AbstractProjectCommand {

    @CommandLine.Parameters(index = "0", description = "Project ID or name", arity = "1")
    private String projectIdOrName;

    @Override
    public void run() throws CLIException {
        try {
            DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();
            DBPProject project = CLIUtils.findProject(projectIdOrName, context());

            setDefaultProject(workspace, project);
            context().addResult("Project '" + project.getName() + "' set as default.");
        } catch (CLIException e) {
            throw e;
        } catch (Exception e) {
            throw new CLIException("Error setting default project: " + e.getMessage(), e, CLIConstants.EXIT_CODE_ERROR);
        }
    }

    private void setDefaultProject(
        @NotNull DBPWorkspace workspace,
        @NotNull DBPProject project
    ) {
        if (workspace instanceof CLIWorkspace cliWorkspace) {
            cliWorkspace.setActiveProject(project);
        }
    }
}
