# DBeaver CLI

[![Build](https://github.com/dbeaver/dbvr/actions/workflows/push-pr-devel.yml/badge.svg?branch=devel)](https://github.com/dbeaver/dbvr/actions/workflows/push-pr-devel.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

DBeaver CLI (`dbvr`) is a command-line interface for working with databases.
It can act as as standalone CLI application or with conjunction with DBeaver and CloudBeaver. 
It provides a scriptable way to manage database projects and data sources, inspect metadata, and execute SQL from the terminal.

## Why DBeaver CLI is useful

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

### Clone the required repositories

```bash
git clone https://github.com/dbeaver/dbeaver-common.git
git clone https://github.com/dbeaver/dbeaver.git
git clone https://github.com/dbeaver/dbvr.git
```

Arrange them as sibling directories so the relative paths in the Maven/Tycho build resolve correctly.

### Build the project

From the `dbvr` repository root:

```bash
mvn -f product/aggregate/pom.xml \
  -Dheadless-platform \
  -Pproduct-dbvr-ce \
  -Dbuild.all-environments \
  package
```

This is the same aggregate build profile used by the repository CI workflow.

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
dbvr sql -ds my-datasource-id --format csv "select * from my_table"
```

Read SQL from a file or standard input:

```bash
dbvr sql -ds my-datasource-id --format json --input-file query.sql
cat query.sql | dbvr sql -ds my-datasource --format json
```

Write results to a file:

```bash
dbvr sql -ds my-datasource-id --format csv --output-file result.csv "select * from my_table"
```

### Explore metadata

Examples of supported metadata operations include listing databases, listing tables, and getting DDL for database objects.

```bash
dbvr -ds my-datasource-id database list
# or
# dbvr -ds my-datasource-id schema list ...
# dbvr -ds my-datasource-id table ddl ...
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
