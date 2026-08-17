/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global require, describe, it, expect, beforeEach, afterEach, mentor*/
(function () {
    "use strict";
    var context, spy = mentor.publisher.popoutHandler, stubs, Model = Backbone.Model.extend(), Collection = Backbone.Collection.extend(), fileDisplayHandler = {
        display : function (content) {
            this.content = content;
        },
        addFileHandler : function() {
            console.log('no-op dummy handler');
        }
    }, View = function (collection) {
        return Backbone.View.extend();
    };

    stubs = {
        jquery : $,
        underscore : _,
        backbone : Backbone,
        currentPackage : new Model(),
        "DiagramsPopoverModel" : new Model(),
        "RelatedDataPopoverModel" : new Model(),
        "ReportsPopoverModel" : new Model()
    };

    stubs.currentPackage.set({id : "packageId"});
    context = createContext(stubs);

    context(['views/contentpanel/toolbar/contentToolBar', "views/contentpanel/toolbar/systemToolBar",
        "views/contentpanel/toolbar/generalButtons"],
        function (contentToolBar, systemToolBar, generalButtons) {
            var toolbar, orig_showDiagrams, orig_REMOVE_TOOL_TIP, orig_dispatchEvent, orig_printButtonClickHandler,
                orig_clickHandler, orig_stopEventFlow, orig_regenerateSVG;

            describe("contentToolBarTest", function () {
                beforeEach(function () {
                    toolbar = new contentToolBar();
                    systemToolBar.templateHTML = "systemToolBar";
                    generalButtons.templateHTML = "generalButtons";
                    mentor.publisher.clientType = "CapitalServiceExplorer";
                    orig_showDiagrams=mentor.publisher.systemData.showDiagrams;
                    mentor.publisher.systemData.showDiagrams = function(evt) {};
                    orig_REMOVE_TOOL_TIP=mentor.publisher.events.REMOVE_TOOL_TIP;
                    mentor.publisher.events.REMOVE_TOOL_TIP = '';
                    orig_dispatchEvent=mentor.publisher.eventDispatcher.dispatchEvent;
                    mentor.publisher.eventDispatcher.dispatchEvent = function(code, evt) {};
                    orig_printButtonClickHandler=mentor.publisher.printer.printButtonClickHandler;
                    mentor.publisher.printer.printButtonClickHandler = function(code, evt) {};
                    orig_clickHandler=mentor.publisher.languageTranslator.clickHandler;
                    mentor.publisher.languageTranslator.clickHandler = function(evt) {};
                    orig_stopEventFlow=mentor.publisher.stopEventFlow;
                    mentor.publisher.stopEventFlow= function () {};
                    orig_regenerateSVG=renderer.regenerateSVG;
                    renderer.regenerateSVG=function () {};
                });

                afterEach(function () {
                    mentor.publisher.clientType = undefined;
                    toolbar = new contentToolBar();
                    systemToolBar.templateHTML = "systemToolBar";
                    generalButtons.templateHTML = "generalButtons";
                    mentor.publisher.clientType = "CapitalServiceExplorer";
                    mentor.publisher.systemData.showDiagrams = orig_showDiagrams;
                    mentor.publisher.events.REMOVE_TOOL_TIP = orig_REMOVE_TOOL_TIP;
                    mentor.publisher.eventDispatcher.dispatchEvent = orig_dispatchEvent;
                    mentor.publisher.printer.printButtonClickHandler = orig_printButtonClickHandler;
                    mentor.publisher.languageTranslator.clickHandler = orig_clickHandler;
                    mentor.publisher.stopEventFlow= orig_stopEventFlow
                    renderer.regenerateSVG=orig_regenerateSVG;
                });

                it("should be able to load contentToolBar Module", function () {
                    expect(contentToolBar).toBeDefined();
                });

                // TODO: This test makes no sense.
                // Only difference in rendering with 'isSystem'=false is that it hides some buttons
                // See systemToolBar.js:196
                xit("should be able to render contentToolBar Module", function () {
                    toolbar.render({type : mentor.publisher.contentType.SYSTEM_SVG});
                    expect(toolbar.$el.html()).toBe("generalButtons");
                });

                it("should be able to render contentToolBar for a system", function () {
                    window.diagramAsSystemsObjectFactoryImpl = "";
                    toolbar.render({type : mentor.publisher.contentType.SYSTEM_SVG, isSystem : true});
                    expect(toolbar.$el.html()).toBe("systemToolBargeneralButtons");
                });
                it("should be able to showToolTip, removeToolTip, showPrint, showLanguages for general buttons", function () {
                    var toolBarGeneralButtons = new generalButtons();
                    spyOn(mentor.publisher.toolTip, "showToolTipFromEvent");
                    spyOn(mentor.publisher.eventDispatcher, "dispatchEvent");
                    spyOn(mentor.publisher.printer, "printButtonClickHandler");
                    spyOn(mentor.publisher.languageTranslator, "clickHandler");
                    var evt = {};
                    toolBarGeneralButtons.showToolTip(evt);
                    toolBarGeneralButtons.removeToolTip(evt);
                    toolBarGeneralButtons.showPrint(evt);
                    toolBarGeneralButtons.showLanguages(evt);
                    expect(mentor.publisher.toolTip.showToolTipFromEvent).toHaveBeenCalled();
                    expect(mentor.publisher.eventDispatcher.dispatchEvent).toHaveBeenCalled();
                    expect(mentor.publisher.printer.printButtonClickHandler).toHaveBeenCalled();
                    expect(mentor.publisher.languageTranslator.clickHandler).toHaveBeenCalled();
                });

                xit("should be able to showColors for general buttons", function () {
                    // Popover module isn't loading
                    var toolBarGeneralButtons = new generalButtons();
                    spyOn(mentor.publisher, "stopEventFlow");
                    var evt = {
                        currentTarget: {
                            offsetTop: 0,
                            offsetHeight: 0,
                        },
                        clientX: 0,
                    };
                    toolBarGeneralButtons.showColors(evt);
                    expect(mentor.publisher.stopEventFlow).toHaveBeenCalled();
                });

                it("should be able to regenerateSignal for general buttons", function () {
                    var toolBarGeneralButtons = new generalButtons();
                    spyOn(mentor.publisher, "stopEventFlow");
                    spyOn(renderer, "regenerateSVG");
                    var evt = {};
                    toolBarGeneralButtons.regenerateSignal(evt);
                    expect(renderer.regenerateSVG).toHaveBeenCalled();
                    expect(mentor.publisher.stopEventFlow).toHaveBeenCalled();
                });

                it("should be able to showPrint, showLanguages for contentToolBar", function () {
                    var contentToolBarObj = new contentToolBar();
                    spyOn(mentor.publisher.printer, "printButtonClickHandler");
                    spyOn(mentor.publisher.languageTranslator, "clickHandler");
                    var evt = {};
                    contentToolBarObj.showPrint(evt);
                    contentToolBarObj.showLanguages(evt);
                    expect(mentor.publisher.printer.printButtonClickHandler).toHaveBeenCalled();
                    expect(mentor.publisher.languageTranslator.clickHandler).toHaveBeenCalled();
                });

                it("should be able to get isDocumentTypeActive", function () {
                    var contentToolBarObj = new contentToolBar();
                    contentToolBarObj.options = {}
                    contentToolBarObj.layoutButtons = {
                        isDocumentTypeActive: function (options) {return true},
                    }
                    expect(contentToolBarObj.isDocumentTypeActive()).toBeTruthy();
                });

                it("should be able to get isReportsBtnActive", function () {
                    var contentToolBarObj = new contentToolBar();
                    contentToolBarObj.options = {}
                    contentToolBarObj.layoutButtons = {
                        isReportsBtnActive: function (options) {return true},
                    }
                    expect(contentToolBarObj.isReportsBtnActive()).toBeTruthy();
                });

                it("should be able to enable FaceViewsNavigation", function () {
                    var contentToolBarObj = new contentToolBar();
                    contentToolBarObj.layoutButtons = {
                        enableFaceViewsNavigation: function (config) {},
                    }
                    spyOn(contentToolBarObj.layoutButtons, "enableFaceViewsNavigation");
                    contentToolBarObj.enableFaceViewsNavigation({});
                    expect(contentToolBarObj.layoutButtons.enableFaceViewsNavigation).toHaveBeenCalled();
                });
            });

        });
})();



