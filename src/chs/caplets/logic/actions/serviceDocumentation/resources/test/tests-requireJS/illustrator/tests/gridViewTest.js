/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
require(["illustrator/views/reports/gridView", "illustrator/models/reports/gridModel"],
        function (GridView, GridModel)
{
    describe("GridViewTest", function ()
    {
        it("should be able to load gridView module", function ()
        {
            expect(GridView).toBeDefined();
        });
        function createTemplates()
        {
            var Templates = Backbone.Model.extend();
            var temp = new Templates();
            temp.set({
                tableContainer: "<table><tbody></tbody></table>",
                innerTable: "",
                table: "<tr><td>data</td></tr>"
            });
            return temp;
        }

        function tableData()
        {
            return {
                tableData: {
                    layout: {
                        attributes: {
                            "column-names": ["name", "description"]
                        }
                    },
                    data: {
                        entries: [
                            {
                                "name": {value: "name-col-value10"},
                                "description": {value: "Beta"}
                            },
                            {
                                "name": {value: "name-col-value1"},
                                "description": {value: "Delta"}
                            },
                            {
                                "name": {value: "name-col-value3"},
                                "description": {value: "Charlie"}
                            },
                            {
                                "name": {value: "name-col-value330"},
                                "description": {value: "Alpha"}
                            },
                            {
                                "name": {value: "name-col-value11"},
                                "description": {value: "Foxtort"}
                            }
                        ]
                    },
                    id: "tableId"
                }
            }
        }

        function createTableModel(searchable, sortable)
        {
            sortable = sortable || false;
            searchable = searchable || false;
            return new GridModel(tableData());
        }

        beforeEach(function ()
        {
            $("body").append("<div id='reports'></div>");
        });

        afterEach(function ()
        {
            $("#reports").remove();
        });

                it("should be able to render table for given data", function ()
                {
                    var tableEventHandler = {};
                    var temp = createTemplates();
                    var tableModel = createTableModel();
                    var view = new GridView(tableModel, temp, tableEventHandler);
                    view.container = "#reports";
                    var isWaiting = true;

                    runs(function() {
                        view.render();
                        setTimeout(function() {
                            isWaiting = false;
                        }, 700);
                    });
                    waitsFor(function() {
                        return !isWaiting;
                    }, 2000);
                    runs(function() {
                        expect($("#reports").html()).toBe("<table><tr><td>data</td></tr></table>");
                    });
                });

        function createTestView(searchable, sortable, eventHandler)
        {
            var tableEventHandler = eventHandler || {};
            var temp = createTemplates();
            var tableModel = createTableModel(searchable, sortable);
            var view = new GridView(tableModel, temp, tableEventHandler);
            view.searchable = searchable;
            view.sortable = sortable;
            // view.data = tableData();
            return view;
        }

        function setTableDataForView(searchable, sortable)
        {
            var view = createTestView(searchable, sortable);
            var data = view.getVisibleIndices({
                "name": "value1",
                "searchAllCols": "value1"
            });
            return data;
        }

        it("getVisibleIndices should filter data for filtrable table", function ()
        {
            var data = setTableDataForView(true, false);
            expect(JSON.stringify(data)).toBe("[0,1,4]");
        });

        it("getVisibleIndices should not filter data for non filtrable table", function ()
        {
            var data = setTableDataForView(false, false);
            expect(JSON.stringify(data)).toBe("[0,1,2,3,4]");
        });

        it("getVisibleIndices should sort data for sortable table", function ()
        {
            var view = createTestView(false, true);
            view.sorting.col = "name";
            var visibleIndices = view.getVisibleIndices({});

            expect(JSON.stringify(visibleIndices)).toBe("[1,2,0,4,3]");
        });

        it("getVisibleIndices should not sort data for non sortable table", function ()
        {
            var view = createTestView(false, true);
            view.sorting.col = "name";
            var visibleIndices = view.getVisibleIndices({});

            expect(JSON.stringify(visibleIndices)).toBe("[1,2,0,4,3]");
        });

        it("sort should use first column for sorting when no column is specified", function ()
        {
            var view = createTestView(false, true);
            var visibleIndices = view.getVisibleIndices({});
            expect(JSON.stringify(visibleIndices)).toBe("[1,2,0,4,3]");
            // expect(view.data["sorted"]).toBe('namefalse');
        });

        it("sort should use specified column for sorting when specified", function ()
        {
            var view = createTestView(false, true);
            view.sorting.col = "description";
            view.sorting.order = view.sorting.asc;
            var visibleIndices = view.getVisibleIndices({});
            expect(JSON.stringify(visibleIndices)).toBe("[4,1,2,0,3]");
        });
        it("getSortOrder should give correct sorting order", function ()
        {
            var view = createTestView(false, true);
            var order = view.getSortOrder("name", view.sorting.asc, view.sorting.desc, "name", view.sorting.asc);
            expect(order).toBe('&#9650;');

            order = view.getSortOrder("name", view.sorting.asc, view.sorting.desc, "name", view.sorting.desc);
            expect(order).toBe('&#9660;');
        });

        it("onColumnHeaderClick should not re-render table when it is not sortable", function ()
        {
            var view = createTestView(false, true);
            view.sortable = false;
            var tableBodyRendered = false;
            view.renderTableBody = function ()
            {
                tableBodyRendered = true;
            }
            view.onColumnHeaderClick({});
            expect(tableBodyRendered).toBeFalsy();
        });
        it("onColumnHeaderClick should not re-render table when it is  sortable", function ()
        {
            var view = createTestView(false, true);
            view.sortable = true;
            var tableBodyRendered = false;
            view.renderTableBody = function ()
            {
                tableBodyRendered = true;
            }
            view.onColumnHeaderClick({});
            expect(tableBodyRendered).toBeTruthy();
        });

        it("onColumnHeaderClick should remove existing filter for sortable table", function ()
        {
            var view = createTestView(false, true);
            view.sortable = true;
            view.sorting.col = "name";
            var removeExistingFilteringRemoved = false;
            var filterSet = false;
            var sortingColumnAndOrderSet = false;
            view.removeExistingFiltering = function ()
            {
                removeExistingFilteringRemoved = true;
            };
            view.setFiltering = function ()
            {
                filterSet = true;
            }
            view.setSortingColumnAndOrder = function ()
            {
                sortingColumnAndOrderSet = true;
            }
            view.onColumnHeaderClick({});
            expect(removeExistingFilteringRemoved).toBeTruthy();
            expect(filterSet).toBeTruthy();
            expect(sortingColumnAndOrderSet).toBeTruthy();
        });
        it("on table cell click should invoke onCellClick method in event handler", function ()
        {
            var tableCellClicked = false;
            var view = createTestView(false, true, {
                onCellClick: function ()
                {
                    tableCellClicked = true;
                }
            });
            view.onCellClick({});
            expect(tableCellClicked).toBeTruthy();
        });
        it("should re-render table when search text entered in a column filter field", function ()
        {
            var view = createTestView(true, true);
            view.getTargetElement = function ()
            {
                return {
                    attr: function (attr)
                    {
                        expect(attr).toBe('data-col');
                        return "name";
                    },
                    val: function ()
                    {
                        return "n";
                    }
                };
            };
            var tableReRendered = false;
            view.renderTableBody = function ()
            {
                tableReRendered = true;
            };
            view.onSearchTextEnter({});
            expect(JSON.stringify(view.filtering)).toBe('{"name":"n"}');
            expect(tableReRendered).toBeTruthy();
            {

            }
        });

    });

}, function (err)
{
    describe("GridView loading failed", function ()
    {
        it("should load GridView panel", function ()
        {
            expect(false).toBeTruthy();
        });

    });
});