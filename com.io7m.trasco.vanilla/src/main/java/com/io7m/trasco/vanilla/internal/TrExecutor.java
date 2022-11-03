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

import com.io7m.trasco.api.TrEventExecutingSQL;
import com.io7m.trasco.api.TrEventUpgrading;
import com.io7m.trasco.api.TrException;
import com.io7m.trasco.api.TrExecutorConfiguration;
import com.io7m.trasco.api.TrExecutorType;
import com.io7m.trasco.api.TrExecutorUpgrade;
import com.io7m.trasco.api.TrSchemaRevision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.trasco.api.TrErrorCode.SQL_EXCEPTION;
import static com.io7m.trasco.api.TrErrorCode.UNRECOGNIZED_SCHEMA_REVISION;
import static com.io7m.trasco.api.TrErrorCode.UPGRADE_DISALLOWED;

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
          new StringBuilder(128)
            .append("Incompatible database schema.")
            .append(System.lineSeparator()).append(
              "No schema version number was retrieved, and this executor was configured with ")
            .append(this.configuration.upgrade())
            .toString(),
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
        new StringBuilder(128)
          .append("Database schema version is too high!")
          .append(System.lineSeparator())
          .append("  Current version: ")
          .append(versionHaveNow)
          .append(System.lineSeparator())
          .append("  Highest known version: ")
          .append(highestKnown)
          .append(System.lineSeparator())
          .toString(),
        UNRECOGNIZED_SCHEMA_REVISION
      );
    }

    if (!startVersion.equals(Optional.of(highestKnown))) {
      if (this.configuration.upgrade() == TrExecutorUpgrade.FAIL_INSTEAD_OF_UPGRADING) {
        throw new TrException(
          new StringBuilder(128)
            .append("Incompatible database schema.")
            .append(System.lineSeparator())
            .append("The schema version is ")
            .append(startVersion.get())
            .append(" but the highest schema we understand is ")
            .append(highestKnown)
            .append(", and upgrades are not permitted by the configuration (")
            .append(this.configuration.upgrade())
            .append(")")
            .toString(),
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

    for (final var statementText : revision.statements()) {
      final var stripped = statementText.strip();
      LOG.trace("execute: {}", stripped);

      this.configuration.events()
        .accept(new TrEventExecutingSQL(stripped));

      try (var statement = connection.prepareStatement(stripped)) {
        statement.execute();
      }
    }
  }
}
