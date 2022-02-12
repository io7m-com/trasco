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

package com.io7m.trasco.api;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * A function executed to retrieve version information.
 *
 * It is expected that users will maintain a table containing a single row that
 * describes the current schema version. For example:
 *
 * <pre>
 * create table schema_version (
 *   version_lock   char(1) not null default 'X',
 *   version_number bigint  not null,
 *   constraint check_lock_primary primary key (version_lock),
 *   constraint check_lock_locked check (version_lock = 'X')
 * )
 * </pre>
 *
 * The function would query the {@code version_number} column to retrieve the
 * schema version.
 */

public interface TrExecutorVersionRetrieverType
{
  /**
   * Retrieve version information from the database.
   *
   * @param connection A database connection
   *
   * @return A version number, if one exists
   *
   * @throws SQLException On errors
   */

  Optional<BigInteger> determineVersion(
    Connection connection)
    throws SQLException;
}
