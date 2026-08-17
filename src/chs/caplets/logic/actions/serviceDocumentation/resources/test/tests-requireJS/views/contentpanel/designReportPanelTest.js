/*global require, describe, it, expect, Backbone, beforeEach, afterEach, mentor*/
(function ()
{
    var context, spy = mentor.publisher.popoutHandler, stubs, Model = Backbone.Model.extend(), View = Backbone.View.extend(), Collection = Backbone.Collection.extend();
    var fakeFileDisplayHandler = {
        display: function (content)
        {
            this.fileHandles[content.type](content);
        },
        addFileHandler: function (type, fileHandler)
        {
            this.fileHandles = this.fileHandles || {};
            this.fileHandles[type] = fileHandler;
        }
    };

    stubs = {
        jquery: $,
        underscore: _,
        backbone: Backbone,
        ListGroupView: new View(),
        currentPackage: new Model(),
        "SignalTracerModel": new Model(),
        "RelatedDataPopoverModel": new Model(),
        "ReportsPopoverModel": new Model(),
        "DiagramsPopoverModel": new Model(),
        "views/navigationPanelView": new View(),
        "views/appNameAndLogo/appNameAndLogoView": {
            updateApplicationNameAndLogo: function () {
            }
        },
        "TranslationUtils": {
            translateHTMLContent: function () {
            }
        },
        "internalLinkHandler": {
            addMouseEventListener: function () {
            }

        },
        fileDisplayHandler: fakeFileDisplayHandler
    };

    stubs.currentPackage.set({id: "packageId"});
    context = createContext(stubs);

    context(["views/contentpanel/designReportPanel", "models/selectedSystem",
                "views/contentpanel/toolbar/contentToolBar"],
            function (designReportPanel, selectedSystem, contentToolBar)
            {
                "use strict";
                describe("designReportPanelTest", function ()
                {
                    var designRep = new designReportPanel();

                    beforeEach(function ()
                    {

                    });

                    it("should be able to load designReportPanel Module", function ()
                    {
                        expect(designReportPanel).toBeDefined();
                        expect(contentToolBar).toBeDefined();
                    });

                    it("should reset report panel and data if new system is opened", function ()
                    {
                        selectedSystem.set("reportId", "testId", {silent: true});
                        selectedSystem.set("reportPath", "testPath", {silent: true});
                        selectedSystem.set("systemId", "changed");
                        expect(selectedSystem.get("reportId")).toBe("");
                        expect(selectedSystem.get("reportPath")).toBe("");
                    });
                    it("should generate systemReportClosed event when panel is closed", function ()
                    {
                        var systemReportClosed = false;
                        selectedSystem.on("systemReportClosed", function ()
                        {
                            systemReportClosed = true;
                        });
                        designRep.close();
                        expect(systemReportClosed).toBeTruthy();
                    });

                    it("should initialize click behaviour on the reports", function ()
                    {
                        var initialized = false, designRep1 = new designReportPanel();
                        mentor.publisher.filter = {
                            vinOptions: "op1"
                        };
                        designRep1.initReportClickEvents = function ()
                        {
                            initialized = true;
                        };
                        designRep1.systemReportLoadFinished("testContainer", "testSystemId");
                        expect(initialized).toBeTruthy();
                        //expect(typeof designRep.initReportClickEvents()).toBe("object");
                    });

                    it("should be able to create system report toolbar when report is opened first time", function ()
                    {
                        var drp = new designReportPanel(), renderedContent;
                        var Toolbar = function ()
                        {
                            return {
                                render: function (content)
                                {
                                    renderedContent = content;
                                }
                            }
                        };
                        drp.createToolBar(drp, Toolbar, "html-content");
                        expect(renderedContent).toBe("html-content");
                    });

                    it("should be able to render the report content", function ()
                    {
                        var drp = new designReportPanel(), actTemplate, actContent;
                        drp.renderTemplate = function (template, content)
                        {
                            actTemplate = template;
                            actContent = content;
                        };
                        drp.renderContent("htmlReportTemplate", {report : "htmlReport"});

                        expect(actContent.report).toBe("htmlReport");
                        expect(actTemplate).toBe("htmlReportTemplate");

                    });

                    it("should generate systemDiagram open event when report is opened", function ()
                    {
                        var reportOpened = true, drp = new designReportPanel();
                        selectedSystem.on("systemReportOpened", function ()
                        {
                            reportOpened = true;
                        });
                        drp.notify();
                    });

                    it("should enable all buttons on toolbar when showDiagramBtn is called", function ()
                    {
                        var drp = new designReportPanel(), selectors = {}, isReportCalled = false, isReportBtnCalled = false, isdiagramCalled = false, currentElement;

                        drp.toggleVisibility = function (element, bool)
                        {
                            selectors[element] = bool;
                        };

                        drp.getSubElement = function (selector)
                        {
                            currentElement = selector;

                            return $("body");
                        };
                        drp.isReportOpen = function ()
                        {
                            isReportCalled = true;
                            return true;
                        };

                        drp.isDiagramButttonActive = function ()
                        {
                            isdiagramCalled = true;
                            return true;
                        };

                        drp.isReportButtonActive = function ()
                        {
                            isReportBtnCalled = true;
                            return true;
                        };

                        drp.showDiagramBtn();
                        expect(isReportCalled).toBeTruthy();
                        expect(isReportBtnCalled).toBeTruthy();
                        expect(isdiagramCalled).toBeTruthy();
                        expect(selectors[".reports-button"]).toBeTruthy();
                        expect(selectors[".related-data-button"]).toBeTruthy();
                        expect(selectors[".diagrams-button"]).toBeTruthy();

                        drp.hideDiagramBtn();

                        expect(selectors[".reports-button"]).toBeTruthy();
                        expect(selectors[".related-data-button"]).toBeFalsy();
                        expect(selectors[".diagrams-button"]).toBeFalsy();

                        drp.hideAllSystemButtons();

                        expect(selectors[".reports-button"]).toBeFalsy();
                        expect(selectors[".related-data-button"]).toBeFalsy();
                        expect(selectors[".diagrams-button"]).toBeFalsy();


                    });

                });
            }, function (err)
            {
                describe("designReportPanelTestFailed", function ()
                {
                    it("failed to load designReportPanel", function ()
                    {
                        expect(err).toBeUndefined();
                    });
                });
            });

})();



