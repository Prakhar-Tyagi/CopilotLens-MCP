/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe, expect, it, listTemplateForTest, Backbone, $, _, createContext*/
(function () {
    "use strict";
    var mockModel = new (Backbone.Model.extend())(), context, stubs, xrefContent, oldMethod, svgPanel, preContenttoolbar, toolbarContent, preZommtoolbar, zoomToolBarContent;

    stubs = {
        currentPackage : mockModel,
        jquery : $,
        underscore : _,
        backbone : Backbone,
        ZoomToolBarModel : mockModel,
        DiagramsPopoverModel : new (Backbone.Model.extend())(),
        RelatedDataPopoverModel : new (Backbone.Model.extend())(),
        ReportsPopoverModel : new (Backbone.Model.extend())()
    };
    context = createContext(stubs);

    context(
        ['views/contentpanel/SVGContentPanel', "views/contentpanel/toolbar/contentToolBar", "views/zoomToolBarView",
            "views/contentpanel/toolbar/systemToolBar", "views/contentpanel/toolbar/generalButtons"],
        function (SVGPanel, contentToolBar, zoomToolBarView, systemToolBar, generaltoolBar) {
            describe("SVGContentPanelTest", function () {
                beforeEach(function () {
                    svgPanel = new SVGPanel();
                    zoomToolBarView.templateHTML = "zomToolBar";
                    systemToolBar.templateHTML = "systemToolBar";
                    generaltoolBar.templateHTML = "generaltoolBar";
                    mentor.publisher.clientType = "CapitalServiceExplorer";

                    oldMethod = mentor.publisher.contentArea.closeExistingPanel;
                    mentor.publisher.contentArea.closeExistingPanel = function () {

                    };
                    $("body").append("<div id='svgPanel'></div>");
                    svgPanel.container = "#svgPanel";
                    svgPanel.templateHTML = "<%=path%>";
                });

                it("should be able to load SVGContentPanel Module", function () {
                    expect(svgPanel).toBeDefined();
                });

                it("should be able to render content using template", function () {
                    var toolbarCreated, svgprocessed, preMeth = svgPanel.createContentPanelToolbar, preMeth2 = svgPanel.processContent;
                    svgPanel.createContentPanelToolbar = function () {
                        toolbarCreated = true;
                    };
                    svgPanel.processContent = function () {
                        svgprocessed = true;
                    };
                    svgPanel.render({path : "testPath"});

                    expect(svgPanel.$el.html()).toBe('testPath');
                    expect(toolbarCreated).toBe(true);
                    expect(svgprocessed).toBe(true);

                    svgPanel.createContentPanelToolbar = preMeth;
                    svgPanel.processContent = preMeth2;
                });

                it("should create toolbar when it is opened first time", function () {
                    var toolbarCreated, state, existingContentRemoved, preMeth = svgPanel.isPanelOpen, preMeth2 = svgPanel.removeExistingContent, preMeth3 = svgPanel.createToolBar;
                    svgPanel.isPanelOpen = function () {
                        return false;
                    };
                    svgPanel.removeExistingContent = function () {
                        existingContentRemoved = true;
                    };
                    svgPanel.createToolBar = function () {
                        toolbarCreated = true;
                    };
                    svgPanel.createContentPanelToolbar();

                    expect(toolbarCreated).toBe(true);
                    expect(existingContentRemoved).toBe(true);

                    svgPanel.isPanelOpen = preMeth;
                    svgPanel.removeExistingContent = preMeth2;
                    svgPanel.createToolBar = preMeth3;
                });

                it("should update toolbar when it is reopened", function () {
                    var toolbarupdated, state, contentSVGRemoved, preMeth = svgPanel.isPanelOpen, preMeth2 = svgPanel.removeContentPanelBody, preMeth3 = svgPanel.updateToolbar;
                    svgPanel.isPanelOpen = function () {
                        return true;
                    };
                    svgPanel.removeContentPanelBody = function () {
                        contentSVGRemoved = true;
                    };
                    svgPanel.updateToolbar = function () {
                        toolbarupdated = true;
                    };
                    svgPanel.createContentPanelToolbar();

                    expect(toolbarupdated).toBe(true);
                    expect(contentSVGRemoved).toBe(true);

                    svgPanel.isPanelOpen = preMeth;
                    svgPanel.removeExistingContent = preMeth2;
                    svgPanel.createToolBar = preMeth3;
                });

                it("should be able to render content using template", function () {

                    var toolbarCreated, isPanelOpen, spy = sinon.spy(mentor.publisher.contentArea,
                        "closeExistingPanel"), spy2 = sinon.spy(mentor.publisher.detailLayoutManager,
                        "getPanelId"), spy3 = sinon.spy(mentor.publisher.contentArea,
                        "layoutContentPanel"), preMeth = mentor.publisher.svgDetailPanel;
                    window.diagramAsSystemsObjectFactoryImpl = "";
                    mentor.publisher.svgDetailPanel = function () {
                        return {
                            loadSVG : function () {

                            }
                        };
                    };

                    svgPanel.render({path : "testPath"});

                    expect(svgPanel.$el.html()).toBe('<div class="toolbar background">systemToolBargeneraltoolBar</div>testPath');

                    mentor.publisher.svgDetailPanel = preMeth;
                    mentor.publisher.detailLayoutManager.getPanelId.restore();
                    mentor.publisher.contentArea.closeExistingPanel.restore();
                    mentor.publisher.contentArea.layoutContentPanel.restore();

                });

                it("should update clear content", function () {
                    svgPanel.render({path : "testPath"});
                    svgPanel.clearContent();
                    expect(svgPanel.$el.html()).toBe('');
                });

                it("should update isArtifactEnable", function () {
                    svgPanel.enableArtifact(svgPanel.subscribedEventType);
                    expect(svgPanel.isArtifactEnable('systemReport')).toBe(true)
                    svgPanel.disableArtifact(svgPanel.subscribedEventType);
                    expect(svgPanel.isArtifactEnable('systemReport')).toBe(false);
                });

                it("should get SVG Loader", function () {
                    expect(svgPanel.getSVGLoader()).toBe(svgPanel.svgLoader);
                });

                it("should get content to display", function () {
                    var selectedSystem = {};
                    expect(JSON.stringify(svgPanel.getContentToDisplay())).toBe(JSON.stringify(selectedSystem));
                });

                it("should be able to remove content panel body", function () {
                    svgPanel.removeContentPanelBody();
                    expect(svgPanel.$el.html()).toBe('');
                });

                it("should be create zoom tool bar", function () {
                    svgPanel.createZoomToolBar();
                    expect(svgPanel.$el.html()).toBe('');
                });

                it("should update clear content", function () {
                    svgPanel.render({path : "testPath"});
                    svgPanel.clearContent();
                    expect(svgPanel.$el.html()).toBe('');
                });

                it("should update isArtifactEnable", function () {
                    svgPanel.enableArtifact(svgPanel.subscribedEventType);
                    expect(svgPanel.isArtifactEnable('systemReport')).toBe(true)
                    svgPanel.disableArtifact(svgPanel.subscribedEventType);
                    expect(svgPanel.isArtifactEnable('systemReport')).toBe(false);
                });

                it("should get SVG Loader", function () {
                    expect(svgPanel.getSVGLoader()).toBe(svgPanel.svgLoader);
                });

                afterEach(function () {
                    mentor.publisher.clientType = undefined;
                    mentor.publisher.contentArea.closeExistingPanel = oldMethod;
                    $("#svgPanel").html('');
                });

            });
        });
})();