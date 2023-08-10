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
import com.io7m.trasco.api.TrParameterReference;
import com.io7m.trasco.api.TrParameterReferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.io7m.trasco.vanilla.internal.v1.TrV1.element;

/**
 * A V1 schema parser.
 */

public final class TrV1ParameterReferencesDeclParser
  implements BTElementHandlerType<TrParameterReference, TrParameterReferences>
{
  private final SortedMap<Integer, TrParameterReference> inOrder;
  private final Map<String, TrParameterReference> byName;

  /**
   * A V1 schema parser.
   *
   * @param context A context
   */

  public TrV1ParameterReferencesDeclParser(
    final BTElementParsingContextType context)
  {
    this.inOrder = new TreeMap<>();
    this.byName = new HashMap<>();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ? extends TrParameterReference>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(
        element("ParameterReference"),
        TrV1ParameterReferenceDeclParser::new
      )
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final TrParameterReference result)
  {
    this.inOrder.put(Integer.valueOf(result.order()), result);
    this.byName.put(result.name(), result);
  }

  @Override
  public TrParameterReferences onElementFinished(
    final BTElementParsingContextType context)
  {
    return new TrParameterReferences(
      Collections.unmodifiableSortedMap(this.inOrder),
      Map.copyOf(this.byName)
    );
  }
}
