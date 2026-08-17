/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
require(["illustrator/models/reports/modifiedObjects"], function (ModifiedObjects)
        {
            describe("ModifiedObjectsTest", function ()
            {
                var tableDataColl
                beforeEach(function ()
                {
                    tableDataColl = new ModifiedObjects({
                        tableData: {
                            layout: {
                                attributes: {
                                    "column-names": ["name"]
                                }
                            },
                            data: {
                                entries: [
                                    {
                                        "name": {value: "name-col-value1a"}
                                    },
                                    {
                                        "name": {value: "name-col-value1b"}
                                    },
                                    {
                                        "name": {value: "name-col-value2b"}
                                    },
                                    {
                                        "name": {value: "name-col-value2b"}
                                    }
                                ]
                            },
                            id: "tableId"
                        }
                    });
                });
                it("should be able to load ModifiedObjects", function ()
                {
                    expect(tableDataColl).toBeDefined();
                });

                it("should return grouped data by merging new and old rows table objects when getTableData is called",
                        function ()
                        {
                            var tableData = tableDataColl.getTableData();
                            expect(tableData.cols.length).toBe(1);
                            expect(tableData.cols[0]).toBe('name');
                            expect(JSON.stringify(tableData.items)).toBe(
                                    '[{"name":{"value":"name-col-value1a"},"old-name":{"value":"name-col-value1b"}},{"name":{"value":"name-col-value2b"},"old-name":{"value":"name-col-value2b"}}]');
                        });

                it("should should be able to sort the data", function ()
                {
                    var tableData = tableDataColl.getTableData();
                    var visibleIndices = [];
                    for(let i=0; i<tableData.items.length; i++){
                        visibleIndices[i]=i;
                    }
                    var decendingSort = tableDataColl.sort(visibleIndices, "name", true);
                    var result = [];
                    for(let i=0; i<decendingSort.length; i++){
                        result.push(tableData.items[decendingSort[i]])
                    }
                    expect(JSON.stringify(result)).toBe(
                            '[{"name":{"value":"name-col-value2b"},"old-name":{"value":"name-col-value2b"}},{"name":{"value":"name-col-value1a"},"old-name":{"value":"name-col-value1b"}}]'
                    )
                    ;
                    var ascendingSort = tableDataColl.sort(visibleIndices, "name", false);
                    var result2 = [];
                    for(let i=0; i<ascendingSort.length; i++){
                        result2.push(tableData.items[ascendingSort[i]])
                    }
                    expect(JSON.stringify(result2)).toBe(
                            '[{"name":{"value":"name-col-value1a"},"old-name":{"value":"name-col-value1b"}},{"name":{"value":"name-col-value2b"},"old-name":{"value":"name-col-value2b"}}]')
                    ;
                });

                it("should be able to filter data by column", function ()
                {
                    var tableData = tableDataColl.getTableData();
                    var visibleIndices = [];
                    for(let i=0; i<tableData.items.length; i++){
                        visibleIndices[i]=i;
                    }
                    var filteredIndices = tableDataColl.filterByColumns(visibleIndices, {
                        name: "1a"
                    });

                    var result = _.filter(tableData.items, function(val, idx){
                        return filteredIndices.includes(idx);
                    });

                    expect(JSON.stringify(result)).toBe(
                            '[{"name":{"value":"name-col-value1a","filtered":false},"old-name":{"value":"name-col-value1b","filtered":false}}]')
                    ;
                });

                it("should be able to filter data by old attribute value", function ()
                {
                    var tableData = tableDataColl.getTableData();
                    var visibleIndices = [];
                    for(let i=0; i<tableData.items.length; i++){
                        visibleIndices[i]=i;
                    }
                    var filteredIndices = tableDataColl.filterByColumns(visibleIndices, {
                        name: "1b"
                    });
                    var result = _.filter(tableData.items, function(val, idx){
                        return filteredIndices.includes(idx);
                    });

                    expect(JSON.stringify(result)).toBe(
                            '[{"name":{"value":"name-col-value1a","filtered":false},"old-name":{"value":"name-col-value1b","filtered":false}}]')
                    ;
                });

                it("should be able to filter data by old attribute value when searched from search bar", function ()
                {
                    var tableData = tableDataColl.getTableData();
                    var visibleIndices = [];
                    for(let i=0; i<tableData.items.length; i++){
                        visibleIndices[i]=i;
                    }

                    var filteredIndices = tableDataColl.filterByColumns(visibleIndices, {
                        name: "1b",
                        searchAllCols: "b"
                    });
                    var result = _.filter(tableData.items, function(val, idx){
                        return filteredIndices.includes(idx);
                    });

                    expect(JSON.stringify(result)).toBe(
                            '[{"name":{"value":"name-col-value1a","filtered":false},"old-name":{"value":"name-col-value1b","filtered":true}}]')
                    ;
                });

            });
        }
        , function (err)
        {
            describe("modfiedObjects loading failed", function ()
            {
                it("should load module", function ()
                {
                    expect(false).toBeTruthy();
                });
            });
        });