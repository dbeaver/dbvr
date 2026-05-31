# dbvr Community, CLI from DBeaver

[![Build](https://github.com/dbeaver/dbvr/actions/workflows/push-pr-devel.yml/badge.svg?branch=devel)](https://github.com/dbeaver/dbvr/actions/workflows/push-pr-devel.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

dbvr Community, a universal CLI for database querying, is a command-line interface for working with databases.
It can act as a standalone CLI application or in conjunction with DBeaver and CloudBeaver.
It provides a scriptable way to manage database projects and data sources, inspect metadata, and execute SQL from the terminal.

## Why dbvr Community is useful

- **Automate database workflows** with a terminal-first interface built on top of the DBeaver platform
- **Manage projects and data sources** without opening a GUI
- **Run SQL scripts and ad-hoc queries** against supported databases
- **Inspect database metadata** such as databases, schemas, tables, and DDL
- **Integrate with CI/CD and shell scripts** using standard command output and files
- **Reuse DBeaver ecosystem support** for drivers and data-source handling

## Features

- `project` commands for creating, listing, renaming, deleting, and selecting default projects
- `datasource` commands for creating, viewing, listing, updating, moving, and deleting data sources
- `sql` command to execute SQL from a literal query, a file, or standard input
- `meta` commands for working with database, schema, and table information

## Getting started

### Prerequisites

To build this repository locally, you need:

- Java
- Maven
- Local sibling checkouts of dependencies referenced by `project.deps`:
  - `dbeaver-common`
  - `dbeaver`

The root Maven build inherits from `../dbeaver`, and the product aggregate also includes sibling modules from `../../../dbeaver-common` and `../../../dbeaver`.

## Repository layout

`dbvr` is built together with sibling repositories. Clone all of them into the
same parent directory, so the relative paths in the Maven/Tycho build resolve
correctly

```bash
git clone https://github.com/dbeaver/dbeaver-common.git
git clone https://github.com/dbeaver/dbeaver.git
git clone https://github.com/dbeaver/dbvr.git
git clone https://github.com/dbeaver/idea-rcp-launch-config-generator.git
git clone https://github.com/dbeaver/dbeaver-osgi-common.git
```
## Build product
```bash
mvn -f product/aggregate/pom.xml \
-Dheadless-platform \
-Pproduct-dbvr-ce \
-Dbuild.all-environments \
package
```

## Develop in IDEA

To generate IntelliJ IDEA project files and RCP launch configurations, run from the `dbvr` repository root:

```bash
./generate_workspace.sh
```

On Windows, use `generate_workspace.cmd`.

Then run
```bash
cd ../dbeaver
```
```bash
mvn generate-sources
```

### Run the CLI

The packaged product creates a `dbvr` executable for Linux and Windows, and a `dbvr.app` bundle for macOS.

After building or downloading a packaged distribution, add the executable to your `PATH` and run:

```bash
dbvr --help
```

## Usage examples

### Show top-level help

```bash
dbvr --help
```

### Work with projects

Create a project:

```bash
dbvr project create --name MyProject --description "This is my project"
```

List projects:

```bash
dbvr project list
```

Set the default project:

```bash
dbvr project default MyProject
```

### Inspect available drivers

```bash
dbvr driver list
```

Show driver properties:

```bash
dbvr driver list --show-properties
```

### Execute SQL

Run an inline query using an existing datasource:

```bash
dbvr sql -ds my-datasource-id -format csv "select * from my_table"
```

Read SQL from a file or standard input:

```bash
dbvr sql -ds my-datasource-id -format json --input-file query.sql
cat query.sql | dbvr sql -ds my-datasource -format json
```

Write results to a file:

```bash
dbvr sql -ds my-datasource-id -format csv -output-file result.csv "select * from my_table"
```

### Explore metadata

Examples of supported metadata operations include listing databases, listing tables, and getting DDL for database objects.

```bash
dbvr meta database list -ds my-datasource-id 
# or
# dbvr meta schema list -ds my-datasource-id ...
# dbvr meta table ddl -ds my-datasource-id -sn=public -tn=orders ...
```

> Exact options and available subcommands may vary by command. Use `dbvr <command> --help` to inspect the current CLI surface.

## Where to get help

- Open an issue in this repository: https://github.com/dbeaver/dbvr/issues
- Browse DBeaver resources and related platform documentation: https://github.com/dbeaver
- Use command help locally with `dbvr --help` or `dbvr <command> --help`

## Maintainers

This project is maintained by the DBeaver team and contributors.

- Organization: [dbeaver](https://github.com/dbeaver)
- Repository: [dbeaver/dbvr](https://github.com/dbeaver/dbvr)

## License

Licensed under the [Apache License 2.0](LICENSE).
