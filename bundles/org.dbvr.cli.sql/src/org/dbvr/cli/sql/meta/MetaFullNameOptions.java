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
package org.dbvr.cli.sql.meta;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.List;

public class MetaFullNameOptions {

    @Nullable
    @CommandLine.Option(
        names = {"--full-name", "-fn"},
        description = "Fully qualified name (e.g. database.schema.table)"
    )
    protected String fullName;

    public record Resolved(
        boolean fromFullName,
        @NotNull List<String> containerPath,
        @Nullable String databaseName,
        @Nullable String schemaName,
        @Nullable String objectName
    ) {
        @NotNull
        public static Resolved fromExplicit(
            @Nullable String databaseName,
            @Nullable String schemaName,
            @Nullable String objectName
        ) {
            return new Resolved(false, List.of(), databaseName, schemaName, objectName);
        }

        @NotNull
        public static Resolved fromTokens(@NotNull String[] tokens, boolean hasObject) {
            if (!hasObject) {
                return new Resolved(true, List.of(tokens), null, null, null);
            }
            // (table ddl -fn=db.schema.table → path=[db, schema], object=table).
            String objectName = tokens[tokens.length - 1];
            List<String> path = List.of(Arrays.copyOfRange(tokens, 0, tokens.length - 1));
            return new Resolved(true, path, null, null, objectName);
        }
    }

    public boolean isSpecified() {
        return CommonUtils.isNotEmpty(fullName);
    }

    @NotNull
    public Resolved resolve(
        @NotNull DBPDataSource dataSource,
        @Nullable String explicitDatabase,
        @Nullable String explicitSchema,
        @Nullable String explicitObject,
        int maxContainerDepth,
        boolean hasObject
    ) throws CLIException {
        if (!isSpecified()) {
            return Resolved.fromExplicit(explicitDatabase, explicitSchema, explicitObject);
        }
        rejectIfAnyExplicit(explicitDatabase, explicitSchema, explicitObject);
        String[][] quoteStrings = dataSource.getSQLDialect().getIdentifierQuoteStrings();
        return Resolved.fromTokens(splitAndValidate(maxContainerDepth, hasObject, quoteStrings), hasObject);
    }

    private static void rejectIfAnyExplicit(
        @Nullable String database,
        @Nullable String schema,
        @Nullable String object
    ) throws CLIException {
        if (CommonUtils.isNotEmpty(database) || CommonUtils.isNotEmpty(schema) || CommonUtils.isNotEmpty(object)) {
            throw new CLIException(
                "--full-name cannot be combined with --database-name, --schema-name or the object name option",
                CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS
            );
        }
    }

    @NotNull
    private String[] splitAndValidate(
        int maxContainerDepth,
        boolean hasObject,
        @Nullable String[][] quoteStrings
    ) throws CLIException {
        String trimmed = fullName == null ? "" : fullName.trim();
        if (trimmed.isEmpty()) {
            throw new CLIException("Fully qualified name is empty", CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
        }
        String[] tokens = SQLUtils.splitFullIdentifier(trimmed, ".", quoteStrings, false);
        for (String token : tokens) {
            if (token.isEmpty()) {
                throw new CLIException("Invalid fully qualified name: '" + fullName + "'",
                    CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
            }
        }
        int maxParts = maxContainerDepth + (hasObject ? 1 : 0);
        if (tokens.length > maxParts) {
            throw new CLIException(
                "Fully qualified name '" + fullName + "' has " + tokens.length
                    + " parts but at most " + maxParts + " is allowed for this command",
                CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS
            );
        }
        return tokens;
    }
}
