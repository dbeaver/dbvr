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

import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import picocli.CommandLine;

@CommandLine.Command(name = "create", description = "Create new project")
public class CreateProject extends AbstractProjectCommand {

    @CommandLine.Option(names = {"-n", "--name"}, description = "Project name", required = true)
    private String name;

    @CommandLine.Option(names = {"-d", "--description"}, description = "Project description")
    private String description;

    @Override
    public void run() throws CLIException {
        try {
            DBWorkbench.getPlatform().getWorkspace().createProject(name, description);
            context().addResult("Project '" + name + "' created.");
        } catch (Exception e) {
            throw new CLIException("Error creating project: " + e.getMessage(), e, CLIConstants.EXIT_CODE_ERROR);
        }
    }
}
