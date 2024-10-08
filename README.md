trasco
===

[![Maven Central](https://img.shields.io/maven-central/v/com.io7m.trasco/com.io7m.trasco.svg?style=flat-square)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.io7m.trasco%22)
[![Maven Central (snapshot)](https://img.shields.io/nexus/s/com.io7m.trasco/com.io7m.trasco?server=https%3A%2F%2Fs01.oss.sonatype.org&style=flat-square)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/io7m/trasco/)
[![Codecov](https://img.shields.io/codecov/c/github/io7m-com/trasco.svg?style=flat-square)](https://codecov.io/gh/io7m-com/trasco)
![Java Version](https://img.shields.io/badge/17-java?label=java&color=e65cc3)

![com.io7m.trasco](./src/site/resources/trasco.jpg?raw=true)

| JVM | Platform | Status |
|-----|----------|--------|
| OpenJDK (Temurin) Current | Linux | [![Build (OpenJDK (Temurin) Current, Linux)](https://img.shields.io/github/actions/workflow/status/io7m-com/trasco/main.linux.temurin.current.yml)](https://www.github.com/io7m-com/trasco/actions?query=workflow%3Amain.linux.temurin.current)|
| OpenJDK (Temurin) LTS | Linux | [![Build (OpenJDK (Temurin) LTS, Linux)](https://img.shields.io/github/actions/workflow/status/io7m-com/trasco/main.linux.temurin.lts.yml)](https://www.github.com/io7m-com/trasco/actions?query=workflow%3Amain.linux.temurin.lts)|
| OpenJDK (Temurin) Current | Windows | [![Build (OpenJDK (Temurin) Current, Windows)](https://img.shields.io/github/actions/workflow/status/io7m-com/trasco/main.windows.temurin.current.yml)](https://www.github.com/io7m-com/trasco/actions?query=workflow%3Amain.windows.temurin.current)|
| OpenJDK (Temurin) LTS | Windows | [![Build (OpenJDK (Temurin) LTS, Windows)](https://img.shields.io/github/actions/workflow/status/io7m-com/trasco/main.windows.temurin.lts.yml)](https://www.github.com/io7m-com/trasco/actions?query=workflow%3Amain.windows.temurin.lts)|

## trasco

A minimalist system for relational database schema updates.

### Features

  * Declare schemas, and upgrades to those schemas.
  * Reliably and atomically update databases.
  * Written in pure Java 17.
  * [OSGi](https://www.osgi.org/) ready.
  * [JPMS](https://en.wikipedia.org/wiki/Java_Platform_Module_System) ready.
  * ISC license.
  * High-coverage automated test suite.

### Motivation

Most relational databases involve declaring a schema ahead of time, detailing
the tables, views, and functions. Most applications, as they evolve, will
require changes to be made to those schemas over time. The `trasco` package
provides a minimal system for performing schema upgrades.

The package assumes the use of a database with transactional DDL. That is,
DDL commands such as `CREATE TABLE`, `ALTER TABLE`, and etc, can be executed
inside transactions and committed or rolled back as necessary. If you are not
using a database that supports transactional DDL, every upgrade has the
potential to damage your database whether or not you use `trasco`;
use a better database.

Databases known to support transactional DDL at the time of writing include:

  * [PostgreSQL](https://www.postgresql.org/)
  * [Apache Derby](https://db.apache.org/derby/)
  * [SQLite](https://www.sqlite.org/index.html)

### Building

```
$ mvn clean verify
```

### Usage

The main classes used in the `trasco` package are the `TrExecutor` and
`TrSchemaRevisionSet` class. A `TrSchemaRevisionSet` contains a complete
set of versioned revisions to a database schema, starting from the initial
database state.

The `TrSchemaRevisionSetParser` class can produce a `TrSchemaRevisionSet`
from an XML file following the [included XSD schema](com.io7m.trasco.xml.schemas/src/main/resources/com/io7m/trasco/xml/schemas/statements-1.xsd).

The `trasco` package expects you to be able to store the current database
schema version and application name somewhere in the database. Typically,
applications will create a `schema_version` table as part of the initial
schema. For example:

```
<Schemas xmlns="urn:com.io7m.trasco.database.statements:1:0">
  <Schema versionCurrent="0">
    <Comment>
      The schema version table stores the current version of the database schema. Implementations are expected to query
      this table on connecting to the database in order to ensure that the calling code is compatible with the tables in
      the database.
    </Comment>

    <Statement><![CDATA[
create table schema_version (
  version_lock            char(1) not null default 'X',
  version_application_id  text    not null,
  version_number          bigint  not null,

  constraint check_lock_primary primary key (version_lock),
  constraint check_lock_locked check (version_lock = 'X')
)
]]></Statement>
  </Schema>
  ...
```

The initial schema version `0` declares a `schema_version` table that carries
a constraint that restricts it to consisting of a single row. Storing the
application ID is useful to prevent accidental errors in environments where
multiple databases are present; if a user points the package at the wrong
database, the mistake can be caught before any upgrades are attempted.

In order to actually populate and upgrade databases, the `TrExecutor` class
consumes a `TrSchemaRevisionSet` and runs each revision update in order.

The `TrExecutor` takes pair of functions that, given a database connection,
retrieve the current schema version, and set the current schema version,
respectively. For example, a function to retrieve the schema version given
a PostgreSQL database with the table declaration above looks like:

```
  private static final String DATABASE_APPLICATION_ID = ...

  private static Optional<BigInteger> schemaVersionGet(
    final Connection connection)
    throws SQLException
  {
    Objects.requireNonNull(connection, "connection");

    try {
      final var statementText =
        "SELECT version_application_id, version_number FROM schema_version";
      LOG.debug("execute: {}", statementText);

      try (var statement = connection.prepareStatement(statementText)) {
        try (var result = statement.executeQuery()) {
          if (!result.next()) {
            throw new SQLException("schema_version table is empty!");
          }
          final var applicationId =
            result.getString(1);
          final var version =
            result.getLong(2);

          if (!Objects.equals(applicationId, DATABASE_APPLICATION_ID)) {
            throw new SQLException(
              String.format(
                "Database application ID is %s but should be %s",
                applicationId,
                DATABASE_APPLICATION_ID
              )
            );
          }

          return Optional.of(valueOf(version));
        }
      }
    } catch (final SQLException e) {
      final var state = e.getSQLState();
      if (state == null) {
        throw e;
      }
      if (state.equals(PSQLState.UNDEFINED_TABLE.getState())) {
        connection.rollback();
        return Optional.empty();
      }

      throw e;
    }
  }
```

The function takes into account that a completely fresh database might not
have a `schema_version` table, and returns `Optional.empty()` accordingly. This
will cause the `TrExecutor` class to assume that the database is fresh, and
start upgrades from the initial revision.

The function to set the database schema version is unsurprising:

```
  private static void schemaVersionSet(
    final BigInteger version,
    final Connection connection)
    throws SQLException
  {
    final String statementText;
    if (Objects.equals(version, BigInteger.ZERO)) {
      statementText = "insert into schema_version (version_application_id, version_number) values (?, ?)";
      try (var statement =
             connection.prepareStatement(statementText)) {
        statement.setString(1, DATABASE_APPLICATION_ID);
        statement.setLong(2, version.longValueExact());
        statement.execute();
      }
    } else {
      statementText = "update schema_version set version_number = ?";
      try (var statement =
             connection.prepareStatement(statementText)) {
        statement.setLong(1, version.longValueExact());
        statement.execute();
      }
    }
  }
```

If the schema version is `0`, we insert the schema version into the table.
Otherwise, we update the existing single row in the table.

The `TrExecutor` class should be used with connections that do not auto-commit.
The class itself will _not_ commit any changes by itself, allowing database
upgrades to be completely atomic assuming that the underlying database supports
transactional DDL. The use of the `trasco` package on databases without
transactional DDL is strongly discouraged; in fact the use of databases
without transactional DDL is strongly discouraged in any situation.

Assuming the functions given above, the `TrExecutor` class can be used:

```
TrSchemaRevisionSet revisions;
DataSource datasource;

try (var connection = dataSource.getConnection()) {
  connection.setAutoCommit(false);

  new TrExecutors().create(
    new TrExecutorConfiguration(
      Example::schemaVersionGet,
      Example::schemaVersionSet,
      event -> { },
      revisions,
      PERFORM_UPGRADES,
      TrArguments.empty(),
      connection
    )
  ).execute();
  connection.commit();
}
```

The `TrExecutor` class publishes events detailing its current progress. These
can be ignored (as in the example) if not needed. Additionally, the
`TrExecutor` can be told to either perform upgrades, or simply fail if the
database is not already at the correct version. Failing immediately can be
useful in the case of read-only databases, or where code simply wants to check
if a target database is at the right version or not.

If any part of the upgrade fails, the `TrExecutor` class raises an exception
and the database will be left in its original state prior to _any_ revision
upgrades.

### Parameterized Statements

Some statements may require the use of configurable parameters. For example,
PostgreSQL's full text search feature requires indexes to be created with
the name of the language used as a parameter (`'english'`, for example). Schemas
can have typed, named parameters that are supplied with values at runtime:

```
<Schemas xmlns="urn:com.io7m.trasco.database.statements:1:0">
  <Parameters>
    <Parameter name="number0" type="NUMERIC"/>
    <Parameter name="number1" type="NUMERIC"/>
    <Parameter name="number2" type="NUMERIC"/>
    <Parameter name="string0" type="STRING"/>
    <Parameter name="number3" type="NUMERIC"/>
  </Parameters>

  <Schema versionCurrent="0">
    <Statement><![CDATA[
create table x (f0 integer, f1 bigint, f2 varchar(100), f3 double, f4 decimal)
]]></Statement>

    <StatementParameterized>
      <ParameterReferences>
        <ParameterReference order="0" name="number0"/>
        <ParameterReference order="1" name="number1"/>
        <ParameterReference order="2" name="string0"/>
        <ParameterReference order="3" name="number2"/>
        <ParameterReference order="4" name="number3"/>
      </ParameterReferences>
      <Text><![CDATA[
insert into x values (?, ?, ?, ?, ?)
]]></Text>
    </StatementParameterized>
  </Schema>
</Schemas>
```

Parameters have a name and a type, and are scoped over all `Schema` declarations
in the `Schemas` set. A `StatementParameterized` is an SQL statement that
takes a list of parameter references. A `ParameterReference` refers to
a parameter declared in the `Parameters` section, and has a defined `order`.

In the example above, the value the programmer supplies to the `number0`
parameter will be passed as the first parameter to the parameterized SQL
statement.

```
new TrExecutors().create(
  new TrExecutorConfiguration(
    Example::schemaVersionGet,
    Example::schemaVersionSet,
    event -> { },
    revisions,
    PERFORM_UPGRADES,
    new TrArguments(
      Map.ofEntries(
        Map.entry("number0", new TrArgumentNumeric("number0", 23)),
        Map.entry("number1", new TrArgumentNumeric("number1", 23L)),
        Map.entry("string0", new TrArgumentString("string0", "23")),
        Map.entry("number2", new TrArgumentNumeric("number2", 23.0)),
        Map.entry("number3", new TrArgumentNumeric("number3", BigDecimal.valueOf(23.0)))
      )
    ),
    connection
  )
).execute();
```

Failing to provide parameters, or providing parameters of the wrong type,
is an error.

#### Parameter Interpolation

By default, `trasco` uses prepared statements to execute schema statements.
For some kinds of statements, on some databases, this will cause problems.
For example, some kinds of DDL statements on PostgreSQL (such as `CREATE INDEX`)
cannot have parameters bound using JDBC. For example:

```
try (var st = connection.prepareStatement("CREATE INDEX ? ON t (c)")) {
  st.setString(1, "index_name");
  st.execute();
}
```

The above code will fail with an error similar to `ERROR: there is no parameter $1`.
Unfortunately, to deal with this kind of statement, it is necessary to manually
build an SQL statement. The `trasco` package offers a `STRING_FORMATTING`
interpolation mode that safely builds query strings with the proper escapes.
Specify an `interpolation` attribute with a value of `STRING_FORMATTING`
for a particular parameterized statement, and use `%s` format string
parameters instead of the standard `?` JDBC parameters:

```
<StatementParameterized parameterInterpolation="STRING_FORMATTING">
  <ParameterReferences>
    <ParameterReference order="0" name="number0"/>
    <ParameterReference order="1" name="number1"/>
    <ParameterReference order="2" name="string0"/>
    <ParameterReference order="3" name="number2"/>
    <ParameterReference order="4" name="number3"/>
  </ParameterReferences>
  <Text><![CDATA[
insert into x values (%s, %s, %s, %s, %s)
]]></Text>
</StatementParameterized>
```

The statement will be interpolated manually using `String.format()` and
the Apache Commons Text string escape functions to safely escape characters
in strings. The resulting text will be passed to JDBC directly.

