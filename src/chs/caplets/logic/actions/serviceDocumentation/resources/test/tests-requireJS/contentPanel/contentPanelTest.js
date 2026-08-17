/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
(function () {
    "use strict";
    var mockModel = new (Backbone.Model.extend())(), context, stubs, xrefContent;

    stubs = {
        currentPackage: mockModel,
        layoutManager: {},
        jquery: $,
        underscore: _,
        backbone: Backbone,
        ReportsCollection: new (Backbone.Collection.extend())(),
        PopoverItemView: Backbone.Model.extend(),
        fileDisplayHandler: {
            display: function (content) {
                this.content = content;
            }
        }
    };
    context = createContext(stubs);

    function createTestData()
    {
        var systems, content;
        systems = {
            getDiagrams: function () {
                return [];
            },
            getFirstDiagram: function () {
                return {
                    mainText: "report.html"
                }
            },
            getOptionExpression: function () {
                return "";
            }
        }
        content = new (Backbone.Model.extend({}))();
        content.set(systems);
        content.type = mentor.publisher.contentType.SYSTEM_SVG;
        return content;
    }

    context(['views/contentpanel/contentPanel'], function (contentPanelView) {
        describe("contentPanelTest", function () {
            var splitter1 = "#splitter1", splitter2 = "#splitter2", splitter3 = "#splitter3", render,
                    contentPanelTemplate = '<div class="component-button small-button collapseBtn collapse-expand-nav-panel" ></div><div id="splitter1" class="contentArea" style="display: none"></div><div id="verticalResizebar" class="verticalResizebar" style="display: none; height: 244px; "></div><div id="splitter2" class="contentArea" style="display: none"></div><div id="horizontalResizebar" class="horizontalResizebar" style="display: none; "></div><div id="splitter3" class="contentArea" style="display: none"></div>';

            render = function () {
                contentPanelView.container = ".contentPanel";
                contentPanelView.templateHTML = contentPanelTemplate;
                contentPanelView.render();
            };

            beforeEach(function () {
                window.resizeVinFilterBox = function () {

                }
                $(".contentPanel").remove()
                $("body").append($("<div class='contentPanel'></div>"));
                render();
            });

            afterEach(function () {
                $(".contentPanel").remove();
            });

            it("should be able to load contentPanel Module", function () {
                expect(contentPanelView).toBeDefined();
            });

            it("clicking system from navigation panel should show reports if there is no diagram within a system",
                    function () {
                        var content = createTestData();
                        var report = contentPanelView.showReportIfSystemDoesHaveNoDiagrams(content);
                        expect(report.type).toBe(mentor.publisher.contentType.SYSTEM_REPORT);
                    });

            it("content panel should reset existing view before dislaying new content",
                    function () {
                        var content = createTestData();
                        contentPanelView.showContent(content);
                        expect(stubs.fileDisplayHandler.content.reset).toBe(true);
                    });

            it("should be able to load template successfully", function () {
                var renderedHTML, expectedHTML = contentPanelTemplate;
                expect($(splitter1).css("display")).toEqual("none");
                expect($(splitter2).css("display")).toEqual("none");
                expect($(splitter3).css("display")).toEqual("none");
            });

        });
    });
})();
