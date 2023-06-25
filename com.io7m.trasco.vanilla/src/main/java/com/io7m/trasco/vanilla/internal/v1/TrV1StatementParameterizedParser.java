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

import com.io7m.blackthorne.api.BTElementHandlerConstructorType;
import com.io7m.blackthorne.api.BTElementHandlerType;
import com.io7m.blackthorne.api.BTElementParsingContextType;
import com.io7m.blackthorne.api.BTQualifiedName;
import com.io7m.trasco.api.TrParameterInterpolation;
import com.io7m.trasco.api.TrParameterReferences;
import com.io7m.trasco.api.TrStatement;
import com.io7m.trasco.api.TrStatementParameterized;
import org.xml.sax.Attributes;

import java.util.Map;
import java.util.Objects;

import static com.io7m.trasco.vanilla.internal.v1.TrV1.element;

/**
 * A parameterized statement parser.
 */

public final class TrV1StatementParameterizedParser
  implements BTElementHandlerType<Object, TrStatementParameterized>
{
  private final StringBuilder text;
  private TrParameterReferences parameters;
  private TrParameterInterpolation interpolation;

  /**
   * A statement parser.
   *
   * @param context The context
   */

  public TrV1StatementParameterizedParser(
    final BTElementParsingContextType context)
  {
    this.parameters = TrParameterReferences.of();
    this.text = new StringBuilder(128);
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(
        element("ParameterReferences"),
        TrV1ParameterReferencesDeclParser::new
      ),
      Map.entry(
        element("Text"),
        TrV1StatementParser::new
      )
    );
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
  {
    this.interpolation =
      TrParameterInterpolation.valueOf(
        Objects.requireNonNullElse(
          attributes.getValue("parameterInterpolation"),
          TrParameterInterpolation.PREPARED_STATEMENT.name()
        )
      );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
  {
    if (result instanceof final TrStatement st) {
      this.text.append(st.text());
      return;
    }
    if (result instanceof final TrParameterReferences refs) {
      this.parameters = refs;
      return;
    }

    throw new IllegalArgumentException("Unexpected: %s".formatted(result));
  }

  @Override
  public TrStatementParameterized onElementFinished(
    final BTElementParsingContextType context)
  {
    return new TrStatementParameterized(
      this.parameters,
      this.text.toString().trim(),
      this.interpolation
    );
  }
}
