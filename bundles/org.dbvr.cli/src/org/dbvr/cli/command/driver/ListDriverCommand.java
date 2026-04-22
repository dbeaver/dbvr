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
package org.dbvr.cli.command.driver;

import org.jkiss.dbeaver.model.cli.CLIAbstractSubcommand;
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.cli.CLIUtils;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import picocli.CommandLine;

import java.util.*;

@CommandLine.Command(name = "list", description = "Show list of supported database drivers")
public class ListDriverCommand extends CLIAbstractSubcommand {
    @CommandLine.ParentCommand
    private DriverManagerHandler parent;

    @CommandLine.Option(names = {"--provider"}, description = "Filter by provider ID")
    private String providerId;


    @CommandLine.Option(names = {"--show-properties"}, description = "Show driver properties")
    private boolean showProperties;

    @Override
    public void run() throws CLIException {
        List<DBPDriver> drivers = getSupportedDBInstances();
        if (drivers.isEmpty()) {
            return;
        }

        Map<DBPDataSourceProviderDescriptor, List<DBPDriver>> groupedDrivers = new TreeMap<>(
            Comparator.comparing(DBPDataSourceProviderDescriptor::getName)
        );

        for (DBPDriver driver : drivers) {
            groupedDrivers.computeIfAbsent(driver.getProviderDescriptor(), k -> new ArrayList<>()).add(driver);
        }

        StringBuilder outBuilder = new StringBuilder();
        for (Map.Entry<DBPDataSourceProviderDescriptor, List<DBPDriver>> entry : groupedDrivers.entrySet()) {
            DBPDataSourceProviderDescriptor provider = entry.getKey();
            List<DBPDriver> providerDrivers = entry.getValue();
            providerDrivers.sort(Comparator.comparing(DBPDriver::getName));

            outBuilder.append(String.format("%s (%s)%n", provider.getId(), provider.getName()));

            for (DBPDriver driver : providerDrivers) {
                outBuilder.append(String.format("    %s (%s)%s%n",
                    driver.getId(),
                    driver.getName(),
                    driver.isDisabled() ? " [Disabled]" : ""
                ));

                if (showProperties) {
                    Map<String, DBPPropertyDescriptor> allProperties = new LinkedHashMap<>();
                    for (DBPPropertyDescriptor prop : driver.getProviderPropertyDescriptors()) {
                        allProperties.put(prop.getId(), prop);
                    }
                    if (!allProperties.isEmpty()) {
                        for (DBPPropertyDescriptor prop : allProperties.values()) {
                            outBuilder.append("      ").append(CLIUtils.getPropertyHelpText(prop));
                        }
                    }
                }
            }
            outBuilder.append(System.lineSeparator());
        }
        context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        context().addResult(outBuilder.toString());
    }

    private List<DBPDriver> getSupportedDBInstances() {
        DataSourceProviderRegistry dataSourceRegistry = DataSourceProviderRegistry.getInstance();
        List<DataSourceProviderDescriptor> dataSourceProviders = dataSourceRegistry.getDataSourceProviders();
        List<DBPDriver> supportedDataBases = new ArrayList<>();
        for (DataSourceProviderDescriptor providerDescriptor : dataSourceProviders) {
            if (providerId != null && !providerDescriptor.getId().equals(providerId)) {
                continue;
            }
            for (DBPDriver driver : providerDescriptor.getDrivers()) {
                if (driver.getReplacedBy() == null) {
                    supportedDataBases.add(driver);
                }
            }
        }
        supportedDataBases.sort(Comparator.comparing(DBPDriver::getName));
        return supportedDataBases;
    }

}
