/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global require, expect, describe, beforeEach, window, it, afterEach, Backbone, runs, waitsFor, mentor, $*/

require(["views/contentpanel/systemDiagramPanel", "currentPackage", "models/selectedSystem",
    "views/contentpanel/toolbar/contentToolBar"],
    function (systemDiagramPanel, currentPackage, selectedContent, toolBarObj) {
        "use strict";
        describe("systemDiagramPanelTest", function () {
            var oldGetObjects, toolBarRenderMethod, customDataModel, Model, spy, oldMethod;
            beforeEach(function () {
                oldMethod = mentor.publisher.contentArea.closeExistingPanel;
                mentor.publisher.contentArea.closeExistingPanel = function () {

                };
                $("body").append("<div id='systemPanel'></div>");

                systemDiagramPanel.getSystemData = function () {
                    var Model = Backbone.Model.extend({}), model = new Model();
                    model.set({title : "testTitle",
                        path : "dummy.svg?packageId=12da",
                        systemId : "dummySystemId",
                        diagramId : 'testDiagramId',
                        computeTitle : function(){}
                    });
                    return model;
                };
                systemDiagramPanel.container = "#systemPanel";
                systemDiagramPanel.createToolBar = function () {

                };
                systemDiagramPanel.addZoomAndPanEventHandlers = function () {

                };
                systemDiagramPanel.templateHTML = "<%=systemId%>, <%=path%>";

            });
            it("should be able to load systemDiagramPanel Module", function () {
                expect(systemDiagramPanel).toBeDefined();
            });

            it("should be able to render system SVG", function () {
                var svgPanelRelaidOut = false;
                systemDiagramPanel.relayoutContentPanel = function () {
                    svgPanelRelaidOut = true;
                };
                runs(function () {
                    systemDiagramPanel.render();
                });

                waitsFor(function () {
                    return svgPanelRelaidOut;
                }, 200);

                runs(function () {
                    //expect(spy.withArgs().calledOnce).toBeTruthy();
                    expect(systemDiagramPanel.$el.html().indexOf('dummySystemId') !== -1).toBeTruthy();
                    expect(systemDiagramPanel.$el.html().indexOf('dummy.svg?packageId=12da') !== -1).toBeTruthy()
                });

            });

            it("should create tool bar when SVG is rendered", function () {
                var svgPanelRelaidOut = false, toolBarcreated;
                systemDiagramPanel.isDiagramOpen = false;
                systemDiagramPanel.relayoutContentPanel = function () {
                    svgPanelRelaidOut = true;
                };

                systemDiagramPanel.createToolBar = function () {
                    toolBarcreated = true;
                };
                runs(function () {
                    systemDiagramPanel.render();
                });

                waitsFor(function () {
                    return svgPanelRelaidOut;
                }, 200);

                runs(function () {
                    expect(toolBarcreated).toBeTruthy();
                });

            });

            it("should create zoom tool bar when SVG is rendered", function () {
                var svgPanelRelaidOut = false, zoomAndPanEventsAreAdded, zoomToolBarCreated;
                systemDiagramPanel.relayoutContentPanel = function () {
                    svgPanelRelaidOut = true;
                };

                systemDiagramPanel.computeTitle = function(){};

                systemDiagramPanel.createZoomToolBar = function () {
                    zoomToolBarCreated = true;
                };

                systemDiagramPanel.addZoomAndPanEventHandlers = function () {
                    zoomAndPanEventsAreAdded = true;
                };

                runs(function () {
                    systemDiagramPanel.render();
                });

                waitsFor(function () {
                    return svgPanelRelaidOut;
                }, 200);

                runs(function () {
                    expect(zoomAndPanEventsAreAdded).toBeTruthy();
                    expect(zoomToolBarCreated).toBeTruthy();
                });

            });

            afterEach(function () {
                mentor.publisher.contentArea.closeExistingPanel = oldMethod;
                $("#systemPanel").html('');
            });

        });
    });
