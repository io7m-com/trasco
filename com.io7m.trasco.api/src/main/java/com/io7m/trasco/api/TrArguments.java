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


package com.io7m.trasco.api;

import com.io7m.seltzer.api.SStructuredError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.io7m.trasco.api.TrErrorCode.ARGUMENT_ERRORS;
import static com.io7m.trasco.api.TrErrorCode.ARGUMENT_MISSING;
import static com.io7m.trasco.api.TrErrorCode.ARGUMENT_TYPE_ERROR;

/**
 * A set of arguments.
 *
 * @param arguments The arguments
 */

public record TrArguments(
  Map<String, TrArgumentType> arguments)
{
  private static final TrArguments INSTANCE =
    new TrArguments(Map.of());

  /**
   * @return The empty set of arguments
   */

  public static TrArguments empty()
  {
    return INSTANCE;
  }

  /**
   * A set of arguments.
   *
   * @param arguments The arguments
   */

  public TrArguments
  {
    Objects.requireNonNull(arguments, "arguments");
  }

  /**
   * Check that these arguments satisfy the given parameters.
   *
   * @param parameters The parameters
   *
   * @throws TrException On failure
   */

  public void checkSatisfies(
    final Map<String, TrParameter> parameters)
    throws TrException
  {
    final var errors =
      new ArrayList<SStructuredError<TrErrorCode>>();

    for (final var name : parameters.keySet()) {
      final var parameter = parameters.get(name);
      final var argument = this.arguments.get(name);

      if (argument == null) {
        errors.add(
          SStructuredError.builder(
              ARGUMENT_MISSING, "No argument provided for parameter.")
            .withAttribute("Parameter Name", name)
            .withAttribute("Parameter Type", parameter.kind().toString())
            .build()
        );
        continue;
      }

      if (argument.type() != parameter.kind()) {
        errors.add(
          SStructuredError.builder(
              ARGUMENT_TYPE_ERROR, "Incorrect argument type.")
            .withAttribute("Parameter Name", name)
            .withAttribute("Parameter Type", parameter.kind().toString())
            .withAttribute("Argument Type", argument.type().toString())
            .build()
        );
        continue;
      }
    }

    if (!errors.isEmpty()) {
      throw new TrException(
        "One or more parameter/argument errors were found.",
        ARGUMENT_ERRORS,
        List.copyOf(errors)
      );
    }
  }
}
