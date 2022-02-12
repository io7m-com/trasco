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
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.trasco.api.TrSchemaRevision;
import org.xml.sax.Attributes;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.io7m.trasco.vanilla.internal.v1.TrV1.element;

/**
 * A V1 schema parser.
 */

public final class TrV1SchemaDeclParser
  implements BTElementHandlerType<Object, TrSchemaRevision>
{
  private final ArrayList<String> statements;
  private BigInteger versionCurrent;

  /**
   * A V1 schema parser.
   *
   * @param context A context
   */

  public TrV1SchemaDeclParser(
    final BTElementParsingContextType context)
  {
    this.statements = new ArrayList<>();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(
        element("Statement"),
        TrV1StatementParser::new
      ),
      Map.entry(
        element("Comment"),
        TrV1CommentParser::new
      )
    );
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
  {
    this.versionCurrent =
      new BigInteger(attributes.getValue("versionCurrent"));
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
  {
    if (result instanceof String) {
      final var trimmed = ((String) result).trim();
      if (trimmed.isBlank() || trimmed.isEmpty()) {
        return;
      }
      this.statements.add(trimmed);
      return;
    }
    if (result instanceof TrV1Comment) {
      return;
    }

    throw new UnreachableCodeException();
  }

  @Override
  public TrSchemaRevision onElementFinished(
    final BTElementParsingContextType context)
  {
    return new TrSchemaRevision(
      this.versionCurrent,
      List.copyOf(this.statements)
    );
  }
}
