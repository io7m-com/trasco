/*
 * Copyright © 2023 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import com.io7m.trasco.api.TrArgumentNumeric;
import com.io7m.trasco.api.TrArgumentString;
import com.io7m.trasco.api.TrArguments;
import com.io7m.trasco.api.TrException;
import com.io7m.trasco.api.TrParameter;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.io7m.trasco.api.TrErrorCode.ARGUMENT_MISSING;
import static com.io7m.trasco.api.TrErrorCode.ARGUMENT_TYPE_ERROR;
import static com.io7m.trasco.api.TrParameterKind.NUMERIC;
import static com.io7m.trasco.api.TrParameterKind.STRING;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class TrArgumentsTest
{
  @Test
  public void testEmptySatisfiesEmpty()
    throws TrException
  {
    final var arguments =
      TrArguments.empty();
    final var parameters =
      Map.<String, TrParameter>of();

    arguments.checkSatisfies(parameters);
  }

  @Test
  public void testMissing()
    throws TrException
  {
    final var arguments =
      TrArguments.empty();
    final var parameters =
      Map.ofEntries(
        entry("x", new TrParameter("x", STRING)),
        entry("y", new TrParameter("y", STRING))
      );

    final var ex =
      assertThrows(TrException.class, () -> {
        arguments.checkSatisfies(parameters);
      });

    assertEquals(
      ARGUMENT_MISSING, ex.extraErrors().get(0).errorCode()
    );
    assertEquals(
      ARGUMENT_MISSING, ex.extraErrors().get(1).errorCode()
    );
  }

  @Test
  public void testWrongTypeString()
    throws TrException
  {
    final var arguments =
      new TrArguments(
        Map.ofEntries(
          entry("x", new TrArgumentNumeric("x", 23)),
          entry("y", new TrArgumentNumeric("y", 23))
        )
      );
    final var parameters =
      Map.ofEntries(
        entry("x", new TrParameter("x", STRING)),
        entry("y", new TrParameter("y", STRING))
      );

    final var ex =
      assertThrows(TrException.class, () -> {
        arguments.checkSatisfies(parameters);
      });

    assertEquals(
      ARGUMENT_TYPE_ERROR, ex.extraErrors().get(0).errorCode()
    );
    assertEquals(
      ARGUMENT_TYPE_ERROR, ex.extraErrors().get(1).errorCode()
    );
  }

  @Test
  public void testWrongTypeNumeric()
    throws TrException
  {
    final var arguments =
      new TrArguments(
        Map.ofEntries(
          entry("x", new TrArgumentString("x", "23")),
          entry("y", new TrArgumentString("y", "23"))
        )
      );
    final var parameters =
      Map.ofEntries(
        entry("x", new TrParameter("x", NUMERIC)),
        entry("y", new TrParameter("y", NUMERIC))
      );

    final var ex =
      assertThrows(TrException.class, () -> {
        arguments.checkSatisfies(parameters);
      });

    assertEquals(
      ARGUMENT_TYPE_ERROR, ex.extraErrors().get(0).errorCode()
    );
    assertEquals(
      ARGUMENT_TYPE_ERROR, ex.extraErrors().get(1).errorCode()
    );
  }
}
