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
package org.dbvr.cli.sql;


import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.cli.*;
import org.jkiss.dbeaver.model.cli.model.DataSourceUpdater;
import org.jkiss.dbeaver.model.cli.model.option.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.exec.output.DBCOutputSeverity;
import org.jkiss.dbeaver.model.exec.output.DBCOutputWriter;
import org.jkiss.dbeaver.model.impl.DataSourceContextProvider;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.data.SQLQueryDataContainer;
import org.jkiss.dbeaver.model.sql.exec.SQLScriptProcessor;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.utils.CommonUtils;
import picocli.CommandLine;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@CommandLine.Command(name = "sql", description = "Execute SQL script")
public class SQLCommand extends CLIAbstractSubcommand {
    private static final Log log = Log.getLog(SQLCommand.class);

    @CommandLine.Parameters(
        index = "0",
        arity = "0..1",
        description = "SQL query to execute. If not specified then read from stdin or input file"
    )
    private String query;

    @CommandLine.Mixin
    private InputFileOption inputFileOption;

    @CommandLine.Mixin
    private OutputFileOption outputFileOption;

    @CommandLine.Mixin
    private DataTransferOptions dataTransferOptions;

    @CommandLine.Mixin
    private ProjectOption projectOption;

    @CommandLine.Mixin
    private DataSourceAuthOptions authOptions;

    @CommandLine.Option(names = "--print-queries", description = "Print queries before execution")
    private boolean printQueries;

    @CommandLine.Option(names = "--disable-status", description = "Disable execution status output")
    private boolean disableStatus;

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    private CreateOrFindDataSource dataSourceOptions;

    private static class CreateOrFindDataSource {
        @CommandLine.ArgGroup(
            exclusive = false
        )
        private CreateDataSourceOptions tempDataSourceOptions;

        @CommandLine.Option(names = {"-ds", "--datasource"}, arity = "1", description = "DataSource ID or name")
        private String existDataSourceIdOrName;

        @CommandLine.Option(names = {"-con", "-connect", "-ds-spec", "--datasource-specification"}, arity = "1")
        private String connectionSpec;
    }

    @Override
    public void run() throws CLIException {
        CLIConnectionUtils.connect(
            dataSourceOptions.existDataSourceIdOrName,
            dataSourceOptions.tempDataSourceOptions,
            dataSourceOptions.connectionSpec,
            getDataSourceUpdaters(),
            projectOption.getProjectIdOrName(),
            context(),
            log
        );

        String sqlQuery = query;
        if (CommonUtils.isEmpty(sqlQuery)) {
            sqlQuery = CLIUtils.readValueFromFileOrSystemIn(inputFileOption);
        }
        if (CommonUtils.isEmpty(sqlQuery)) {
            throw new CLIException("SQL query is empty", CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
        }

        DBPDataSourceContainer dataSourceContainer = context().getContextParameter(DBPDataSourceContainer.class.getName());
        if (dataSourceContainer == null) {
            throw new CLIException(
                "No datasource specified",
                CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS
            );
        }

        executeScript(dataSourceContainer, sqlQuery);
    }

    private void executeScript(
        @NotNull DBPDataSourceContainer dataSourceContainer,
        @NotNull String sqlQuery
    ) throws CLIException {
        DBPDataSource dataSource = dataSourceContainer.getDataSource();
        if (dataSource == null) {
            throw new CLIException("Can't obtain data source", CLIConstants.EXIT_CODE_ERROR);
        }
        DBRProgressMonitor monitor = new LoggingProgressMonitor(log);
        monitor.beginTask("Execute SQL script", 1);

        try {
            executeScript(monitor, dataSource, sqlQuery);
        } finally {
            monitor.done();
        }
    }

    private void executeScript(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull String sqlQuery
    ) throws CLIException {
        DBCExecutionContext executionContext = dataSource.getDefaultInstance().getDefaultContext(monitor, false);

        List<SQLScriptElement> scriptElements = SQLScriptParser.parseScript(executionContext.getDataSource(), sqlQuery);
        SQLScriptContext scriptContext = new SQLScriptContext(null, () -> executionContext, null, new LogOutputWriter(), null);

        String outputFormat = dataTransferOptions.getOutputFormat();

        if (CommonUtils.isEmpty(outputFormat)) {
            throw new CLIException("Can't determine output format: '" + outputFormat + "'", CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
        }
        DataTransferProcessorDescriptor processorDescriptor = DataTransferRegistry.getInstance()
            .getAvailableProcessors(StreamTransferConsumer.class, DBSEntity.class)
            .stream()
            .filter(p -> outputFormat.equals(p.getShortId()))
            .findFirst()
            .orElseGet(() -> DataTransferRegistry.getInstance()
                .getAvailableProcessors(StreamTransferConsumer.class, DBSEntity.class)
                .stream()
                .filter(p -> outputFormat.equals(p.getProcessorFileExtension()))
                .findFirst()
                .orElse(null));
        if (processorDescriptor == null) {
            throw new CLIException(
                "Can't find data transfer processor for format '" + outputFormat + "'",
                CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS
            );
        }

        IDataTransferProcessor processorInstance = processorDescriptor.getInstance();
        if (!(processorInstance instanceof IStreamDataExporter streamDataExporter)) {
            throw new CLIException(
                "Invalid processor. " + IStreamDataExporter.class.getSimpleName() + " expected",
                CLIConstants.EXIT_CODE_ERROR
            );
        }

        Map<String, Object> processorProperties = new HashMap<>();
        if (CommonUtils.isNotEmpty(dataTransferOptions.getOutputFormatParameters())) {
            String cutomPropsString = dataTransferOptions.getOutputFormatParameters();
            if (CommonUtils.isNotEmpty(cutomPropsString)) {
                Arrays.stream(cutomPropsString.split(","))
                    .filter(CommonUtils::isNotEmpty)
                    .map(param -> {
                        String[] keyValue = param.split("=", 2);
                        if (keyValue.length == 2
                            && CommonUtils.isNotEmpty(keyValue[0])
                            && CommonUtils.isNotEmpty(keyValue[1])
                        ) {
                            return keyValue;
                        } else {
                            log.warn("Skip invalid output format parameter '" + param + "'");
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .forEach(keyValue -> processorProperties.put(keyValue[0], keyValue[1]));
            }
        }
        for (DBPPropertyDescriptor prop : processorDescriptor.getProperties()) {
            if (!processorProperties.containsKey(prop.getId())) {
                processorProperties.put(prop.getId(), prop.getDefaultValue());
            }
        }

        DataSourceContextProvider dataSourceContextProvider = new DataSourceContextProvider(dataSource);
        StreamConsumerSettings settings = prepareSettings();

        long offset = dataTransferOptions.getOffset();
        long limit = dataTransferOptions.getLimit();
        Path outputFile = outputFileOption.getOutputFile();
        try (
            var out = outputFile == null
                ? new ByteArrayOutputStream()
                : new BufferedOutputStream(Files.newOutputStream(outputFile))
        ) {
            var uncloseableOut = new FilterOutputStream(out) {
                @Override
                public void close() {
                    // Don't close the stream because it may be reused for multiple queries
                    // and third-party nio libraries may not support APPEND option, so we should reuse original stream
                }
            };
            for (var script : scriptElements) {
                if (!(script instanceof SQLQuery q)) {
                    log.debug("Skip non-query script element: " + script.getText());
                    continue;
                }
                if (printQueries) {
                    String queryText = q.getText() + "\n";
                    if (outputFile == null) {
                        out.write(queryText.getBytes(settings.getOutputEncoding()));
                    } else {
                        context().addResult(queryText);
                    }
                }
                StreamTransferConsumer consumer = new StreamTransferConsumer();
                SQLQueryDataContainer sqlQueryDataContainer = new SQLQueryDataContainer(
                    dataSourceContextProvider, q, scriptContext, log
                );
                consumer.initTransfer(
                    sqlQueryDataContainer,
                    settings,
                    new IDataTransferConsumer.TransferParameters(
                        processorDescriptor.isBinaryFormat(),
                        processorDescriptor.isHTMLFormat(),
                        uncloseableOut
                    ),
                    streamDataExporter,
                    processorProperties,
                    dataSource.getContainer().getProject()
                );

                SQLScriptProcessor scriptProcessor = new SQLScriptProcessor(
                    executionContext,
                    List.of(q),
                    scriptContext,
                    consumer,
                    log
                );
                if (offset > 0) {
                    scriptProcessor.setOffset(offset);
                }
                if (limit > 0) {
                    scriptProcessor.setMaxRows(limit);
                }
                scriptProcessor.runScript(monitor);

                consumer.finishTransfer(monitor, false);
                DBCStatistics statistics = scriptProcessor.getTotalStatistics();

                if (!disableStatus) {
                    String statusMessage;
                    if (statistics.getRowsFetched() > 0) {
                        statusMessage = "Rows read: " + statistics.getRowsFetched() + ", (" + statistics.getTotalTime() + "ms)\n";
                    } else if (statistics.getRowsUpdated() > 0) {
                        statusMessage = "Rows updated: " + statistics.getRowsUpdated() + " (" + statistics.getTotalTime() + "ms)\n";
                    } else {
                        statusMessage = "OK\n";
                    }

                    if (outputFile == null) {
                        out.write(statusMessage.getBytes(settings.getOutputEncoding()));
                    } else {
                        context().addResult(statusMessage);
                        if (statistics.getRowsFetched() <= 0) {
                            if (statistics.getRowsUpdated() > 0) {
                                out.write((statistics.getRowsUpdated() + "\n").getBytes(settings.getOutputEncoding()));
                            } else {
                                out.write("OK\n".getBytes(settings.getOutputEncoding()));
                            }
                        }
                    }
                }

                if (out instanceof ByteArrayOutputStream byteArrayOutputStream) {
                    String result = byteArrayOutputStream.toString(settings.getOutputEncoding());
                    context().addResult(result);
                    byteArrayOutputStream.reset();
                }
                context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
            }
        } catch (Exception e) {
            throw new CLIException("Failed to execute script", e, CLIConstants.EXIT_CODE_ERROR);
        }
    }

    private StreamConsumerSettings prepareSettings() {
        StreamConsumerSettings settings = new StreamConsumerSettings();
        settings.setOutputClipboard(false);
        settings.setOutputEncodingBOM(false);
        return settings;
    }

    private static class LogOutputWriter implements DBCOutputWriter {
        @Override
        public void println(@Nullable DBCOutputSeverity severity, @Nullable String message) {
            if (message != null) {
                log.debug("CLI sql execution log: " + message);
            }
        }

        @Override
        public void flush() {

        }
    }

    @NotNull
    protected List<DataSourceUpdater> getDataSourceUpdaters() {
        List<DataSourceUpdater> updaters = new ArrayList<>();
        if (!CommonUtils.isEmpty(spec.mixins())) {
            for (CommandLine.Model.CommandSpec mixin : spec.mixins().values()) {
                if (mixin.userObject() instanceof DataSourceUpdater mixinUpdater) {
                    updaters.add(mixinUpdater);
                }
            }
        }
        if (dataSourceOptions.tempDataSourceOptions != null) {
            updaters.add(dataSourceOptions.tempDataSourceOptions);
        }
        return updaters;
    }


}
