/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.trasco.vanilla.internal;

import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.trasco.api.TrArgumentNumeric;
import com.io7m.trasco.api.TrArgumentString;
import com.io7m.trasco.api.TrEventExecutingSQL;
import com.io7m.trasco.api.TrEventUpgrading;
import com.io7m.trasco.api.TrException;
import com.io7m.trasco.api.TrExecutorConfiguration;
import com.io7m.trasco.api.TrExecutorType;
import com.io7m.trasco.api.TrExecutorUpgrade;
import com.io7m.trasco.api.TrSchemaRevision;
import com.io7m.trasco.api.TrStatement;
import com.io7m.trasco.api.TrStatementParameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.trasco.api.TrErrorCode.SQL_EXCEPTION;
import static com.io7m.trasco.api.TrErrorCode.UNRECOGNIZED_SCHEMA_REVISION;
import static com.io7m.trasco.api.TrErrorCode.UPGRADE_DISALLOWED;
import static java.util.Map.entry;

/**
 * An executor.
 */

public final class TrExecutor implements TrExecutorType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(TrExecutor.class);

  private final TrExecutorConfiguration configuration;

  /**
   * An executor.
   *
   * @param inConfiguration The configuration
   */

  public TrExecutor(
    final TrExecutorConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
  }

  @Override
  public void execute()
    throws TrException
  {
    this.configuration.arguments()
      .checkSatisfies(this.configuration.revisions().parameters());

    final Optional<BigInteger> existing;
    try {
      existing = this.configuration.versionGet()
        .determineVersion(this.configuration.connection());
    } catch (final SQLException e) {
      throw new TrException(e.getMessage(), e, SQL_EXCEPTION);
    }

    try {
      this.executeUpgrades(existing);
    } catch (final SQLException e) {
      throw new TrException(e.getMessage(), e, SQL_EXCEPTION);
    }
  }

  private void executeUpgrades(
    final Optional<BigInteger> startVersion)
    throws TrException, SQLException
  {
    if (startVersion.isEmpty()) {
      if (this.configuration.upgrade() == TrExecutorUpgrade.FAIL_INSTEAD_OF_UPGRADING) {
        throw new TrException(
          "Incompatible database schema, and upgrades are not permitted by the configuration.",
          Map.ofEntries(
            entry("Configuration", this.configuration.upgrade().toString())
          ),
          UPGRADE_DISALLOWED
        );
      }
    }

    final var connection = this.configuration.connection();
    connection.setAutoCommit(false);

    final var revisionsMap =
      this.configuration.revisions()
        .revisions();

    LOG.debug(
      "{} schema revisions available",
      Integer.valueOf(revisionsMap.size())
    );

    if (revisionsMap.isEmpty()) {
      return;
    }

    BigInteger versionHaveNow;
    if (startVersion.isEmpty()) {
      versionHaveNow = revisionsMap.firstKey().subtract(BigInteger.ONE);
    } else {
      versionHaveNow = startVersion.get();
    }

    LOG.debug("database schema version is {}", versionHaveNow);

    final var highestKnown = revisionsMap.lastKey();
    if (versionHaveNow.compareTo(highestKnown) > 0) {
      throw new TrException(
        "Database schema version is too high!",
        Map.ofEntries(
          entry("Current Version", versionHaveNow.toString()),
          entry("Highest Known Version", highestKnown.toString())
        ),
        UNRECOGNIZED_SCHEMA_REVISION
      );
    }

    if (!startVersion.equals(Optional.of(highestKnown))) {
      if (this.configuration.upgrade() == TrExecutorUpgrade.FAIL_INSTEAD_OF_UPGRADING) {
        throw new TrException(
          "Incompatible database schema, and upgrades are not permitted by the configuration.",
          Map.ofEntries(
            entry("Schema Version", startVersion.get().toString()),
            entry("Highest Known Version", highestKnown.toString()),
            entry("Configuration", this.configuration.upgrade().toString())
          ),
          UPGRADE_DISALLOWED
        );
      }
    }

    final var upgrades =
      revisionsMap.tailMap(versionHaveNow, false);

    for (final var entry : upgrades.entrySet()) {
      final var revision = entry.getValue();

      LOG.debug(
        "upgrading revision {} to revision {}",
        versionHaveNow,
        revision.version()
      );

      this.configuration.events()
        .accept(new TrEventUpgrading(versionHaveNow, revision.version()));

      this.executeRevision(revision);
      this.configuration.versionSet()
        .updateVersion(revision.version(), connection);
      versionHaveNow = revision.version();
    }
  }

  private void executeRevision(
    final TrSchemaRevision revision)
    throws SQLException
  {
    final var connection = this.configuration.connection();

    for (final var statement : revision.statements()) {
      if (statement instanceof final TrStatement st) {
        this.executeStatement(connection, st);
        continue;
      }
      if (statement instanceof final TrStatementParameterized st) {
        this.executeStatementParameterized(connection, st);
        continue;
      }
    }
  }

  private void executeStatementParameterized(
    final Connection connection,
    final TrStatementParameterized st)
    throws SQLException
  {
    final var stripped = st.text().strip();
    LOG.trace("execute: {}", stripped);

    this.configuration.events()
      .accept(new TrEventExecutingSQL(stripped));

    final var arguments =
      this.configuration.arguments().arguments();
    final var referencesInOrder =
      st.references().inOrder();

    try (var sql = connection.prepareStatement(stripped)) {

      /*
       * Set all of the required arguments.
       */

      for (final var order : referencesInOrder.keySet()) {
        final var parameterRef =
          referencesInOrder.get(order);
        final var argument =
          arguments.get(parameterRef.name());

        final int paramIndex = order.intValue() + 1;
        if (argument instanceof final TrArgumentString s) {
          sql.setString(paramIndex, s.value());
          continue;
        }
        if (argument instanceof final TrArgumentNumeric n) {
          if (n.value() instanceof final Integer x) {
            sql.setInt(paramIndex, x.intValue());
          } else if (n.value() instanceof final Long x) {
            sql.setLong(paramIndex, x.longValue());
          } else if (n.value() instanceof final Double x) {
            sql.setDouble(paramIndex, x.doubleValue());
          } else if (n.value() instanceof final BigDecimal x) {
            sql.setBigDecimal(paramIndex, x);
          } else {
            throw new UnimplementedCodeException();
          }
        }
      }

      sql.execute();
    }
  }

  private void executeStatement(
    final Connection connection,
    final TrStatement st)
    throws SQLException
  {
    final var stripped = st.text().strip();
    LOG.trace("execute: {}", stripped);

    this.configuration.events()
      .accept(new TrEventExecutingSQL(stripped));

    try (var sql = connection.prepareStatement(stripped)) {
      sql.execute();
    }
  }
}
