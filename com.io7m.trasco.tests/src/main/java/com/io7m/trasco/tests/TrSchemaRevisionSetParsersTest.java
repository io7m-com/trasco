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

import com.io7m.anethum.api.ParsingException;
import com.io7m.trasco.api.TrParameterReference;
import com.io7m.trasco.api.TrParameterReferences;
import com.io7m.trasco.api.TrSchemaRevision;
import com.io7m.trasco.api.TrStatement;
import com.io7m.trasco.api.TrStatementParameterized;
import com.io7m.trasco.vanilla.TrSchemaRevisionSetParsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Stream;

import static com.io7m.trasco.api.TrParameterInterpolation.PREPARED_STATEMENT;
import static com.io7m.trasco.api.TrParameterInterpolation.STRING_FORMATTING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class TrSchemaRevisionSetParsersTest
{
  private TrSchemaRevisionSetParsers parsers;
  private Path directory;

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.parsers =
      new TrSchemaRevisionSetParsers();
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
  public void testEmpty()
    throws Exception
  {
    try (var stream = this.resourceOf("example-0.xml")) {
      final var set =
        this.parsers.parse(URI.create("urn:stdin"), stream);

      assertEquals(new TreeMap<>(), set.revisions());
    }
  }

  @Test
  public void testSimple()
    throws Exception
  {
    try (var stream = this.resourceOf("example-1.xml")) {
      final var set =
        this.parsers.parse(URI.create("urn:stdin"), stream);

      final var rev0 =
        new TrSchemaRevision(
          BigInteger.ZERO,
          List.of(
            new TrStatement(
            """
              create table schema_version (
                version_lock   integer not null default 1,
                version_number integer  not null,
                          
                constraint check_lock_primary primary key (version_lock),
                constraint check_lock_locked check (version_lock = 1)
              )
                            """.trim())
          )
        );

      final var rev1 =
        new TrSchemaRevision(
          BigInteger.ONE,
          List.of(new TrStatement(
            """
              create table example0 (
                user_id text not null primary key
              )
                            """.trim()
          ))
        );

      final var rev2 =
        new TrSchemaRevision(
          BigInteger.TWO,

          List.of(new TrStatement(
            """
              alter table example0 add col1 integer
                            """.trim()
          ))
        );

      final var rev3 =
        new TrSchemaRevision(
          new BigInteger("3"),
          List.of(new TrStatement(
            """
              alter table example0 add col2 integer
                            """.trim()
          ))
        );

      final var expected = new TreeMap<>();
      expected.put(rev0.version(), rev0);
      expected.put(rev1.version(), rev1);
      expected.put(rev2.version(), rev2);
      expected.put(rev3.version(), rev3);

      assertEquals(expected, set.revisions());
    }
  }

  @Test
  public void testExample5()
    throws Exception
  {
    try (var stream = this.resourceOf("example-5.xml")) {
      final var set =
        this.parsers.parse(URI.create("urn:stdin"), stream);

      final var rev0 =
        new TrSchemaRevision(
          BigInteger.ZERO,
          List.of(
            new TrStatement("""
create table x (f0 integer, f1 bigint, f2 varchar(100), f3 double, f4 decimal)
""".trim()),
            new TrStatementParameterized(
              TrParameterReferences.of(
                new TrParameterReference(0, "number0"),
                new TrParameterReference(1, "number1"),
                new TrParameterReference(2, "string0"),
                new TrParameterReference(3, "number2"),
                new TrParameterReference(4, "number3")
              ),
              """
insert into x values (?, ?, ?, ?, ?)
                              """.trim(),
              PREPARED_STATEMENT)
          )
        );

      final var expected = new TreeMap<>();
      expected.put(rev0.version(), rev0);

      assertEquals(expected, set.revisions());
    }
  }

  @Test
  public void testExample6()
    throws Exception
  {
    try (var stream = this.resourceOf("example-6.xml")) {
      final var set =
        this.parsers.parse(URI.create("urn:stdin"), stream);

      final var rev0 =
        new TrSchemaRevision(
          BigInteger.ZERO,
          List.of(
            new TrStatement("""
create table x (f0 integer, f1 bigint, f2 varchar(100), f3 double, f4 decimal)
""".trim()),
            new TrStatementParameterized(
              TrParameterReferences.of(
                new TrParameterReference(0, "number0"),
                new TrParameterReference(1, "number1"),
                new TrParameterReference(2, "string0"),
                new TrParameterReference(3, "number2"),
                new TrParameterReference(4, "number3")
              ),
              """
insert into x values (%s, %s, %s, %s, %s)
                              """.trim(),
              STRING_FORMATTING)
          )
        );

      final var expected = new TreeMap<>();
      expected.put(rev0.version(), rev0);

      assertEquals(expected, set.revisions());
    }
  }

  @TestFactory
  public Stream<DynamicTest> testErrors()
  {
    return Stream.of(
      "error-0.xml",
      "error-1.xml",
      "error-2.xml",
      "error-3.xml",
      "error-4.xml")
      .map(name -> {
        return DynamicTest.dynamicTest(
          "testError_" + name,
          () -> this.testError(name));
      });
  }

  private void testError(
    final String name)
  {
    try (var stream = this.resourceOf(name)) {
      final var ex =
        assertThrows(ParsingException.class, () -> {
          this.parsers.parse(URI.create("urn:stdin"), stream);
        });
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private InputStream resourceOf(
    final String name)
    throws IOException
  {
    return TrTestDirectories.resourceStreamOf(
      TrSchemaRevisionSetParsersTest.class,
      this.directory,
      name
    );
  }
}
