/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global require, describe, it, expect, Backbone, beforeEach, afterEach, mentor*/
(function ()
{
    var context, spy = mentor.publisher.popoutHandler, stubs, View = Backbone.View.extend(), Model = Backbone.Model.extend(), Collection = Backbone.Collection.extend();
    window.createAdvancedViewer = function ()
    {

    };
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
        currentPackage: new Model(),
        "DiagramsPopoverModel": new Model(),
        "RelatedDataPopoverModel": new Model(),
        "ReportsPopoverModel": new Model(),
        fileDisplayHandler: fakeFileDisplayHandler,
        SignalTracerModel: new Model(),
        ListGroupView: new View(),
        TranslationUtils: new Model(),
        "views/navigationPanelView": new View(),
        Packages: Backbone.Collection.extend({
            url: "dummy/url/packages.xml"
        })
    };

    stubs.currentPackage.set({id: "packageId"});
    context = createContext(stubs);

    stubs.SignalTracerModel.checkRendererAvailablility = function ()
    {
    };
    stubs.SignalTracerModel.rendererLicenceAvaialable = function ()
    {
    };

    stubs.TranslationUtils.translateHTMLContent = function ()
    {
    };

    context(["views/contentpanel/threeDXMLPanel", "views/contentpanel/toolbar/generalButtons",
            "views/contentpanel/customContentPanel", "models/selectedSystem", "views/contentpanel/locationViewPanel",
            "views/contentpanel/systemDiagramPanel", "views/contentpanel/toolbar/systemToolBar"],
        function (threeDXMLPanel, generalButtons, customContentPanel, selectedSystem, locationViewPanel,
            systemDiagramPanel, systemToolBar)
        {
            "use strict";
            describe("threeDXMLPanelTest", function ()
            {
                var viewRendered, org_render, fakeFileDisplayHandler,
                        initialize3DPanel, removedEventHandlersFromThreePanel;

                initialize3DPanel = function ()
                {
                    removedEventHandlersFromThreePanel = false;
                    window.xml3dPlayerReady = function ()
                    {

                    };
                    var preCreateViewer = window.createAdvancedViewer;/*, preCross = window.crossHighlightHandler*/;

                    window.crossHighlightHandler.zoomObjectIn3DXML = function ()
                    {

                    };
                    threeDXMLPanel.removeEventHandlers = function () {
                        removedEventHandlersFromThreePanel = true;
                    }

                    threeDXMLPanel.model = {
                        title: "catia",
                        path: "3dXMl.3dxml",
                        get: function ()
                        {
                            return "3dXMl.3dxml";
                        }
                    };
                    return {preCreateViewer: preCreateViewer/*, preCross: preCross*/};
                };

                beforeEach(function ()
                {
                    viewRendered = false;
                    generalButtons.templateHTML = "";
                    systemToolBar.templateHTML = "";
                    /*mentor.publisher.contentType.THREE_D_XML = "3dXML";*/
                    var Model = Backbone.Model.extend({});
                    $('body').html('');
                    $('body').append($('<div id="threeD"></div>'));
                    threeDXMLPanel.currentPackage = new Model();
                    threeDXMLPanel.selectedSystem = new Model();
                    threeDXMLPanel.templateHTML = "<%=path%><%=title%>";
                    threeDXMLPanel.container = "#threeD";
                    window.LoadMask = {
                        removeLoadMask: function ()
                        {
                        },
                        addLoadMask: function ()
                        {
                        },
                        LoadSVGMask: function ()
                        {

                        }
                    };

                });

                it("should be able to load threeDXMLPanel module", function ()
                {
                    expect(threeDXMLPanel).toBeDefined();
                });

                it("should be able to render",
                    function ()
                    {
                        var preMethod = window.xml3dPlayerReady;
                        var __ret = initialize3DPanel();
                        var preCreateViewer = __ret.preCreateViewer;
                        /*var preCross = __ret.preCross;*/
                        threeDXMLPanel.render();
                        expect($('body').html()).toBe('<div id="threeD"><div class="toolbar background"></div>3dXMl.3dxml3dXMl.3dxml</div>');
                        window.xml3dPlayerReady = preMethod;
                        /*window.crossHighlightHandler = preCross;*/
                        window.createAdvancedViewer = preCreateViewer;

                    });

                it("should be able to render customPanelView",
                    function ()
                    {
                        var model = new Model();
                        model.set("mainText", "test");
                        model.set("path", "test.html");
                        selectedSystem.set("customContent", model, {silent: true});
                        customContentPanel.container = "#threeD";
                        customContentPanel.templateHTML = "<%=path%>";
                        customContentPanel.render();
                        expect($('body').html()).toBe('<div id="threeD"><div class="toolbar background"></div>test.html?packageId=12da</div>');
                    });

                it("should be able to render locationViewPanel",
                    function ()
                    {
                        /*  var twoSVghand = window.TwoDSVGEventHandler;
                         window.TwoDSVGEventHandler = function() {

                         } ;*/
                        selectedSystem.set("locationView",
                            {mainText: "2dView", path: "test.svg", type: mentor.publisher.contentType.LOCATION_VIEWS},
                            {silent: true});
                        locationViewPanel.container = "#threeD";
                        locationViewPanel.templateHTML = "<%=path%>";
                        locationViewPanel.render();
                        expect($('body').html()).toBe('<div id="threeD"><div class="toolbar background"></div>test.svg?packageId=12da</div>');
                        // window.TwoDSVGEventHandler = twoSVghand;
                    });

                it("should be able to render systemDiagramPanel",
                    function ()
                    {
                        var toolbarcreated = false;
                        systemDiagramPanel.createToolBar = function ()
                        {
                            toolbarcreated = true;
                        };
                        selectedSystem.set("systemId", "testSystemId", {silent: true});
                        selectedSystem.set("diagramId", "diagramId", {silent: true});
                        selectedSystem.set("path", "path", {silent: true});
                        systemDiagramPanel.container = "#threeD";
                        systemDiagramPanel.templateHTML = "<%=path%>";
                        systemDiagramPanel.render();
                        expect($('body').html()).toBe('<div id="threeD">path?packageId=12da</div>');
                        expect(toolbarcreated).toBeTruthy();
                    });

                it("should close 3D panel if user opens different project", function ()
                {
                    initialize3DPanel();
                    expect(threeDXMLPanel.model).toBeDefined();
                    stubs.currentPackage.trigger("change:projectId");
                    expect(threeDXMLPanel.model).toBe('');
                    expect(removedEventHandlersFromThreePanel).toBeTruthy();

                });

                afterEach(function ()
                {
                    $("#threeD").remove();
                });

            });

        }, function (err)
        {
            describe("contentPanelTestFailed", function ()
            {
                it("should load the test and dependencies", function ()
                {
                    expect(err).toBeUndefined();
                });
            });
        });
})();

