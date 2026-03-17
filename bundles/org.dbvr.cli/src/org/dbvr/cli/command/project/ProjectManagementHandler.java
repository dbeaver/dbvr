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

import org.jkiss.dbeaver.model.cli.CLIAbstractSubcommand;
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

@CommandLine.Command(
    name = "project",
    description = "Project management commands",
    subcommands = {
        ListProjects.class,
        CreateProject.class,
        DeleteProject.class,
        RenameProject.class,
        SetDefaultProject.class
    }
)
public class ProjectManagementHandler extends CLIAbstractSubcommand {
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() throws CLIException {
        if (spec.commandLine().getParseResult().subcommand() == null) {
            StringWriter writer = new StringWriter();
            spec.commandLine().usage(new PrintWriter(writer));
            context().addResult(writer.toString());
            context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        }
    }
}
