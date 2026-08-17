/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
require(["illustrator/models/reports/gridModel"], function (GridModel)
{
    describe("GridModelTest", function ()
    {
        var tableDataColl
        beforeEach(function ()
        {
            tableDataColl = new GridModel({
                tableData: {
                    layout: {
                        attributes: {
                            "column-names": ["name"]
                        }
                    },
                    data: {
                        entries: [
                            {
                                "name": {value: "name-col-value10"}
                            },
                            {
                                "name": {value: "name-col-value1"}
                            },
                            {
                                "name": {value: "name-col-value3"}
                            },
                            {
                                "name": {value: "name-col-value330"}
                            },
                            {
                                "name": {value: "name-col-value11"}
                            }
                        ]
                    },
                    id: "tableId"
                }
            });
        });
        it("should be able to load GridModel", function ()
        {
            expect(tableDataColl).toBeDefined();
        });

        it("should retuen all table objects when getTableData is called", function ()
        {
            var tableData = tableDataColl.getTableData();
            expect(tableData.cols.length).toBe(1);
            expect(tableData.cols[0]).toBe('name');
            expect(JSON.stringify(tableData.items)).toBe(
                    '[{"name":{"value":"name-col-value10"}},{"name":{"value":"name-col-value1"}},' +
                    '{"name":{"value":"name-col-value3"}},{"name":{"value":"name-col-value330"}},' +
                    '{"name":{"value":"name-col-value11"}}]');
        });

        it("should  be able to sort the data", function ()
        {
            var tableData = tableDataColl.getTableData();
            var unsortedIndices = tableData.items.map(function (val, idx) {
                return idx;
            });

            var sortedReversed = tableDataColl.sort(unsortedIndices, "name", true);
            expect(JSON.stringify(sortedReversed)).toBe("[3,4,0,2,1]");

            var sorted = tableDataColl.sort(unsortedIndices, "name", false);
            expect(JSON.stringify(sorted)).toBe("[1,2,0,4,3]");
        });

        it("should be able to filter data by column", function ()
        {
            var tableData = tableDataColl.getTableData();
            var unsortedIndices = tableData.items.map(function (val, idx) {
                return idx;
            });

            var filteredIndices = tableDataColl.filterByColumns(unsortedIndices, {
                name: "1"
            });

            expect(JSON.stringify(filteredIndices)).toBe("[0,1,4]");
        });

        it("should be able to match filterText for translated values", function(){
            var origUtilsTranslate = Utils.translate;
            Utils.translate = function(key){
                return "value-translated";
            }

            var item = tableDataColl.getTableData().items[0];
            var actualValue = tableDataColl.matchTextWithFilterText(item, "name", "value-Translated");
            expect(actualValue).toBe(true);
            Utils.translate = origUtilsTranslate;
        });
    });
}, function (err)
{
    describe("gridmodel loading failed", function ()
    {
        it("should load module", function ()
        {
            expect(false).toBeTruthy();
        });
    });
});