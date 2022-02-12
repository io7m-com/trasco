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
import java.util.NavigableMap;
import java.util.Objects;

/**
 * A set of schema revisions.
 *
 * @param revisions The revisions
 */

public record TrSchemaRevisionSet(
  NavigableMap<BigInteger, TrSchemaRevision> revisions)
{
  /**
   * A set of schema revisions.
   *
   * @param revisions The revisions
   */

  public TrSchemaRevisionSet
  {
    Objects.requireNonNull(revisions, "revisions");

    if (revisions.size() >= 2) {
      BigInteger previous = null;
      for (final var current : revisions.keySet()) {
        if (previous != null) {
          if (!current.subtract(previous).equals(BigInteger.ONE)) {
            throw new IllegalArgumentException(
              String.format(
                "Revision versions must always increment by 1 (received %s followed by %s)",
                previous,
                current
              ));
          }
        }
        previous = current;
      }
    }
  }
}
