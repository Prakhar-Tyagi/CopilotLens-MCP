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
    var context, spy = mentor.publisher.popoutHandler, stubs, Model = Backbone.Model.extend(), Collection = Backbone.Collection.extend(), View = Backbone.View.extend(), preTwoDSVGHandler = window.TwoDSVGEventHandler;
    window.TwoDSVGEventHandler =function() {

    };
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
        "SignalTracerModel": new Model(),
        "RelatedDataPopoverModel": new Model(),
        "ReportsPopoverModel": new Model(),
        "ListGroupView" : new View(),
        "DiagramsPopoverModel": new Model(),
        "views/navigationPanelView" : new View(),
        "TranslationUtils": {
            translateHTMLContent: function ()
            {
            }
        },
        "internalLinkHandler": {
            addMouseEventListener: function ()
            {
            }

        },
        fileDisplayHandler: fakeFileDisplayHandler
    };

    stubs.currentPackage.set({id: "packageId"});
    context = createContext(stubs);

    context(["views/contentpanel/customContentPanel", "currentPackage", "models/selectedSystem",
                "views/contentpanel/toolbar/contentToolBar"/*, "views/navigationPanelView"*/],
            function (customContentPanel, currentPackage, selectedSystem, toolBarObj, navigationPanelView) {
                "use strict";

                function configurePDFJS() {
                    mentor.publisher.config = {
                        "use-pdfjs": false
                    };
                }

                describe("customContentPanelTest", function ()
                {
                    var oldGetObjects, toolBarRenderMethod, customDataModel, Model, preContentArea;

                    beforeEach(function ()
                    {
                        var expect_method = expect;
                        preContentArea = mentor.publisher.contentArea;
                        mentor.publisher.contentArea = {
                            layoutContentPanel: function (content)
                            {
                                // expect_method(content.type).toBeDefined();
                            },
                            closeExistingPanel: function ()
                            {
                            }
                        };
                        $("body").append("<div id='dummyId'></div>");

                        Model = Backbone.Model.extend({});
                        customDataModel = new Model();
                        toolBarRenderMethod = toolBarObj.render;
                        customContentPanel.templateHTML = "<%=path%>, <%=title%>, <%=contentType%>";
                        customContentPanel.container = "#dummyId";

                    });

                    it("should be able to load customContentPanel Module", function ()
                    {
                        expect(customContentPanel).toBeDefined();
                    });

                    it("should be able render a file type with html extension with toolbar", function ()
                    {
                        configurePDFJS();
                        var toolBardata;
                        customDataModel.set({mainText: "testTitle", path: "somePath.html"});

                        customContentPanel.createToolBar = function (toolBar, content) {
                            toolBardata = content;
                        };
                        selectedSystem.set("customContent", customDataModel, {silent: true});
                        customContentPanel.render();
                        expect(customContentPanel.$el.html()).toBe('somePath.html?packageId=12da, testTitle, text/html');
                    });

                    it("should be able to render a svg file type with event handling attached", function ()
                    {
                        var zoomPanAdded, isFinishCalling, toolBarCreated;
                        getPluginType = function ()
                        {
                            return "image/svg+xml";
                        };
                        customDataModel.set({mainText: "svgName", path: "somePath.svg"});

                        customContentPanel.createToolBar = function (toolBar, content)
                        {
                            toolBarCreated = true;
                        };


                        selectedSystem.set("customContent", customDataModel, {silent: true});
                        runs(function () {
                            configurePDFJS();
                            customContentPanel.render();
                        }, "Render view");

                        waitsFor(function ()
                        {
                            return toolBarCreated;
                        }, "wait for view to render", 5000);

                        runs(function ()
                        {
                            expect(toolBarCreated).toBeTruthy();
                            expect($("#dummyId").html()).toBe('somePath.svg?packageId=12da, svgName, image/svg+xml');
                        }, "execute assert condition");

                    });

                    it("should be able to render a pdf file using pdfjs", function ()
                    {
                        var pathInfor = customContentPanel.convertPDFPathToUsePDFJSIfConfigured("pdfFilePath.pdf", "application/pdf", true);
                        expect(pathInfor.path).toBe("pdfjs/web/viewer.html?file=../../pdfFilePath.pdf");

                    });

                    it("should not use pdfjs if its not configured", function ()
                    {
                        var pathInfor = customContentPanel.convertPDFPathToUsePDFJSIfConfigured("pdfFilePath.pdf", "application/pdf", false);
                        expect(pathInfor.path).toBe("pdfFilePath.pdf");

                    });

                    it("should not decode URI when SC is accessed as file and not hosted on a server", function ()
                    {
                        var path = "pdf File Path.pdf";
                        path = encodeURI(path);
                        expect(path).toBe("pdf%20File%20Path.pdf");
                    });

                    it("should be re render when customData changes", function ()
                    {
                        var reRendered;
                        customDataModel.set({mainText: "tstTifsdtle", path: "somePath.html"});
                        customContentPanel.render = function ()
                        {
                            reRendered = true;
                        };
                        configurePDFJS();
                        customContentPanel.off();
                        customContentPanel.initialize();
                        selectedSystem.set("customContent", customDataModel);
                        expect(reRendered).toBeTruthy();
                    });


                    it("should reset the data when system changes", function () {
                        var reRendered;
                        customDataModel.set({mainText: "tstTifsdtle", path: "somePath.html"});
                        configurePDFJS();
                        customContentPanel.render = function () {
                            reRendered = true;
                        };
                        customContentPanel.off();
                        customContentPanel.initialize();
                        selectedSystem.set("customContent", customDataModel);
                        selectedSystem.set("systemId", "changed");
                        expect(selectedSystem.set("customContent").path).toBeUndefined();
                    });

                    it("should be able to unselect Navigation Panel", function ()
                    {
                        spyOn(selectedSystem, "trigger");
                        customDataModel.set({mainText: "testTitle", path: "somePath.html"});
                        selectedSystem.set("customContent", customDataModel);
                        customContentPanel.unselectInNavigationPanel();
                        expect(selectedSystem.trigger).toHaveBeenCalled();
                    });

                    it("it should be able append SVGPan.js to custom SVGs", function ()
                    {
                        var svgPanJS = customContentPanel.getSVGPanJsRelativePath("packet\\package\\resources\\diaram.svg");
                        expect(svgPanJS).toBe("../../../s/SVGPan.js");
                    });

                    afterEach(function () {
                        mentor.publisher.contentArea = preContentArea;
                        toolBarObj.render = toolBarRenderMethod;
                        customDataModel.set({mainText: "testTitle", path: "somePath.html"});
                        selectedSystem.set("customContent", customDataModel, {silent: true});
                        $("#dummyId").html('');
                    });

                    it('should add underline CSS for mapped object', function() {
                        document.body.innerHTML = '<div id="pdfElement">PDF content</div>'

                        var pdfElement = document.getElementById('pdfElement');
                        customContentPanel.attachHoverBehaviour(pdfElement);
                        const event = new MouseEvent('mouseover');
                        pdfElement.dispatchEvent(event);

                        var computedStyle = window.getComputedStyle(pdfElement);
                        setTimeout(() => {
                            expect(computedStyle.getPropertyValue('text-decoration')).toBe('underline');
                        },800);
                    });

                });
            }, function (err)
            {
                describe("customContentPanelTestFailed", function ()
                {
                    it("failed to load customContentPanel", function ()
                    {
                        expect(err).toBeUndefined();
                    });
                });
            });
    window.TwoDSVGEventHandler = preTwoDSVGHandler;


})();

