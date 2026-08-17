/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe, expect, it, listTemplateForTest*/

(function () {
    "use strict";
    var selectedPackage = new (Backbone.Model.extend())(),
        mockPack = new (Backbone.Model.extend())(),
        context,
        stubs, PopoverItemView,
        harnessLayouts=new (Backbone.Model.extend({getType: function () {return "sampleType"}}))();

    stubs = {
        currentPackage : mockPack,
        jquery : $,
        underscore : _,
        backbone : Backbone,
        AttributesCollection : new (Backbone.Collection.extend())(),
        harnessLayouts: harnessLayouts,
        selectedPackage: selectedPackage,
    };
    context = createContext(stubs);

    context(['views/p/harnessPopoverView'], function (harnessPopoverView, selectedPackage) {
        describe("harnessPopoverViewTest", function () {

            beforeEach(function () {
                stubs.currentPackage.set("id", "projectId");
            });

            it("should be able to load harnessPopoverView Module", function () {
                expect(harnessPopoverView).toBeDefined();
            });
            it("should be able to get data id", function () {
                //change it to get data id correctly
                event={currentTarget:"target"}
                var eventTarget = $('target');
                $(eventTarget.parent()).attr("data-id", "testDataId");
                expect(harnessPopoverView.getDataId(event)).toBeUndefined();
            });

            it("should be able to stop event propagation", function () {
                var evt={
                    stopPropagation : function () {},
                }
                spyOn(evt, "stopPropagation");
                harnessPopoverView.stopEventPropagation(evt);
                expect(evt.stopPropagation).toHaveBeenCalled();
            });

            it("should be able to get pop out handler", function () {
                expect(harnessPopoverView.popoutHandler(), mentor.publisher.popoutHandler.openPopout);
            });

            it("should be able to get title", function () {
                expect(harnessPopoverView.getTitle()).toBe("HarnessLayouts");
            });
            it("should be able to get class name", function () {
                expect(harnessPopoverView.getClassName()).toBe("HarnessLayouts");
            });

            it("should be able to get first active system", function () {
                var harnesses = [
                    {
                        "id" : "UIDba4871-1422e294c1b-279e2bee43fed9b5aeb17e7eb27cb620",
                        "mainText" : "Aerospace Harness",
                        "tooltips" : [ ],
                        "documentSets" : [
                            {
                                "title" : "reports",
                                "type" : "html",
                                "documents" : "documents"
                            }
                        ],
                    },
                    {
                        "id" : "UIDba4871-1422e294c1b-279e2bee43fed9b5aeb17e7eb27cb620",
                        "mainText" : "Aerospace Harness",
                        "tooltips" : [ ],
                        "documentSets" : [
                            {
                                "title" : "reports",
                                "type" : "html",
                                "documents" : "documents"
                            }
                        ],
                    },
                ]
                expect(harnessPopoverView.firstActiveSystem(harnesses)).toBe(harnesses[0]);
            });

            it("should be able to get item content", function () {
                var content = {
                    listItemId:"testDiagramId",
                    id:"testDiagramId",
                    layoutId:"testDiagramId",
                    type:"harnessLayoutDiagram",
                    objectId:"testDiagramId",
                    reset:false
                };
                var origGetData=harnessPopoverView.getData;
                var harnessLayout = {
                    get: function (id) {
                        return "testDiagramId";
                    },
                    getDefaultDocument: function () {
                        return {};
                    }
                };
                harnessPopoverView.getData = function () {
                    return {
                        get: function (param) {
                            return harnessLayout;
                        }
                    };
                };
                expect(JSON.stringify(harnessPopoverView.getItemContent("itemId1"))).toBe(JSON.stringify(content));
                harnessPopoverView.getData=origGetData;
            });

            it("should be able to pop out", function ( ) {
                var evt = {
                    stopPropagation: function () {},
                };
                var origGetData=harnessPopoverView.getData;
                harnessPopoverView.getData=function () {
                    return {
                        get: function (param) {
                            return {
                                get: function (param) {
                                    return "testLayoutId";
                                }
                            }
                        }
                    }
                };

                spyOn(evt, "stopPropagation");
                harnessPopoverView.popOut(evt);
                expect(evt.stopPropagation).toHaveBeenCalled();

                harnessPopoverView.getData=origGetData;
            });

            it("should be able to show harness layout", function ( ) {
                var origGetWindowObj=harnessPopoverView.getWindowObj,
                    windowObj={
                        mentor: {
                            publisher: {
                                fileDisplayHandler: {
                                    display: function (content) {}
                                }
                            }
                        }
                    };
                ;
                harnessPopoverView.getWindowObj=function () {return windowObj};
                spyOn(harnessLayouts, "getType");
                spyOn(windowObj.mentor.publisher.fileDisplayHandler, "display");
                harnessPopoverView.showHarnessLayout({layoutId:"sampleLayoutId"});
                expect(harnessLayouts.getType).toHaveBeenCalled();
                expect(windowObj.mentor.publisher.fileDisplayHandler.display).toHaveBeenCalled();

                harnessPopoverView.getWindowObj=origGetWindowObj;
            });

            it("should be able to display content", function ( ) {
                var origResetContentPanel=mentor.publisher.detailLayoutManager.resetContentPanel,
                    origShowHarnessLayout=harnessPopoverView.showHarnessLayout
                ;
                mentor.publisher.detailLayoutManager.resetContentPanel=function () {};
                harnessPopoverView.showHarnessLayout=function (content) {};
                spyOn(mentor.publisher.detailLayoutManager, "resetContentPanel");
                spyOn(harnessPopoverView, "showHarnessLayout");
                harnessPopoverView.displayContent({});
                expect(mentor.publisher.detailLayoutManager.resetContentPanel).toHaveBeenCalled();
                expect(harnessPopoverView.showHarnessLayout).toHaveBeenCalled();
                mentor.publisher.detailLayoutManager.resetContentPanel=origResetContentPanel;
                harnessPopoverView.showHarnessLayout=origShowHarnessLayout;
            });


        });
    });
})();
