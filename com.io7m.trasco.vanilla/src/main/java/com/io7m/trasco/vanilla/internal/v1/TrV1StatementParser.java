/*
 * Copyright © 2021 Mark Raynsford <code@io7m.com> http://io7m.com
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

package com.io7m.trasco.vanilla.internal.v1;

import com.io7m.blackthorne.api.BTElementHandlerType;
import com.io7m.blackthorne.api.BTElementParsingContextType;

/**
 * A statement parser.
 */

public final class TrV1StatementParser
  implements BTElementHandlerType<Object, String>
{
  private final StringBuilder text;

  /**
   * A statement parser.
   *
   * @param context The context
   */

  public TrV1StatementParser(
    final BTElementParsingContextType context)
  {
    this.text = new StringBuilder(128);
  }

  @Override
  public void onCharacters(
    final BTElementParsingContextType context,
    final char[] data,
    final int offset,
    final int length)
  {
    this.text.append(data, offset, length);
  }

  @Override
  public String onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.text.toString().trim();
  }
}
