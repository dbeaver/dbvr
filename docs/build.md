# Building DBeaver CLI (dbvr)

This document describes how to build the `dbvr` product from source and how to
generate an IntelliJ IDEA workspace for development.

## Prerequisites

- JDK 21 or higher
- Apache Maven 3.9.x or higher (the bundled `dbeaver-common/mvnw` wrapper can be used instead)

## Repository layout

`dbvr` is built together with sibling repositories. Clone all of them into the
same parent directory so the relative paths in the Maven/Tycho build resolve
correctly

```bash
git clone https://github.com/dbeaver/dbeaver-common.git
git clone https://github.com/dbeaver/dbeaver.git
git clone https://github.com/dbeaver/dbeaver-jdbc-libsql.git
git clone https://github.com/dbeaver/dbvr.git
git clone https://github.com/dbeaver/idea-rcp-launch-config-generator.git
git clone https://github.com/dbeaver/dbeaver-osgi-common.git
```

## Build the product

Run from the `dbvr` repository root:

```bash
./generate_workspace.sh
```

On Windows use `generate_workspace.cmd`.
