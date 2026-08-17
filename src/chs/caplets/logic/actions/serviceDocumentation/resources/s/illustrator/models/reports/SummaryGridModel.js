define("SummaryGridModel", [
    'jquery',
    'underscore',
    'backbone',
    'illustrator/models/reports/gridModel',
    'GroupByModel'
], function ($, _, Backbone, GridModel, GroupByModel)
{
    "use strict";
    var SummaryGridModel;
    SummaryGridModel = GridModel.extend({
        initialize: function ()
        {
            var rows = this.get(
                    'tableData').data.entries, groupByColumnName = 'Name', groupSubHeaderColumn = 'ShortDescription', mergedGroups,
                    columns, table, transpose, transposedRows, mapColumnTo, mapInnerTableTo, groupByModel;
            mapColumnTo = function (column, columnName)
            {
                column.value = column.columnName + ": " + column.value;
                return column;
            };
            mapInnerTableTo = function (column, columnName)
            {
                var innerTableRows = column.value.data.entries, innerTableGroupByModel, mergedGroupsOfInnerTable, innerTable, groupedRows;
                innerTableGroupByModel = new GroupByModel({entries: innerTableRows});
                mergedGroupsOfInnerTable =
                        innerTableGroupByModel.group(groupByColumnName, groupSubHeaderColumn,
                                function (column, columnName)
                                {
                                    return column;
                                }, mapInnerTableTo);
                innerTable = _.map(mergedGroupsOfInnerTable, function (group, groupId)
                {
                    var reducedGroup = _.reduce(group, function (acc, item)
                    {
                        return acc.length == 0 ? (item.value) : (acc + ',' + item.value);
                    }, '');
                    var groupedContent = groupId + "(" + reducedGroup + ")";
                    var reducedRow = {};
                    reducedRow[column.columnName] = {"value": groupedContent};
                    return reducedRow;
                }, []);
                column.value.data.items = innerTable;
                column.value.data.cols = [column.columnName];
                return column;
            };
            groupByModel = new GroupByModel({entries: rows});
            mergedGroups = groupByModel.group(groupByColumnName, groupSubHeaderColumn, mapColumnTo, mapInnerTableTo);
            columns = Object.keys(mergedGroups);
            table = _.map(mergedGroups, _.values);
            transpose = _.zip.apply(_, table);
            transposedRows = _.filter(transpose, function (row)
            {
                return _.some(row, function (c)
                {
                    if (c.value && c.value.data) {
                        return c.value.data.entries.length > 0;
                    }
                    return true;

                });
            }).map(function (row)
            {
                var map = _.object(_.map(row, function (item, index)
                {
                    return [columns[index], item];
                }));
                return map;
            });
            this.set("sortable", false);
            this.set("searchable", false);
            this.get('tableData').cols = columns;
            this.get('tableData').items = transposedRows;
            this.set('container', "#" + this.get('tableData').id + ">.change-table");
        }
    });
    return SummaryGridModel;
});

