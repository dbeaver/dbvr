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
package org.dbvr.cli.app;

import org.jkiss.code.NotNull;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;

import java.util.Arrays;

/**
 * Adds a lowercase {@code -v} alias to the existing {@code -V}/{@code --version} option
 * instead of declaring a competing {@code versionHelp} option.
 */
public class VersionAliasTransformer implements CommandLine.IModelTransformer {
    private static final String VERSION_ALIAS = "-v";

    @NotNull
    @Override
    public CommandSpec transform(@NotNull CommandSpec commandSpec) {
        OptionSpec versionOption = commandSpec.findOption("--version");
        if (versionOption == null || Arrays.asList(versionOption.names()).contains(VERSION_ALIAS)) {
            return commandSpec;
        }
        String[] names = Arrays.copyOf(versionOption.names(), versionOption.names().length + 1);
        names[names.length - 1] = VERSION_ALIAS;
        commandSpec.remove(versionOption);
        // Subcommands (at every depth) added before this transformer runs already hold their own
        // inherited copy of versionOption (picocli pushes INHERIT-scoped options down the whole
        // subcommand tree on addSubcommand()). Remove those stale copies too, otherwise addOption()
        // below fails when it re-cascades the replacement option through the same tree.
        removeStaleVersionCopies(commandSpec);
        commandSpec.addOption(OptionSpec.builder(versionOption).names(names).build());
        return commandSpec;
    }

    private void removeStaleVersionCopies(@NotNull CommandSpec commandSpec) {
        for (CommandLine subcommand : commandSpec.subcommands().values()) {
            CommandSpec subSpec = subcommand.getCommandSpec();
            OptionSpec staleCopy = subSpec.findOption("--version");
            if (staleCopy != null) {
                subSpec.remove(staleCopy);
            }
            removeStaleVersionCopies(subSpec);
        }
    }
}
