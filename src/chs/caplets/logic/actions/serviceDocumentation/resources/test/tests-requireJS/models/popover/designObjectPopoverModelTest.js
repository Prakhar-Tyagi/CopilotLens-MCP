/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global require, describe, it, expect, beforeEach, Backbone, afterEach, createContext*/
(function () {
    "use strict";
    var context, loadPopover, callOrder = 0, stubs, spy, data = [], overrideFetch = {fetch : function (model) {
        if (model.getType) {
            callOrder = callOrder + 1;
            this.callOrder = callOrder;
            this.expanded = this.expand;
            return [
                {}
            ]
        }
        return data;
    }};

    stubs = {
        currentPackage : new (Backbone.Model.extend())(),
        backbone : Backbone,
        PopoverModel : Backbone.Model.extend({
            loadPopoverData : function () {
                loadPopover = true;
            }
        }),
        SignalTracerModel : new (Backbone.Model.extend())(),
        XRefsCollection : new (Backbone.Model.extend(overrideFetch))(),
        AttributesCollection : new (Backbone.Collection.extend(overrideFetch))(),
        ConnectorFaceviewsCollection : new (Backbone.Collection.extend(overrideFetch))(),
        CustomDataCollection : new (Backbone.Collection.extend(overrideFetch))(),
        ThreeDViewCollection : new (Backbone.Collection.extend(overrideFetch))(),
        TwoDLocationCollection : new (Backbone.Collection.extend(overrideFetch))(),
        XRefActiveConfigModel : new (Backbone.Model.extend())(),
        "collections/p/groundPathCollection": new (Backbone.Collection.extend(overrideFetch))(),
        "collections/harnessPopoverColl": new (Backbone.Collection.extend(overrideFetch))()
    };
    context = createContext(stubs);

    context(['DesignObjectPopoverModel', "models/selectedSystem"], function (DesignObjectPopoverModel, selectedSystem) {

        describe("DesignObjectPopoverModelTest", function () {
            it("should be able to load DesignObjectPopoverModel Module", function () {
                expect(DesignObjectPopoverModel).toBeDefined();
            });

            it("when alt key is pressed then isValidEvent should return false ", function () {
                expect(DesignObjectPopoverModel.isValidEvent({systemId : "testSystemId"})).toBeTruthy();
                expect(DesignObjectPopoverModel.isValidEvent({systemId : ""})).toBeFalsy();
                selectedSystem.set("systemId", "test2SystemId");
                expect(DesignObjectPopoverModel.isValidEvent()).toBeTruthy();
            });

            it("should always show filter ", function () {
                DesignObjectPopoverModel.isDynamicNavigationActive = function () {
                    return true;
                };
                DesignObjectPopoverModel.loadData({});
                expect(DesignObjectPopoverModel.get("showFilter")).toBeTruthy();
            });

            it("should show configuration filter button when config filter is active", function () {
                DesignObjectPopoverModel.isDynamicNavigationActive = function () {
                    return true;
                };
                DesignObjectPopoverModel.loadData({});
                expect(DesignObjectPopoverModel.get("showXrefBuilderButton")).toBeTruthy();
            });

            it("should load system", function () {
                var testSystemId, testObjectId;
                DesignObjectPopoverModel.isDynamicNavigationActive = function () {
                    return false;
                };
                stubs.SignalTracerModel.update = function () {
                };

                DesignObjectPopoverModel.getCurrentProject = function () {
                    return {
                        loadObjectData : function (systemId, objectId) {
                            testSystemId = systemId;
                            testObjectId = objectId;
                            return {
                                getSignalTraceFiles : function () {

                                },
                                objectId : objectId,
                                systemId : systemId
                            }
                        }
                    };
                };
                DesignObjectPopoverModel.loadData({id : "objectId"});
                expect(testSystemId).toBe("test2SystemId");
                expect(testObjectId).toBe("objectId");
            });

            it("should update SignalTraceModel when it gets displayed", function () {
                var testSystemId, testObjectId, testsignalFiles;
                DesignObjectPopoverModel.isDynamicNavigationActive = function () {
                    return false;
                };
                stubs.SignalTracerModel.update = function (signalFiles, objectId, systemId) {
                    testSystemId = systemId;
                    testObjectId = objectId;
                    testsignalFiles = signalFiles;
                };

                DesignObjectPopoverModel.getCurrentProject = function () {
                    return {
                        loadObjectData : function (systemId, objectId) {
                            return {
                                getSignalTraceFiles : function () {
                                    return "testFilName";
                                },
                                objectId : objectId,
                                systemId : systemId
                            }
                        }
                    };
                };
                var data = DesignObjectPopoverModel.loadData({id : "objectId"});
                expect(testSystemId).toBe("test2SystemId");
                expect(testObjectId).toBe("objectId");
                expect(testsignalFiles).toBe("testFilName");
            });

            it("should not display popover when an object has no attributes/properties", function () {
                DesignObjectPopoverModel.getPopoverOrder = function () {
                    return [];
                };
                DesignObjectPopoverModel.loadCollections({});
                expect(mentor.publisher.eventDispatcher.dispatchEvent.getCall(0).args[0]).toBe(mentor.publisher.events.CLOSE_POPOVER);
            });

            function resetData()
            {
                stubs.AttributesCollection.callOrder = 0;
                stubs.XRefsCollection.callOrder = 0;
                stubs.XRefsCollection.expand = undefined;
                stubs.ConnectorFaceviewsCollection.callOrder = 0;
                stubs.CustomDataCollection.callOrder = 0;
                stubs.TwoDLocationCollection.callOrder = 0;
                stubs.ThreeDViewCollection.callOrder = 0;
                stubs["collections/harnessPopoverColl"].callOrder = 0;
                mentor.publisher.config = {
                    collapseAllPanelsInObjectPopover: false
                };
            }

            it("should display popover as per popover order", function () {

                DesignObjectPopoverModel.getPopoverOrder = function () {
                    return {type : ["Links", "Attributes", "FaceViews", "CustomData", "LocationView", "TwodViews"]};
                };
                data = [
                    {}
                ];
                resetData();

                DesignObjectPopoverModel.loadCollections({getType : function () {
                    return "type";
                }});

                expect(stubs.AttributesCollection.callOrder).toBe(2);
                expect(stubs.AttributesCollection.expanded).toBe(false);

                expect(stubs.XRefsCollection.callOrder).toBe(1);
                expect(stubs.XRefsCollection.expanded).toBeUndefined();

                expect(stubs.ConnectorFaceviewsCollection.callOrder).toBe(3);
                expect(stubs.ConnectorFaceviewsCollection.expanded).toBe(false);

                expect(stubs.CustomDataCollection.callOrder).toBe(4);
                expect(stubs.CustomDataCollection.expanded).toBe(false);

                expect(stubs.TwoDLocationCollection.callOrder).toBe(6);
                expect(stubs.TwoDLocationCollection.expanded).toBe(false);

                expect(stubs.ThreeDViewCollection.callOrder).toBe(5);
                expect(stubs.ThreeDViewCollection.expanded).toBe(false);
            });

            it("popover order for device connector should follow connector order", function () {

                DesignObjectPopoverModel.getPopoverOrder = function () {
                    return {Connector : ["Links", "TwodViews", "LocationView", "CustomData", "FaceViews", "Attributes",
                            "HarnessLayout"]};
                };
                data = [
                    {}
                ];
                callOrder = 0;
                resetData();

                DesignObjectPopoverModel.loadCollections({getType : function () {
                        return "DeviceConnector";
                    }});
                expect(stubs.AttributesCollection.callOrder).toBe(6);
                expect(stubs.AttributesCollection.expanded).toBe(false);

                expect(stubs.XRefsCollection.callOrder).toBe(1);
                expect(stubs.XRefsCollection.expanded).toBeUndefined();

                expect(stubs.ConnectorFaceviewsCollection.callOrder).toBe(5);
                expect(stubs.ConnectorFaceviewsCollection.expanded).toBe(false);

                expect(stubs.CustomDataCollection.callOrder).toBe(4);
                expect(stubs.CustomDataCollection.expanded).toBe(false);

                expect(stubs.TwoDLocationCollection.callOrder).toBe(2);
                expect(stubs.TwoDLocationCollection.expanded).toBe(false);

                expect(stubs.ThreeDViewCollection.callOrder).toBe(3);
                expect(stubs.ThreeDViewCollection.expanded).toBe(false);

                expect(stubs["collections/harnessPopoverColl"].callOrder).toBe(7);
                expect(stubs["collections/harnessPopoverColl"].expanded).toBe(false);
            });

            it("should respond to OPEN_OBJECT_POPUP event", function () {
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.OPEN_OBJECT_POPUP, {});
                expect(loadPopover).toBe(true);
            });

            it("should dispatch HIGHLIGHT_OBJECT_ACROSS_WINDOWS event on OPEN_OBJECT_POPUP event", function () {
                var invoked, handler;

                invoked = false;
                handler = function () {
                    invoked = true;
                };
                mentor.publisher.colors = mentor.publisher.colors || {};

                mentor.publisher.eventDispatcher.attachEventListener("HIGHLIGHT_OBJECT_ACROSS_WINDOWS", handler);
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.OPEN_OBJECT_POPUP, {
                    id: "id",
                    systemId: "systemId"
                });
                mentor.publisher.eventDispatcher.removeEventListener("HIGHLIGHT_OBJECT_ACROSS_WINDOWS", handler);

                expect(invoked).toBeTruthy();
            });
        });

    },
    function (err) {
        describe("DesignObjectPopoverModelTest", function () {
           it("should be loaded properly", function () {
               expect(err.requireModules).toBeFalsy();
           })
        });
    });
})();
