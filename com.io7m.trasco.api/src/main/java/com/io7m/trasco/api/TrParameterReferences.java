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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * A set of references to parameters.
 *
 * @param inOrder The parameters in the order that they will be supplied to a statement
 * @param byName  The parameters by name
 */

public record TrParameterReferences(
  SortedMap<Integer, TrParameterReference> inOrder,
  Map<String, TrParameterReference> byName)
{
  /**
   * A set of references to parameters.
   *
   * @param inOrder The parameters in the order that they will be supplied to a statement
   * @param byName  The parameters by name
   */

  public TrParameterReferences
  {
    final var ov = Set.copyOf(inOrder.values());
    final var nv = Set.copyOf(byName.values());
    if (!Objects.equals(ov, nv)) {
      throw new IllegalArgumentException(
        "Parameter reference maps must match.");
    }
  }

  /**
   * Construct a set of parameter references from the given list.
   *
   * @param parameters The list
   *
   * @return A parameter reference set
   */

  public static TrParameterReferences of(
    final List<TrParameterReference> parameters)
  {
    return new TrParameterReferences(
      new TreeMap<>(
        parameters.stream()
          .collect(Collectors.toMap(p -> Integer.valueOf(p.order()), p -> p))),
      parameters.stream()
        .collect(Collectors.toMap(TrParameterReference::name, p -> p))
    );
  }

  /**
   * Construct a set of parameter references from the given list.
   *
   * @param parameters The list
   *
   * @return A parameter reference set
   */

  public static TrParameterReferences of(
    final TrParameterReference... parameters)
  {
    return of(List.of(parameters));
  }
}
