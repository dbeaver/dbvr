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
import picocli.CommandLine;

@CommandLine.Command(name = "list", description = "List all projects")
public class ListProjects extends AbstractProjectCommand {

    @Override
    public void run() throws CLIException {
        try {
            context().addResult(serializeProjectList());
        } catch (Exception e) {
            throw new CLIException("Error listing projects: " + e.getMessage(), e, CLIConstants.EXIT_CODE_ERROR);
        }
    }
}
