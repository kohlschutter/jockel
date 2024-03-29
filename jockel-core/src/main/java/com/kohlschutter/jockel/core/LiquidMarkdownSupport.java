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
package com.kohlschutter.jockel.core;

import com.kohlschutter.dumbo.ConsoleSupport;
import com.kohlschutter.dumbo.annotations.FilterMapping;
import com.kohlschutter.dumbo.annotations.Filters;
import com.kohlschutter.dumbo.annotations.ServletMapping;
import com.kohlschutter.dumbo.annotations.Servlets;
import com.kohlschutter.dumbo.api.DumboComponent;
import com.kohlschutter.dumbo.appdefaults.AppDefaultsSupport;
import com.kohlschutter.dumbo.bootstrap.BootstrapSupport;
import com.kohlschutter.dumbo.ext.prism.PrismSupport;

import jakarta.servlet.DispatcherType;

@Servlets({ //
    @ServletMapping(map = "*.md", to = MarkdownServlet.class, initOrder = 1),
    //
})
@Filters({ //
    @FilterMapping(map = "*.html", to = HtmlJspFilter.class, initOrder = 1, //
        dispatcherTypes = {DispatcherType.REQUEST}), //
    @FilterMapping(map = {"*.txt", "*.json", "*.xml"}, to = LiquidFilter.class, initOrder = 2, //
        dispatcherTypes = {DispatcherType.REQUEST}), //
    @FilterMapping(map = {"*.css"}, to = CssFilter.class, initOrder = 3, //
        dispatcherTypes = {DispatcherType.REQUEST}), //
    @FilterMapping(map = {"*.scss"}, to = ScssFilter.class, initOrder = 4, //
        dispatcherTypes = {DispatcherType.REQUEST}), //
})
public interface LiquidMarkdownSupport extends DumboComponent, AppDefaultsSupport, BootstrapSupport,
    ConsoleSupport, PrismSupport {

}
