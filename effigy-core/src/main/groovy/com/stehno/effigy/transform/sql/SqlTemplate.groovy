/*
 * Copyright (c) 2015 Christopher J. Stehno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stehno.effigy.transform.sql

import groovy.transform.ToString

/**
 * Created by cjstehno on 9/29/15.
 */
@ToString(includeNames = true, includeFields = true)
class SqlTemplate {

    private static final VARIABLE_PATTERN = /:[A-Za-z0-9]*/
    private final String sql

    SqlTemplate(final String sql) {
        this.sql = sql
    }

    /*
        supports
            named variables "select a,b from foo where x=:someX"
            properties "select a,b from foo where x=:{bar.x * 10}"
            ordinal "select a,b from foo where x=? and y=?"

            names must match args or Param annot values
            ordinal must match method arg order or Param annot values
     */
}
