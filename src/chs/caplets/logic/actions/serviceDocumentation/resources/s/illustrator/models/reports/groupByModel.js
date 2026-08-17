define("GroupByModel", [
    'jquery',
    'underscore',
    'backbone'
], function ($, _, Backbone)
{
    "use strict";
    var GroupByModel;
    GroupByModel = Backbone.Model.extend({
        defaults: {
            entries: {}
        },
        group: function (groupById, groupBySubId, mapColumnTo, mapInnerTableTo)
        {
            var groupByIdQC = "{" + groupById + "}", groupBySubIdQC = "{" + groupBySubId + "}";
            return _.chain(this.get('entries')).map(function (row, rIndex)
            {
                var grouped = _.chain(row).map(function (column, columnName)
                {
                    var modifiedColumn = $.extend(true, {}, column);
                    modifiedColumn.columnName = columnName;
                    return modifiedColumn;
                }).filter(function (column, cIndex)
                {
                    return column.columnName !== groupById && column.columnName !== groupBySubId &&
                            column.columnName !== groupByIdQC && column.columnName !== groupBySubIdQC;
                }).map(function (column, columnName)
                {
                    if (column.value && column.value.data) {
                        return mapInnerTableTo.call(column, column, columnName);
                    }
                    else {
                        return mapColumnTo.call(column, column, columnName);
                    }
                }).filter(function (column, cIndex)
                {
                    return column != null;
                }).reduce(function (memo, modifiedColumn)
                {
                    var groupKey, subGroupKey, key;
                    groupKey = row[groupById] ? row[groupById].value : (row[groupByIdQC] ? row[groupByIdQC].value : '');
                    subGroupKey = row[groupBySubId] ? row[groupBySubId].value :
                            (row[groupBySubIdQC] ? row[groupBySubIdQC].value : '');
                    key = groupKey + " " + subGroupKey;
                    if (!memo[key]) {
                        memo[key] = [];
                    }
                    var group = memo[key];
                    // group = _.union(group, modifiedColumn); modifiedColumn is an object and _.union now only work with Array after version 1.7.0
                    group = _.union(group, modifiedColumn instanceof Array ? modifiedColumn : [modifiedColumn]);
                    memo[key] = group;
                    return memo;
                }, {});
                return grouped.value();
            }).reduce(function (memo, group)
            {
                Object.keys(group).forEach(function (key)
                {
                    memo[key] = group[key];
                });
                return memo;
            }, {}).value();
        }
    });
    return GroupByModel;
});
