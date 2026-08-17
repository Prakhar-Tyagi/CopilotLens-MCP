/*global define, mentor, _*/
define([
    'underscore',
    'backbone'
], function (underscore, Backbone)
{
    "use strict";
    var GridModel, p = mentor.publisher;
    GridModel = Backbone.Model.extend({
        defaults: {
            sortable: true,
            searchable: true,
            container: '',
            tableData: {}
        },
        initialize: function ()
        {
            this.get('tableData').cols = this.get('tableData').layout.attributes['column-names'];
            this.get('tableData').items = this.get('tableData').data.entries;
            this.set('container', "#" + this.get('tableData').id + ">.change-table");
        },

        getTableData: function ()
        {
            return this.get('tableData');
        },
        compareWithColValue: function (item, column, filterText)
        {
            var found = false;
            if (this.matchTextWithFilterText(item, column, filterText)) {
                item[column].filtered = true;
                found = true;
            }
            else {
                item[column].filtered = false;
            }
            return found;
        }, matchWithCols: function (filteringText, column, item)
        {
            var found = false;
            if (filteringText) {
                found = this.compareWithColValue(item, column, filteringText);
            }
            else {
                item[column].filtered = false;
                found = true;
            }
            return found;
        },
        searchInAllCols: function (item, filteringTextPerColumn, searchText)
        {
            var found = false;
            for (var column in filteringTextPerColumn) {
                if (filteringTextPerColumn.hasOwnProperty(column) && column !== 'searchAllCols') {
                    var filterText = (searchText || "");
                    var match = this.matchWithCols(filterText, column, item);
                    if (match) {
                        found = true;
                    }
                }
            }
            return found;
        },
        searchInTable: function (visibleIndices, filteringTextPerColumn, searchText)
        {
            return _.filter(visibleIndices, function (index) {
                var item = this.getTableData().items[index];
                return this.searchInAllCols(item, filteringTextPerColumn, searchText);
            }.bind(this));
        },
        filterByColumns: function (visibleIndices, filteringTextPerColumn)
        {
            var searchInAllCols = filteringTextPerColumn['searchAllCols'];
            visibleIndices = this.searchInTable(visibleIndices, filteringTextPerColumn, searchInAllCols);
            for (var column in filteringTextPerColumn) {
                if (filteringTextPerColumn.hasOwnProperty(column) && column !== "searchAllCols") {
                    var filterText = filteringTextPerColumn[column] || "";
                    if (filterText) {
                        visibleIndices = this.filter(visibleIndices, column, filterText);
                    }
                }
            }
            return visibleIndices;
        },
        matchTextWithFilterText: function (item, colName, filterText)
        {
            if (item[colName].value && item[colName].value.data) {
                return true;
            }
            var translatedItemValue = Utils.translate(item[colName].value)
            var itemValue = (translatedItemValue || "").toLowerCase();
            filterText = (filterText || "");
            filterText = filterText.toLocaleLowerCase && filterText.toLocaleLowerCase()

            if (itemValue.indexOf(filterText) >= 0) {
                return true;
            }
        },
        filter: function (visibleIndices, colName, filterText)
        {
            return _.filter(visibleIndices, function (index)
            {
                var item = this.getTableData().items[index];
                return this.matchTextWithFilterText(item, colName, filterText);
            }.bind(this));
        },
        sort: function (visibleIndices, colName, dsc)
        {
            var sortedArray = visibleIndices || [];
            sortedArray = sortedArray.sort(function (aIndex, bIndex)
            {
                function chunkify(t)
                {
                    var tz = [], x = 0, y = -1, n = 0, i, j;

                    while (i = (j = t.charAt(x++)).charCodeAt(0)) {
                        var m = (i == 46 || (i >= 48 && i <= 57));
                        if (m !== n) {
                            tz[++y] = "";
                            n = m;
                        }
                        tz[y] += j;
                    }
                    return tz;
                }

                var a = this.getTableData().items[aIndex];
                var b = this.getTableData().items[bIndex];
                var translatedVal_a = Utils.translate(a[colName].value);
                var translatedVal_b = Utils.translate(b[colName].value);
                var aa = chunkify(translatedVal_a.toLowerCase());
                var bb = chunkify(translatedVal_b.toLowerCase());

                for (var x = 0; aa[x] && bb[x]; x++) {
                    if (aa[x] !== bb[x]) {
                        var c = Number(aa[x]), d = Number(bb[x]);
                        if (c == aa[x] && d == bb[x]) {
                            return c - d;
                        }
                        else {
                            return (aa[x] > bb[x]) ? 1 : -1;
                        }
                    }
                }
                return aa.length - bb.length;
            }.bind(this));
            if (dsc) {
                sortedArray = sortedArray.reverse();
            }
            return sortedArray;
        }
    });
    return GridModel;
});
