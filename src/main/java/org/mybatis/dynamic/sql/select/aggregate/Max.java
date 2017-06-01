/**
 *    Copyright 2016-2017 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.dynamic.sql.select.aggregate;

import org.mybatis.dynamic.sql.SqlColumn;

public class Max<T> extends SqlColumn<T> {

    private Max(SqlColumn<T> column) {
        super(column);
    }

    @Override
    public String name() {
        return "max(" + super.name() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public String nameIncludingTableAlias() {
        return tableAlias().map(a -> "max(" + a + "." + name + ")").orElse(name()); //$NON-NLS-1$
    }

    public static <T> Max<T> max(SqlColumn<T> column) {
        return new Max<>(column);
    }
}