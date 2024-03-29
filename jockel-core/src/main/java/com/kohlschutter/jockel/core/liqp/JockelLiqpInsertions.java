/*
 * jockel
 *
 * Copyright 2024 Christian Kohlschütter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kohlschutter.jockel.core.liqp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import liqp.Insertion;
import liqp.blocks.Capture;
import liqp.blocks.Case;
import liqp.blocks.Comment;
import liqp.blocks.Cycle;
import liqp.blocks.For;
import liqp.blocks.If;
import liqp.blocks.Ifchanged;
import liqp.blocks.Raw;
import liqp.blocks.Tablerow;
import liqp.blocks.Unless;
import liqp.tags.Assign;
import liqp.tags.Break;
import liqp.tags.Continue;
import liqp.tags.Decrement;
import liqp.tags.Increment;

public final class JockelLiqpInsertions {
  private static final Map<String, Insertion> INSERTIONS = new HashMap<>();
  private static final Map<String, Insertion> INSERTIONS_READONLY = Collections.unmodifiableMap(
      INSERTIONS);

  static {
    // Register all standard insertions.
    registerInsertion(new Assign());
    registerInsertion(new Break());
    registerInsertion(new Capture());
    registerInsertion(new Case());
    registerInsertion(new Comment());
    registerInsertion(new Continue());
    registerInsertion(new Cycle());
    registerInsertion(new Decrement());
    registerInsertion(new For());
    registerInsertion(new If());
    registerInsertion(new Ifchanged());

    // registerInsertion(new Include());
    registerInsertion(new JockelIncludeTag());

    registerInsertion(new Increment());
    registerInsertion(new Raw());
    registerInsertion(new Tablerow());
    registerInsertion(new Unless());
  }

  private JockelLiqpInsertions() {
    throw new IllegalStateException("No instances");
  }

  /**
   * Registers a new insertion.
   *
   * @param insertion the insertion to be registered.
   */
  private static void registerInsertion(Insertion insertion) {
    INSERTIONS.put(insertion.name, insertion);
  }

  public static Map<String, Insertion> getInsertions() {
    return INSERTIONS_READONLY;
  }
}
