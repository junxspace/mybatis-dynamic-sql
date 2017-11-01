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
package org.mybatis.dynamic.sql.select;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mybatis.dynamic.sql.SelectListItem;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlCriterion;
import org.mybatis.dynamic.sql.SqlTable;
import org.mybatis.dynamic.sql.VisitableCondition;
import org.mybatis.dynamic.sql.select.join.JoinCondition;
import org.mybatis.dynamic.sql.select.join.JoinCriterion;
import org.mybatis.dynamic.sql.select.join.JoinModel;
import org.mybatis.dynamic.sql.select.join.JoinSpecification;
import org.mybatis.dynamic.sql.select.join.JoinType;
import org.mybatis.dynamic.sql.where.AbstractWhereModelBuilder;
import org.mybatis.dynamic.sql.where.WhereModel;

public class SelectModelBuilder {

    private boolean isDistinct;
    private List<SelectListItem> selectList;
    private SqlTable table;
    private Map<SqlTable, String> tableAliases = new HashMap<>();
    private WhereModel whereModel;
    private OrderByModel orderByModel;
    private JoinModel joinModel;
    private List<JoinSpecification> joinSpecifications = new ArrayList<>();
    
    private SelectModelBuilder(SelectListItem...selectList) {
        this.selectList = Arrays.asList(selectList);
    }
    
    public SelectSupportAfterFromBuilder from(SqlTable table) {
        this.table = table;
        return new SelectSupportAfterFromBuilder();
    }

    public SelectSupportAfterFromBuilder from(SqlTable table, String tableAlias) {
        this.table = table;
        tableAliases.put(table, tableAlias);
        return new SelectSupportAfterFromBuilder();
    }

    public static SelectModelBuilder of(SelectListItem...selectList) {
        return new SelectModelBuilder(selectList);
    }
    
    public static SelectModelBuilder ofDistinct(SelectListItem...selectList) {
        SelectModelBuilder builder = SelectModelBuilder.of(selectList);
        builder.isDistinct = true;
        return builder;
    }
    
    protected SelectModel buildModel() {
        return new SelectModel.Builder(table)
                .isDistinct(isDistinct)
                .withColumns(selectList)
                .withTableAliases(tableAliases)
                .withWhereModel(whereModel)
                .withOrderByModel(orderByModel)
                .withJoinModel(joinModel)
                .build();
    }
    
    @FunctionalInterface
    public interface Buildable {
        SelectModel build();
    }
    
    public class SelectSupportAfterFromBuilder implements Buildable {
        private SelectSupportAfterFromBuilder() {
            super();
        }
        
        public <T> SelectSupportWhereBuilder where(SqlColumn<T> column, VisitableCondition<T> condition) {
            return new SelectSupportWhereBuilder(column, condition);
        }

        public <T> SelectSupportWhereBuilder where(SqlColumn<T> column, VisitableCondition<T> condition,
                SqlCriterion<?>...subCriteria) {
            return new SelectSupportWhereBuilder(column, condition, subCriteria);
        }
        
        public SelectSupportAfterOrderByBuilder orderBy(SqlColumn<?>...columns) {
            orderByModel = OrderByModel.of(Arrays.asList(columns));
            return new SelectSupportAfterOrderByBuilder();
        }
        
        @Override
        public SelectModel build() {
            return buildModel();
        }

        public JoinSpecificationStarter join(SqlTable joinTable) {
            return new JoinSpecificationStarter(joinTable, JoinType.INNER);
        }
        
        public JoinSpecificationStarter join(SqlTable joinTable, String tableAlias) {
            tableAliases.put(joinTable, tableAlias);
            return join(joinTable);
        }

        public JoinSpecificationStarter leftJoin(SqlTable joinTable) {
            return new JoinSpecificationStarter(joinTable, JoinType.LEFT);
        }
        
        public JoinSpecificationStarter leftJoin(SqlTable joinTable, String tableAlias) {
            tableAliases.put(joinTable, tableAlias);
            return leftJoin(joinTable);
        }

        public JoinSpecificationStarter rightJoin(SqlTable joinTable) {
            return new JoinSpecificationStarter(joinTable, JoinType.RIGHT);
        }
        
        public JoinSpecificationStarter rightJoin(SqlTable joinTable, String tableAlias) {
            tableAliases.put(joinTable, tableAlias);
            return rightJoin(joinTable);
        }

        public JoinSpecificationStarter fullJoin(SqlTable joinTable) {
            return new JoinSpecificationStarter(joinTable, JoinType.FULL);
        }
        
        public JoinSpecificationStarter fullJoin(SqlTable joinTable, String tableAlias) {
            tableAliases.put(joinTable, tableAlias);
            return fullJoin(joinTable);
        }
    }
    
    public class SelectSupportWhereBuilder extends AbstractWhereModelBuilder<SelectSupportWhereBuilder> 
            implements Buildable {
        private <T> SelectSupportWhereBuilder(SqlColumn<T> column, VisitableCondition<T> condition) {
            super(column, condition);
        }
        
        private <T> SelectSupportWhereBuilder(SqlColumn<T> column, VisitableCondition<T> condition,
                SqlCriterion<?>...subCriteria) {
            super(column, condition, subCriteria);
        }
        
        public SelectSupportAfterOrderByBuilder orderBy(SqlColumn<?>...columns) {
            whereModel = buildWhereModel();
            orderByModel = OrderByModel.of(Arrays.asList(columns));
            return new SelectSupportAfterOrderByBuilder();
        }
        
        @Override
        public SelectModel build() {
            whereModel = buildWhereModel();
            return buildModel();
        }
        
        @Override
        protected SelectSupportWhereBuilder getThis() {
            return this;
        }
    }
    
    public class JoinSpecificationStarter {
        private SqlTable joinTable;
        private JoinType joinType;
        
        public JoinSpecificationStarter(SqlTable joinTable, JoinType joinType) {
            this.joinTable = joinTable;
            this.joinType = joinType;
        }

        public <T> JoinSpecificationFinisher on(SqlColumn<T> joinColumn, JoinCondition<T> joinCondition) {
            return new JoinSpecificationFinisher(joinTable, joinColumn, joinCondition, joinType);
        }

        public <T> JoinSpecificationFinisher on(SqlColumn<T> joinColumn, JoinCondition<T> joinCondition,
                JoinCriterion<?>...joinCriteria) {
            return new JoinSpecificationFinisher(joinTable, joinColumn, joinCondition, joinType, joinCriteria);
        }
    }

    public class JoinSpecificationFinisher implements Buildable {

        private SqlTable joinTable;
        private List<JoinCriterion<?>> joinCriteria = new ArrayList<>();
        private JoinType joinType;
        
        public <T> JoinSpecificationFinisher(SqlTable table, SqlColumn<T> joinColumn,
                JoinCondition<T> joinCondition, JoinType joinType) {
            this.joinTable = table;
            this.joinType = joinType;

            JoinCriterion<T> joinCriterion = new JoinCriterion.Builder<T>()
                    .withJoinColumn(joinColumn)
                    .withJoinCondition(joinCondition)
                    .withConnector("on") //$NON-NLS-1$
                    .build();
            
            joinCriteria.add(joinCriterion);
        }

        public <T> JoinSpecificationFinisher(SqlTable table, SqlColumn<T> joinColumn,
                JoinCondition<T> joinCondition, JoinType joinType, JoinCriterion<?>...joinCriteria) {
            this.joinTable = table;
            this.joinType = joinType;

            JoinCriterion<T> joinCriterion = new JoinCriterion.Builder<T>()
                    .withJoinColumn(joinColumn)
                    .withJoinCondition(joinCondition)
                    .withConnector("on") //$NON-NLS-1$
                    .build();
            
            this.joinCriteria.add(joinCriterion);
            this.joinCriteria.addAll(Arrays.asList(joinCriteria));
        }
        
        protected JoinSpecification buildJoinSpecification() {
            return new JoinSpecification.Builder()
                    .withJoinCriteria(joinCriteria)
                    .withJoinTable(joinTable)
                    .withJoinType(joinType)
                    .build();
        }
        
        protected JoinModel buildJoinModel() {
            joinSpecifications.add(buildJoinSpecification());
            return JoinModel.of(joinSpecifications);
        }
        
        @Override
        public SelectModel build() {
            joinModel = buildJoinModel();
            return buildModel();
        }
        
        public <T> SelectSupportWhereBuilder where(SqlColumn<T> column, VisitableCondition<T> condition) {
            joinModel = buildJoinModel();
            return new SelectSupportWhereBuilder(column, condition);
        }

        public <T> SelectSupportWhereBuilder where(SqlColumn<T> column, VisitableCondition<T> condition,
                SqlCriterion<?>...subCriteria) {
            joinModel = buildJoinModel();
            return new SelectSupportWhereBuilder(column, condition, subCriteria);
        }

        public SelectSupportAfterOrderByBuilder orderBy(SqlColumn<?>...columns) {
            joinModel = buildJoinModel();
            orderByModel = OrderByModel.of(Arrays.asList(columns));
            return new SelectSupportAfterOrderByBuilder();
        }

        public <T> JoinSpecificationFinisher and(SqlColumn<T> joinColumn, JoinCondition<T> joinCondition) {
            JoinCriterion<T> joinCriterion = new JoinCriterion.Builder<T>()
                    .withJoinColumn(joinColumn)
                    .withJoinCondition(joinCondition)
                    .withConnector("and") //$NON-NLS-1$
                    .build();
            this.joinCriteria.add(joinCriterion);
            return this;
        }

        public JoinSpecificationStarter join(SqlTable joinTable) {
            joinSpecifications.add(buildJoinSpecification());
            return new JoinSpecificationStarter(joinTable, JoinType.INNER);
        }
        
        public JoinSpecificationStarter join(SqlTable joinTable, String tableAlias) {
            tableAliases.put(joinTable, tableAlias);
            return join(joinTable);
        }

        public JoinSpecificationStarter leftJoin(SqlTable joinTable) {
            joinSpecifications.add(buildJoinSpecification());
            return new JoinSpecificationStarter(joinTable, JoinType.LEFT);
        }
        
        public JoinSpecificationStarter leftJoin(SqlTable joinTable, String tableAlias) {
            tableAliases.put(joinTable, tableAlias);
            return leftJoin(joinTable);
        }

        public JoinSpecificationStarter rightJoin(SqlTable joinTable) {
            joinSpecifications.add(buildJoinSpecification());
            return new JoinSpecificationStarter(joinTable, JoinType.RIGHT);
        }
        
        public JoinSpecificationStarter rightJoin(SqlTable joinTable, String tableAlias) {
            tableAliases.put(joinTable, tableAlias);
            return rightJoin(joinTable);
        }

        public JoinSpecificationStarter fullJoin(SqlTable joinTable) {
            joinSpecifications.add(buildJoinSpecification());
            return new JoinSpecificationStarter(joinTable, JoinType.FULL);
        }
        
        public JoinSpecificationStarter fullJoin(SqlTable joinTable, String tableAlias) {
            tableAliases.put(joinTable, tableAlias);
            return fullJoin(joinTable);
        }
    }
    
    public class SelectSupportAfterOrderByBuilder implements Buildable {
        private SelectSupportAfterOrderByBuilder() {
            super();
        }
        
        @Override
        public SelectModel build() {
            return buildModel();
        }
    }
}
