/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
require(["illustrator/views/reports/changeReport", "models/selectedSystem", "currentPackage"],
        function (ChangeReport, selectedSystem, currentPackage)
        {
            describe("changeReportTest", function ()
            {
                var changeReport;
                var data = [{
                    "id": "summary",
                    "title": "{Summary}",
                    "attributes": {},
                    "layout": {
                        "type": "summary",
                        "attributes": {
                            "column-names": ["Name"]
                        }
                    },
                    "data": {
                        "entries": [{
                            "Name": {
                                "value": "DOOR_LF:B:54011101"
                            }
                        }]
                    }
                }, {
                    "id": "bundlesremoved",
                    "title": "{Bundles} {Removed}",
                    "attributes": {
                        "count": 1
                    },
                    "layout": {
                        "type": "table",
                        "attributes": {
                            "sorted-by": "{Name}",
                            "column-names": ["Name"]
                        }
                    },
                    "data": {
                        "entries": [{
                            "Name": {
                                "value": "BUN639",
                                "uid": "UIDdb325e-156b1904d69-b6c2cd7d6ae096caa0f778060e9d407f"
                            }
                        }]
                    }
                }];
                beforeEach(function ()
                {
                    $("body").append("<div id='reports'></div>");
                    $.ajax("/base/s/illustrator/templates/change-report.html", {async: false}).done(
                            function (html)
                            {
                                changeReport = new ChangeReport("#reports", {
                                    data: data
                                });
                                changeReport.htmlTemplate = html;
                                changeReport.renderToolBar = function ()
                                {

                                };
                                changeReport.render();

                            });
                });
                afterEach(function ()
                {
                    $("#reports").remove();
                });
                it("should be able to load changeReport", function ()
                {
                    expect(ChangeReport).toBeDefined();
                });
                it("should render table data in a container", function ()
                {
                    expect($("#reports").html().indexOf("Bundles Removed  - 1 ")).toBeTruthy();
                });

                function clickTableCell(ctrlPressed)
                {
                    var objectShown, objectHighlighted, autoZoomed;
                    changeReport.tableEventHandler.objectInteractionInterface.showPopover = function (uid, x, y)
                    {
                        objectShown = true;
                        expect(uid).toBe("test-uid");
                        expect(x).toBe(5);
                        expect(y).toBe(5);
                    };

                    changeReport.tableEventHandler.objectInteractionInterface.zoomToObject = function (waitTime)
                    {
                        autoZoomed = true;
                        expect(waitTime).toBe(1000);
                    };
                    changeReport.tableEventHandler.objectInteractionInterface.highlightObject = function (uid)
                    {
                        objectHighlighted = true;
                        expect(uid).toBe("test-uid");
                    };
                    changeReport.tableEventHandler.onCellClick($("#cell"), {
                        pageX: 5,
                        pageY: 5,
                        ctrlKey: ctrlPressed,
                        stopPropagation: function ()
                        {

                        }
                    });
                    return [objectShown, objectHighlighted, autoZoomed];
                }

                it("should show object popover when table cell with uid is clicked", function ()
                {
                    $("#reports").append("<div id='cell' data-objectId='test-uid' data-highlighted='true'></div>");
                    var objectShown = clickTableCell();
                    expect(objectShown[0]).toBeTruthy();
                });

                it("should not show object popover when table cell is not highlighted", function ()
                {
                    $("#reports").append("<div id='cell' data-objectId='test-uid'></div>");
                    var objectShown = clickTableCell();
                    expect(objectShown[0]).toBeFalsy();
                });

                it("should  show object popover when table cell is not highlighted but ctrl key is pressed", function ()
                {
                    $("#reports").append("<div id='cell' data-objectId='test-uid'></div>");
                    var objectShown = clickTableCell(true);
                    expect(objectShown[0]).toBeTruthy();
                });

                it("should  highlight object when table cell is clicked", function ()
                {
                    $("#reports").append("<div id='cell' data-objectId='test-uid'></div>");
                    var objectShown = clickTableCell();
                    expect(objectShown[1]).toBeTruthy();
                });

                it("should auto zoom to the highlighted element when it is configured", function (done) {
                    autoZoomOnClick(true).then(function (objectAutoZoomed) {
                        expect(objectAutoZoomed).toBeTruthy();
                        done();
                    });
                });

                function autoZoomOnClick(configured) {
                    return new Promise(function (resolve) {
                        var objectAutoZoomed = false;
                        changeReport.tableEventHandler.objectInteractionInterface.objectHighlighter = {
                            zoomViews: function () {
                                objectAutoZoomed = true;
                            }
                        };
                        changeReport.tableEventHandler.objectInteractionInterface.dataLoader = {
                            loadFile: function (url, async, cache, type) {
                                return {
                                    data: {
                                        autoZoomOnClick: configured
                                    }
                                };
                            }
                        };

                        changeReport.tableEventHandler.objectInteractionInterface.zoomToObject(10);

                        setTimeout(function () {
                            resolve(objectAutoZoomed);
                        }, 20);
                    });
                }


                it("should not auto zoom to the highlighted element when it is not configured", function ()
                {
                    autoZoomOnClick(false);
                });
                it("should  auto zoom to the highlighted element when it is configured", function ()
                {
                    autoZoomOnClick(true);
                });

                xit("loadSectionTable should load modified table using correct templates", function ()
                {
                    var tableLoaded, testTableData = {
                        title: "{bundles} {Modified}"
                    };
                    changeReport.loadTable = function (templates, view, model, tableData, tableEventHandler)
                    {
                        expect(templates).toBe(changeReport.modifiedSectionTemplates);
                        expect(view).toBe(changeReport.tableView);
                        expect(model).toBe("illustrator/models/reports/modifiedObjects");
                        expect(tableData).toBe(testTableData);
                        expect(tableEventHandler).toBe(changeReport.tableEventHandler);
                        tableLoaded = true;
                    };
                    changeReport.loadSectionTableWithPairs(testTableData);
                    expect(tableLoaded).toBeTruthy();
                });

                xit("loadSectionTable should load grid table using correct templates", function ()
                {
                    var tableLoaded, testTableData = {
                        title: "{bundles} {added}"
                    };
                    changeReport.loadTable = function (templates, view, model, tableData, tableEventHandler)
                    {
                        expect(templates).toBe(changeReport.sectionTableTemplates);
                        expect(view).toBe(changeReport.tableView);
                        expect(model).toBe(changeReport.tableModel);
                        expect(tableData).toBe(testTableData);
                        expect(tableEventHandler).toBe(changeReport.tableEventHandler);
                        tableLoaded = true;
                    };
                    changeReport.loadSectionTable(testTableData);
                    expect(tableLoaded).toBeTruthy();
                });

                xit("summary table should load  using correct templates", function ()
                {
                    var tableLoaded, testTableData = {
                        title: "{bundles} {added}"
                    };
                    changeReport.loadTable = function (templates, view, model, tableData, tableEventHandler)
                    {
                        expect(templates).toBe(changeReport.summaryTableTemplates);
                        expect(view).toBe(changeReport.tableView);
                        expect(model).toBe(changeReport.summaryTableModel);
                        expect(tableData).toBe(testTableData);
                        expect(tableEventHandler).toBeUndefined();
                        tableLoaded = true;
                    };
                    changeReport.loadSummaryTable(testTableData);
                    expect(tableLoaded).toBeTruthy();
                });

                xit("loadTables should load summary table for summary module", function ()
                {
                    var summaryLoaded, summaryMod = [{
                        layout: {
                            type: "summary"
                        }
                    }];
                    changeReport.loadSummaryTable = function (mod)
                    {
                        summaryLoaded = true;

                    }
                    changeReport.loadTables(summaryMod);
                    expect(summaryLoaded).toBeTruthy();
                });

                xit("loadTables should load non summary table for using section table module", function ()
                {
                    var tableLoaded, sectionmod = [{
                        layout: {
                            type: "table"
                        }
                    }];
                    changeReport.loadSectionTable = function (mod)
                    {
                        tableLoaded = true;

                    }
                    changeReport.loadTables(sectionmod);
                    expect(tableLoaded).toBeTruthy();
                });
                xit("should be able to load table using loadTable method", function ()
                {
                    var viewRendered = false;
                    var view = Backbone.View.extend({
                        render: function ()
                        {
                            viewRendered = true;
                        }
                    });
                    var model = Backbone.Model.extend({});
                    changeReport.moduleLoader = function (mods, callback)
                    {
                        expect(mods.length).toBe(5);
                        callback({}, {}, {}, view, model);
                    };
                    changeReport.loadSectionTable({
                        title: "bundles added",
                        layout: {
                            type: "table",
                            attributes: {},
                        }
                    });
                    expect(viewRendered).toBeTruthy();
                });

                xit("should be able to open view in popout window", function ()
                {
                    var contentOpned = false
                    var type;
                    currentPackage.set("searchText", "test-search", {silent:true});
                    var fileOpener = {
                        display: function (content)
                        {
                            expect(content.type.indexOf("Popout") >= 0).toBeTruthy();
                            expect(content.searchText.indexOf("test-search") >= 0).toBeTruthy();
                            contentOpned = true;
                        }
                    }
                    changeReport.moduleLoader = function (mods, callback)
                    {
                        expect(mods.length).toBe(1);
                        expect(mods[0]).toBe("fileDisplayHandler");
                        callback(fileOpener);
                    };
                    changeReport.openPopout();
                    expect(contentOpned).toBeTruthy();
                    currentPackage.set("searchText", "", {silent:true});
                });
                it("should be able to highlight cell of modified element", function () {
                    $("#reports").append(
                            "<table class='grid-ui'><tr><td id='highlighted' data-highlighted='true' style='background-color:#EEEE99;'>" +
                            "</td></tr>" +
                            "<tr><td id='toBeHighlighted' data-objectId='test-uid'></td></tr></table>");
                    changeReport.$el = $("#reports");
                    selectedSystem.set("objectId", "test-uid");
                    changeReport.highlightCells();
                    expect($("#toBeHighlighted").attr('style')).toBe('background-color:#EEEE99;');
                    expect($("#toBeHighlighted").attr('data-highlighted')).toBe('true');

                    // Adjusted expectations for removed attributes
                    expect($("#highlighted").attr('data-highlighted')).toBeUndefined();
                    expect($("#highlighted").attr('style')).toBeUndefined();
                });


            });

        }, function (err)
        {
            describe("changeReportTest loading failed", function ()
            {
                it("should load changeReportTest panel", function ()
                {
                    console.log(err);
                    expect(false).toBeTruthy();
                });
            });
        });