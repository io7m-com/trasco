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

import com.io7m.blackthorne.core.BTElementHandlerConstructorType;
import com.io7m.blackthorne.core.BTElementHandlerType;
import com.io7m.blackthorne.core.BTElementParsingContextType;
import com.io7m.blackthorne.core.BTQualifiedName;
import com.io7m.trasco.api.TrParameter;
import com.io7m.trasco.api.TrSchemaRevision;
import com.io7m.trasco.api.TrSchemaRevisionSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.io7m.trasco.vanilla.internal.v1.TrV1.element;
import static java.util.function.Function.identity;

/**
 * A parser for database schemas.
 */

public final class TrV1SchemaDeclSetParser
  implements BTElementHandlerType<Object, TrSchemaRevisionSet>
{
  private final List<TrSchemaRevision> revisions;
  private final HashMap<String, TrParameter> parameters;

  /**
   * A parser for database schemas.
   *
   * @param context The context
   */

  public TrV1SchemaDeclSetParser(
    final BTElementParsingContextType context)
  {
    this.revisions = new ArrayList<>();
    this.parameters = new HashMap<>();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(
        element("Parameters"),
        TrV1ParametersDeclParser::new
      ),
      Map.entry(
        element("Schema"),
        TrV1SchemaDeclParser::new
      )
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
  {
    if (result instanceof final TrSchemaRevision revision) {
      this.revisions.add(revision);
      return;
    }

    if (result instanceof final Map<?, ?> map) {
      this.parameters.putAll((Map<? extends String, ? extends TrParameter>) map);
      return;
    }

    throw new IllegalArgumentException("Unexpected: %s".formatted(result));
  }

  @Override
  public TrSchemaRevisionSet onElementFinished(
    final BTElementParsingContextType context)
  {
    this.revisions.sort(TrSchemaRevision::compareTo);

    return new TrSchemaRevisionSet(
      Map.copyOf(this.parameters),
      new TreeMap<>(
        this.revisions.stream()
          .collect(Collectors.toMap(TrSchemaRevision::version, identity()))
      )
    );
  }
}
