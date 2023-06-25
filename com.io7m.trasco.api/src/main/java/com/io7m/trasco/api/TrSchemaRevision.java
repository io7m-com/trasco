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
import java.util.List;
import java.util.Objects;

/**
 * A database schema revision.
 *
 * @param version    The version
 * @param statements The SQL statements used to upgrade the previous version to
 *                   this version
 */

public record TrSchemaRevision(
  BigInteger version,
  List<TrStatementType> statements)
  implements Comparable<TrSchemaRevision>
{
  /**
   * A database schema revision.
   *
   * @param version    The version
   * @param statements The SQL statements used to upgrade the previous version
   *                   to this version
   */

  public TrSchemaRevision
  {
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(statements, "statements");
  }

  @Override
  public int compareTo(
    final TrSchemaRevision o)
  {
    return this.version.compareTo(o.version);
  }
}
