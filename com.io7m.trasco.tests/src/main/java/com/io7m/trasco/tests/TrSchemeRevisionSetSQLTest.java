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


package com.io7m.trasco.tests;

import com.io7m.trasco.vanilla.TrSchemaRevisionSetSQL;
import com.io7m.trasco.vanilla.TrStatementExclusion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import static com.io7m.trasco.vanilla.TrStatementExclusion.FUNCTIONS;
import static com.io7m.trasco.vanilla.TrStatementExclusion.GRANTS;
import static com.io7m.trasco.vanilla.TrStatementExclusion.ROLES;
import static com.io7m.trasco.vanilla.TrStatementExclusion.TRIGGERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public final class TrSchemeRevisionSetSQLTest
{
  private Path directory;

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.directory =
      TrTestDirectories.createTempDirectory();
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    TrTestDirectories.deleteDirectory(this.directory);
  }

  @Test
  public void testExcludeRoles()
    throws Exception
  {
    final var input =
      this.resourceOf("example-2.xml");
    final var output =
      this.directory.resolve("out.sql");

    TrSchemaRevisionSetSQL.showSQLStatements(
      input,
      output,
      EnumSet.of(ROLES)
    );

    final var lines = Files.lines(output).toList();
    assertEquals(
      "grant select, insert, update on something to egg;",
      lines.get(0));
    assertEquals(1, lines.size());
  }

  @Test
  public void testExcludeGrants()
    throws Exception
  {
    final var input =
      this.resourceOf("example-2.xml");
    final var output =
      this.directory.resolve("out.sql");

    TrSchemaRevisionSetSQL.showSQLStatements(
      input,
      output,
      EnumSet.of(GRANTS)
    );

    final var lines = Files.lines(output).toList();
    assertEquals("create role egg;", lines.get(0));
    assertEquals("drop role egg;", lines.get(1));
    assertEquals(2, lines.size());
  }

  @Test
  public void testExcludeFunctions()
    throws Exception
  {
    final var input =
      this.resourceOf("example-3.xml");
    final var output =
      this.directory.resolve("out.sql");

    TrSchemaRevisionSetSQL.showSQLStatements(
      input,
      output,
      EnumSet.of(FUNCTIONS)
    );

    final var lines = Files.lines(output).toList();
    assertEquals("create role egg;", lines.get(0));
    assertEquals("grant select, insert, update on something to egg;", lines.get(1));
    assertEquals("drop role egg;", lines.get(2));
    assertEquals(3, lines.size());
  }

  @Test
  public void testExcludeTriggers()
    throws Exception
  {
    final var input =
      this.resourceOf("example-4.xml");
    final var output =
      this.directory.resolve("out.sql");

    TrSchemaRevisionSetSQL.showSQLStatements(
      input,
      output,
      EnumSet.of(TRIGGERS)
    );

    final var lines = Files.lines(output).toList();
    assertFalse(lines.stream().anyMatch(s -> s.toUpperCase().startsWith("CREATE TRIGGER")));
    assertFalse(lines.stream().anyMatch(s -> s.toUpperCase().startsWith("DROP TRIGGER")));
  }

  @Test
  public void testExcludeNothing()
    throws Exception
  {
    final var input =
      this.resourceOf("example-2.xml");
    final var output =
      this.directory.resolve("out.sql");

    TrSchemaRevisionSetSQL.showSQLStatements(
      input,
      output,
      EnumSet.noneOf(TrStatementExclusion.class)
    );

    final var lines = Files.lines(output).toList();
    assertEquals("create role egg;", lines.get(0));
    assertEquals(
      "grant select, insert, update on something to egg;",
      lines.get(1));
    assertEquals("drop role egg;", lines.get(2));
    assertEquals(3, lines.size());
  }

  private Path resourceOf(
    final String name)
    throws IOException
  {
    return TrTestDirectories.resourceOf(
      TrSchemeRevisionSetSQLTest.class,
      this.directory,
      name
    );
  }
}
