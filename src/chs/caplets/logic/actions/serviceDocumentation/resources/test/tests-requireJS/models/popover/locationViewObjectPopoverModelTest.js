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
    var context, stubs;

    stubs = {
        backbone : Backbone,
        PopoverModel : Backbone.Model.extend(),
        SignalTracerModel : new (Backbone.Model.extend())(),
        XRefsCollection : new (Backbone.Model.extend())()
    };
    context = createContext(stubs);

    context(['LocationViewObjectPopoverModel'], function (LocationViewObjectPopoverModel) {

        describe("LocationViewObjectPopoverModelTest", function () {
            it("should be able to load LocationViewObjectPopoverModel Module", function () {
                expect(LocationViewObjectPopoverModel).toBeDefined();
            });

            it("should extend Popover model", function () {
                expect(LocationViewObjectPopoverModel instanceof  stubs.PopoverModel).toBeTruthy();
            });

            it("when alt key is pressed then isValidEvent should return false ", function () {
                stubs.SignalTracerModel.altClickRender = true;
                expect(LocationViewObjectPopoverModel.isValidEvent()).toBeFalsy();
                stubs.SignalTracerModel.altClickRender = false;
                expect(LocationViewObjectPopoverModel.isValidEvent()).toBeTruthy();
            });

            it("should always show filter ", function () {
                LocationViewObjectPopoverModel.isDynamicNavigationActive = function () {
                    return false;
                };
                LocationViewObjectPopoverModel.loadData({});
                expect(LocationViewObjectPopoverModel.get("showFilter")).toBeTruthy();
            });

            it("should show configuration filter button when config filter is active", function () {
                LocationViewObjectPopoverModel.isDynamicNavigationActive = function () {
                    return true;
                };
                LocationViewObjectPopoverModel.loadData({});
                expect(LocationViewObjectPopoverModel.get("showXrefBuilderButton")).toBeTruthy();
            });

            it("should load system", function () {
                var testSystemId, testObjectId;
                LocationViewObjectPopoverModel.isDynamicNavigationActive = function () {
                    return false;
                };
                stubs.SignalTracerModel.update = function () {
                };

                LocationViewObjectPopoverModel.getCurrentProject = function () {
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
                LocationViewObjectPopoverModel.loadData({systems : [
                    {systemId : "testSystemId", objectId : "objectId"}
                ]});
                expect(testSystemId).toBe("testSystemId");
                expect(testObjectId).toBe("objectId");
            });

            it("should update SignalTraceModel when it gets displayed", function () {
                var testSystemId, testObjectId, testsignalFiles;
                LocationViewObjectPopoverModel.isDynamicNavigationActive = function () {
                    return false;
                };
                stubs.SignalTracerModel.update = function (signalFiles, objectId, systemId) {
                    testSystemId = systemId;
                    testObjectId = objectId;
                    testsignalFiles = signalFiles;
                };

                LocationViewObjectPopoverModel.getCurrentProject = function () {
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
                var data = LocationViewObjectPopoverModel.loadData({name : "testName", systems : [
                    {systemId : "testSystemId", objectId : "objectId"}
                ]});
                expect(testSystemId).toBe("testSystemId");
                expect(testObjectId).toBe("objectId");
                expect(testsignalFiles).toBe("testFilName");

                expect(data.getName()).toBe("testName");
                expect(data.getCrossReferences().listItems.length).toBe(1);
            });
        });

    });
})();
