/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */
// define(["ComponentLoader"], function(componentLoader) {
(function () {
    'use strict';

    var stubs = {
        PopoverItemView : Backbone.View.extend({}),
        'models/selectedSystem': Backbone.Model.extend({})
    };
    var context = createContext(stubs);

    context(["PopoverItemView"], function(popoverItemView) {
        describe("documentObjectPopover2Test", function () {
            var publisher = mentor.publisher;
            var designObjectPopover = publisher.designObjectPopover;
            var origComponentLoader = designObjectPopover.getComponentLoader;
            var orig_getDocumentEventHandler=publisher.documentObjectSection.getDocumentEventHandler;

            beforeEach(function () {
                designObjectPopover.getComponentLoader = function() {
                    return {
                        loadComponents: function () {
                            console.log("Mocked ComponentLoader#loadComponent");
                        },
                        getComponentViewByName: function() {
                            return Backbone.View.extend({});
                        }
                    };
                };
                expect(popoverItemView).toBeDefined();
            });

            afterEach(function() {
                designObjectPopover.getComponentLoader = origComponentLoader;
            });

            it("test addSection", function () {
                spyOn(designObjectPopover, "loadHTMLTemplateAndRenderView");
                designObjectPopover.addSection("TestSection");
                // TODO: how can we match a single argument?
                // expect(designObjectPopover.loadHTMLTemplateAndRenderView).toHaveBeenCalledWith(
                //         's/templates/p/popoverPanelTemplate.html', Function,
                //         {expand: true, showPopoutBtn: true, async: true});
                expect(designObjectPopover.loadHTMLTemplateAndRenderView).toHaveBeenCalled();
            });

            it("test createDesignObjectSection", function () {
                spyOn(designObjectPopover, "loadHTMLTemplateAndRenderView").andCallThrough();
                designObjectPopover.createDesignObjectSection("TestSection", {}, {});
                expect(designObjectPopover.loadHTMLTemplateAndRenderView).toHaveBeenCalled();
            });

            it("should be able to create document object section", function () {
                var viewData={data: 123, models: [{id: 1, content: 'content1'}, {id: 2, content: 'content1'}]};
                var documentObjectSection = publisher.documentObjectSection.createDocumentObjectSection("TestView123", viewData, {});

                var data = documentObjectSection.getData();
                var title = documentObjectSection.getTitle();
                var className = documentObjectSection.getClassName();

                expect(data).toBe(viewData);
                expect(title).toBe("TestView123");
                expect(className).toBe("TestView123");
                expect(documentObjectSection.shouldShowPopup()).toBeFalsy();
            });

            it("should be able to get tool tip content", function () {
                publisher.documentObjectSection.getDocumentEventHandler=function () {
                    var documentEventHandler = new Backbone.Model({
                        toolTipContentType: {
                            loadToolTip: function (content) {
                                return {
                                    name: 'testName',
                                    desc: 'testDesc'
                                }
                            }
                        }
                    });
                    return documentEventHandler;
                };
                var documentObjectSection = publisher.documentObjectSection.createDocumentObjectSection("TestView123", {}, {});

                var tt1 = documentObjectSection.getTooltipContent(new Backbone.Model());
                expect(tt1).toBe('');

                var tt2 = documentObjectSection.getTooltipContent({
                    tooltips: {
                        name: 'testName',
                        desc: 'testDesc'
                    }
                });
                expect(tt2.name).toBe('testName');
                expect(tt2.desc).toBe('testDesc');

                var contentParam = new Backbone.Model({
                    tooltips: '',
                    type: 'toolTipContentType',
                });
                var tt3= documentObjectSection.getTooltipContent(contentParam);
                expect(tt3.name).toBe('testName');
                expect(tt3.desc).toBe('testDesc');
                publisher.documentObjectSection.getDocumentEventHandler=orig_getDocumentEventHandler;
            });

            it("should be able to popover the clicked item", function () {
                var viewData={data: 123, models: [{id: 'testId', content: 'content1'}]};
                var evt, targetElem=$('<div /> ');
                evt={
                    currentTarget: targetElem,
                    detail: {},
                };
                var config1={}
                var documentObjectSection = publisher.documentObjectSection.createDocumentObjectSection("TestView123", viewData, config1);
                documentObjectSection.popoverItemClicked(evt);

                targetElem=$('<div  data-id="testId" /> ');
                evt={
                    currentTarget: targetElem,
                    detail: {},
                };
                var config2={
                    onMouseClick: function (evt, item) {},
                };
                documentObjectSection = publisher.documentObjectSection.createDocumentObjectSection("TestView123", viewData, config2);
                spyOn(config2, "onMouseClick");
                documentObjectSection.popoverItemClicked(evt);
                expect(config2.onMouseClick).toHaveBeenCalled();

                var documentEventHandler = new Backbone.Model({
                    itemType: {
                        display: function (clickedItem, evt) {},
                    }
                });
                publisher.documentObjectSection.getDocumentEventHandler=function () {
                    return documentEventHandler;
                };
                viewData={data: 123, models: [{id: 'testId', content: 'content1', get: function (param) {return "itemType";} }]};
                var config3={};
                documentObjectSection = publisher.documentObjectSection.createDocumentObjectSection("TestView123", viewData, config3);
                spyOn(documentEventHandler.get('itemType'), "display");
                documentObjectSection.popoverItemClicked(evt);
                expect(documentEventHandler.get('itemType').display).toHaveBeenCalled();
                publisher.documentObjectSection.getDocumentEventHandler=orig_getDocumentEventHandler;
            });

            it("should be able to pop out", function () {
                var viewData={data: 123, models: [{id: 'testId1', content: 'content1'}]};
                var evt={
                    stopPropagation: function () {},
                };
                var config1={};
                var documentObjectSection = publisher.documentObjectSection.createDocumentObjectSection("TestView123", viewData, config1);
                documentObjectSection.getDataId=function (evt) {return 'testId2'};
                documentObjectSection.popOut(evt);

                var config2={
                    onPopout: function (evt, item) {},
                };
                documentObjectSection = publisher.documentObjectSection.createDocumentObjectSection("TestView123", viewData, config2);
                documentObjectSection.getDataId=function (evt) {return 'testId1'};
                spyOn(config2, "onPopout");
                spyOn(evt, "stopPropagation");
                documentObjectSection.popOut(evt);
                expect(config2.onPopout).toHaveBeenCalled();
                expect(evt.stopPropagation).toHaveBeenCalled();

                var docRequestHandler = {};
                var documentEventHandler = new Backbone.Model({
                    itemType: docRequestHandler,
                });
                publisher.documentObjectSection.getDocumentEventHandler=function () {
                    return documentEventHandler;
                };
                viewData={data: 123, models: [{id: 'testId1', content: 'content1', get: function (param) {return "itemType";} }]};
                var config3={};
                documentObjectSection = publisher.documentObjectSection.createDocumentObjectSection("TestView123", viewData, config3);
                documentObjectSection.openPopout=function (url) {};
                documentObjectSection.getDataId=function (evt) {return 'testId1'};
                spyOn(documentObjectSection, "openPopout");
                docRequestHandler.createURL= function (clickedItem, evt) {return 'testURL'};
                documentObjectSection.popOut(evt);
                expect(documentObjectSection.openPopout).toHaveBeenCalled();
                expect(evt.stopPropagation).toHaveBeenCalled();
                publisher.documentObjectSection.getDocumentEventHandler=orig_getDocumentEventHandler;
            });

            it("should be able to instantiate the document object section group", function () {
                var isSetElementCalled=false, isGetCoordinatesCalled=false,
                    isIsSignalTracerAvailableCalled=false, isGetRenderConnectivityBtnToolTipCalled=false;
                var popoverView=Backbone.Model.extend({
                        setElement: function (container) {isSetElementCalled=true;},
                        getCoordinates: function (x, y) {isGetCoordinatesCalled=true; return {x: 10, y: 10}},
                        isSignalTracerAvailable: function () {isIsSignalTracerAvailableCalled=true; return false},
                        getRenderConnectivityBtnToolTip: function () {isGetRenderConnectivityBtnToolTipCalled=true; return {}},
                        $el: $('<div id="popOver"/>')
                    }),
                    popoverTemplate='',
                    config={title:"testTitle", x: 10, y: 10};
                publisher.documentObjectSection.instantiateDocumentObjectSectionGrp(popoverView, popoverTemplate, config);
                expect(isSetElementCalled).toBeTruthy();
                expect(isGetCoordinatesCalled).toBeTruthy();
                expect(isIsSignalTracerAvailableCalled).toBeTruthy();
                expect(isGetRenderConnectivityBtnToolTipCalled).toBeTruthy();
            });

        });
    }, function (error) {
        describe("documentObjectPopover2Test - Module load failed", function () {
            it("This expectation should not be called", function () {
                console.log("package Loading Error ", error);
                expect(error).not.toBeDefined();
            });
        });
    });
})();

