/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe, expect, it, listTemplateForTest, Backbone, $, _, createContext*/

require(['views/contentpanel/harnessReportPanel', "models/selectedSystem"],
        function (harnessReportPanel, selectedSystem) {
            var harnessData, report, panelclosed, panelOpened, reportHTML, origFilter;
            //adding dummy listeners for panel close and open events
            selectedSystem.on(harnessReportPanel.getCloseEvent(harnessReportPanel.publishedEventType), function () {
                panelclosed = true;
            });
            selectedSystem.on(harnessReportPanel.getOpenEvent(harnessReportPanel.publishedEventType), function () {
                panelOpened = true;
            });
            describe("harnessReportPanelTest", function () {
                function createHarnessReportPanel()
                {
                    selectedSystem.set("harnessLayoutId", "testId", {silent: true})
                    selectedSystem.set(mentor.publisher.contentType.HARNESS_LAYOUT_REPORT, report, {silent: true});
                    harnessReportPanel.render();
                    expect(reportHTML.indexOf($("#reportHtml").html()) >= 0).toBeTruthy();
                }

                beforeEach(function () {
                    panelclosed = false;
                    panelOpened = false;
                    //create empty report container
                    var container = $("<div id='harnessReportPanelTest'></div>");
                    reportHTML = "<div id='reportHtml'>report goes here</div>";
                    $('body').append(container);

                    $(container).html(
                            '<div id="splitter1" class="contentArea auto-splitter-1"></div><div id="verticalResizebar" class="verticalResizebar">' +
                            '</div><div id="splitter2" class="contentArea auto-splitter-2"></div> <div id="horizontalResizebar" class="horizontalResizebar"></div>' +
                            ' <div id="splitter3" class="contentArea auto-splitter-3"></div>');

                    harnessData =
                            '[ {"id" : "UIDba4871-1422e294c1b-279e2bee43fed9b5aeb17e7eb27cb620","mainText" : "Aerospace Harness","tooltips" : [ ],"documentSets" : [ {"title" : "reports","type" : "html","documents" : ' +
                            '[ {"id" : "Harness Component BOM","mainText" : "Harness Component BOM","path" : "Harness Component BOM.html"} ]} ]} ]';
                    var harDes = JSON.parse(harnessData);
                    report = {
                        get: function () {
                            return "id";
                        }
                    }
                    harnessReportPanel.getHarnessLayouts = function () {
                        return {
                            get: function (id) {
                                return harnessData;
                            }
                        }
                    };
                    harnessReportPanel.loadModule = function () {
                        return function (path, callback) {
                            callback(reportHTML)
                        }
                    }

                    harnessReportPanel.createToolBar = function () {
                    };

                    harnessReportPanel.translateTemplate = function () {
                        return reportHTML;
                    }

                    harnessReportPanel.container = "#splitter3";
                    origFilter = mentor.publisher.filter;
                    mentor.publisher.filter = {
                        vinOptions: ""
                    };

                    createHarnessReportPanel();
                });

                afterEach(function () {
                    $("#harnessReportPanelTest").remove();
                    mentor.publisher.filter = origFilter;
                });

                it("should not be undefined", function () {
                    expect(harnessReportPanel).toBeDefined();
                });

                it("should render report when a report is selected", function () {
                    var isWaiting = true;
                    expect(harnessReportPanel.isPanelActive()).toBeTruthy();

                    runs(function() {
                        setTimeout(function() {
                            isWaiting = false;
                        }, 500);
                    });

                    waitsFor(function() {
                        return !isWaiting;
                    }, 2000);

                    runs(function() {
                        expect(panelOpened).toBeTruthy();
                    });

                });

                // This is same as "should render report when a report is selected"
                xit("should notify listeners when report is opened", function () {
                    expect(panelOpened).toBeTruthy();
                });

                it("should close when close button is clicked", function () {

                    harnessReportPanel.close();
                    expect($("#reportHtml").html()).toBeUndefined();


                });

                it("should notify listeners when report is closed", function () {
                    harnessReportPanel.close();
                    expect(panelclosed).toBeTruthy();

                });

                it("should re render content when language preference changes", function () {
                    spyOn(harnessReportPanel, "updateTitle");
                    spyOn(harnessReportPanel, "reRender");
                    var isWaiting = true;

                    runs(function() {
                        var preferences = require("preferences");
                        preferences.set("language", "FR");
                        setTimeout(function() {
                            isWaiting = false;
                        }, 100);
                    });

                    waitsFor(function() {
                        return !isWaiting;
                    }, 2000);

                    runs(function() {
                        expect(harnessReportPanel.reRender).toHaveBeenCalled();
                        expect(harnessReportPanel.updateTitle).toHaveBeenCalled();
                    });
                });

                xit("should get harness layouts", function () {
                    expect(harnessReportPanel.getHarnessLayouts()).toBeTruthy();
                });

                xit("should get title", function () {
                    var Model = Backbone.Model.extend({});
                    var report = new Model(), harnessLayout = new Model(), harnessLayouts = new Model();
                    report.set("mainText", "mainReportText");
                    harnessLayout.set("mainText", "mainHarnessLayoutText");
                    selectedSystem.set("harnessLayoutId","testId");
                    harnessLayouts.set("testId", harnessLayout);
                    selectedSystem.set(harnessReportPanel.getContentType(), report);
                    harnessReportPanel.getHarnessLayouts = function () {
                        return harnessLayouts;
                    }
                    expect(harnessReportPanel.getTitle()).toBe("mainHarnessLayoutText, mainReportText");
                });

                xit("should get data ID", function () {
                    selectedSystem.set("harnessLayoutId", "testId")
                    expect(harnessReportPanel.getDataId()).toBe("testId");
                });

                xit("should get document type", function () {
                    expect(harnessReportPanel.getDocumentType()).toBe("reports");
                });

                xit("should get document set", function () {
                    var result=harnessReportPanel.getDocumentSet();
                    var expectedResult = [ {"id" : "UIDba4871-1422e294c1b-279e2bee43fed9b5aeb17e7eb27cb620","mainText" : "Aerospace Harness","tooltips" : [ ],"documentSets" : [ {"title" : "reports","type" : "html","documents" : [ {"id" : "Harness Component BOM","mainText" : "Harness Component BOM","path" : "Harness Component BOM.html"} ]} ]} ];
                    expect(result).toBe(expectedResult);
                });

                xit("should get tool bar content", function () {
                    expect(harnessReportPanel.getToolBarContent()).toBe({});
                });
            });
        }, function (err) {
            describe("harnessReportPanelTest - Module load failed", function () {
                it("failed to load module in test", function () {
                    expect(err).toBeUndefined();
                });
            });
        });


