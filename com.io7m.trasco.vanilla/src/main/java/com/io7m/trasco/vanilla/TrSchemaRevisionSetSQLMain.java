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

package com.io7m.trasco.vanilla;

import java.nio.file.Paths;
import java.util.EnumSet;

import static com.io7m.trasco.vanilla.TrSchemaRevisionSetSQL.showSQLStatements;

/**
 * Command-line program for dumping raw SQL statements from sets of revisions.
 */

public final class TrSchemaRevisionSetSQLMain
{
  private TrSchemaRevisionSetSQLMain()
  {

  }

  /**
   * Main command-line entry point.
   *
   * @param args The arguments
   *
   * @throws Exception On error
   */

  public static void main(
    final String[] args)
    throws Exception
  {
    if (args.length < 2) {
      System.err.println("usage: input.xml output.sql [excludes ...]");
      throw new IllegalArgumentException("usage: input.xml output.sql [excludes ...]");
    }

    final var input =
      Paths.get(args[0]);
    final var output =
      Paths.get(args[1]);

    final var exclusions =
      EnumSet.noneOf(TrStatementExclusion.class);

    for (int index = 2; index < args.length; ++index) {
      exclusions.add(TrStatementExclusion.valueOf(args[index]));
    }

    showSQLStatements(input, output, exclusions);
  }
}
