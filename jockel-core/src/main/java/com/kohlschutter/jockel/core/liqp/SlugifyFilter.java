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

import java.util.Locale;

import liqp.TemplateContext;
import liqp.filters.Filter;

// FIXME: Support slugify modes (none, raw, default, pretty, ascii, latin)
// see https://jekyllrb.com/docs/liquid/filters/
public class SlugifyFilter extends Filter {
  public SlugifyFilter() {
    super("slugify");
  }

  @Override
  public Object apply(Object value, TemplateContext context, Object... params) {
    String content = super.asString(value, context);

    if (content.isEmpty()) {
      return content;
    }

    content = content.replace(' ', '-').toLowerCase(Locale.ENGLISH);

    return content;
  }
}
