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
import org.jkiss.dbeaver.model.cli.help.CLIExample;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import picocli.CommandLine;

@CommandLine.Command(name = CreateProject.COMMAND_CREATE, description = "Create new project")
@CLIExample(examples = {
    CreateProject.EXAMPLE
})
public class CreateProject extends AbstractProjectCommand {
    public static final String COMMAND_CREATE = "create";
    public static final String OPTION_NAME = "--name";
    public static final String OPTION_DESCRIPTION = "--description";

    static final String EXAMPLE = ProjectManagementCommand.COMMAND_PROJECT + " " + COMMAND_CREATE
        + " " + OPTION_NAME + " MyProject "
        + " " + OPTION_DESCRIPTION + " \"This is my project\"";

    @CommandLine.Option(names = {"-n", OPTION_NAME}, description = "Project name", required = true)
    private String name;

    @CommandLine.Option(names = {"-d", OPTION_DESCRIPTION}, description = "Project description")
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
