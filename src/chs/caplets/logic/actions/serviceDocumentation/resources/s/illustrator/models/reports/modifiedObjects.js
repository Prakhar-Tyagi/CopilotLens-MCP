/*global define, mentor, _*/
define([
    'underscore',
    'backbone',
    'illustrator/models/reports/gridModel'
], function (underscore, Backbone, GridModel)
{
    "use strict";
    var ModifiedObjectsModel, p = mentor.publisher;
    ModifiedObjectsModel = GridModel.extend({
        groupByAttributeType: function (rows, filterMethod)
        {
            var objects = [];
            for (var i = 0; i < rows.length; i = i + 2) {
                if (rows.hasOwnProperty(i)) {
                    var row = rows[i];
                    var rowOld = rows[i + 1];
                    for (var prop in row) {
                        if (row.hasOwnProperty(prop)) {
                            var oldPropValue = rowOld[prop];
                            row['old-' + prop] = oldPropValue;
                        }
                    }
                    objects.push(row);
                }
            }
            return objects;
        },
        initialize: function ()
        {
            this.get('tableData').cols = this.get('tableData').layout.attributes['column-names'];
            this.get('tableData').items =
                    this.groupByAttributeType(this.get('tableData').data.entries);
            this.set('container', "#" + this.get('tableData').id + ">.change-table");
        },

        getOldColName: function (column)
        {
            return "old-" + column;
        }, matchWithCols: function (filteringText, column, item)
        {
            var newValColMatch = ModifiedObjectsModel.__super__.matchWithCols(filteringText, column, item);
            var oldValColMatch = ModifiedObjectsModel.__super__.matchWithCols(filteringText, this.getOldColName(column),
                    item);
            return newValColMatch || oldValColMatch;
        },

        filter: function (visibleIndices, colName, filterText)
        {
            return _.filter(visibleIndices, function (index)
            {
                var item = this.getTableData().items[index];
                if (item[colName].value && item[colName].value.data) {
                    return true;
                }

                if (item[this.getOldColName(colName)].value && item[this.getOldColName(colName)].value.data) {
                    return true;
                }
                var itemValue1 = (Utils.translate(item[colName].value) || "").toLowerCase();
                var itemValue2 = (Utils.translate(item[this.getOldColName(colName)].value) || "").toLowerCase();

                filterText = (filterText || "").toLowerCase();
                if (itemValue1.indexOf(filterText) >= 0 || itemValue2.indexOf(filterText) >= 0 ) {
                    return true;
                }
            }.bind(this));
        }

    });
    return ModifiedObjectsModel;
});
