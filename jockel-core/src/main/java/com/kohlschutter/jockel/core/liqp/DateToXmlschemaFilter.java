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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.kohlschutter.dumbo.util.SuppliedThreadLocal;

import liqp.TemplateContext;
import liqp.filters.Filter;

public class DateToXmlschemaFilter extends Filter {
  private static final liqp.filters.Date LIQP_DATE_FILTER = new liqp.filters.Date() {
  };
  private static final SimpleDateFormat[] PARSERS = {
      new SimpleDateFormat("yyyy-MM-dd hh:mm:ss zzz", Locale.ENGLISH), //
      new SimpleDateFormat("yyyy-MM-dd hh:mm zzz", Locale.ENGLISH), //
      new SimpleDateFormat("yyyy-MM-dd hh:mm:ss XXX", Locale.ENGLISH), //
      new SimpleDateFormat("yyyy-MM-dd hh:mm XXX", Locale.ENGLISH), //
  };
  private static final ThreadLocal<SimpleDateFormat> TL_ISO_8601 = SuppliedThreadLocal.of(
      () -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH));

  public DateToXmlschemaFilter() {
    super("date_to_xmlschema");
  }

  @Override
  public Object apply(Object value, TemplateContext context, Object... params) {
    if (value == null) {
      return null;
    }
    String date = LIQP_DATE_FILTER.apply(value, context, new Object[0]).toString();

    Date d = null;
    for (SimpleDateFormat parser : PARSERS) {
      try {
        d = parser.parse(date);
        break;
      } catch (ParseException e) {
        continue;
      }
    }
    if (d == null) {
      throw new IllegalStateException("Cannot parse date: " + date);
    }

    return TL_ISO_8601.get().format(d);
  }
}
