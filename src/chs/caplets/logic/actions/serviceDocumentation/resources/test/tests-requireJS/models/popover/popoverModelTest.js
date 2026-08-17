/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global require, describe, it, expect, beforeEach, Backbone, afterEach, createContext*/
(function ()
{
    "use strict";
    var context, stubs;

    stubs = {
        currentPackage: {
            getFirstSection: function ()
            {
                return {
                    listItems: function ()
                    {
                        return [
                            {mainText: "someText"}
                        ];
                    }
                }
            }
        },
        PopoverFilterModel: new (Backbone.Model.extend({})),
        backbone: Backbone
    };
    var preFilter;
    context = createContext(stubs);

    context(['PopoverModel'], function (PopoverModel)
    {

        describe("PopoverModelTest", function ()
        {
            beforeEach(function ()
            {
                 preFilter = mentor.publisher.filter;
                mentor.publisher.filter = {
                    applyFilter: function (model)
                    {
                        return model;
                    }
                }
            });
            it("should be able to load PopoverModel Module", function ()
            {
                expect(PopoverModel).toBeDefined();
            });

            it("should be get current popover location", function ()
            {
                var popoverModel = new PopoverModel();
                popoverModel.loadPopoverData({detail: {data: "testData"}, clientX: 100, clientY: 200});
                expect(popoverModel.get("x")).toBe(100);
                expect(popoverModel.get("y")).toBe(200);
                expect(popoverModel.get("popoverModel").data).toBe("testData");
            });

            it("should toggle loadSkeleton state", function ()
            {
                var popoverModel = new PopoverModel();
                expect(popoverModel.get("loadSkeleton")).toBeUndefined();
                popoverModel.loadPopoverData({detail: {data: "testData"}, clientX: 100, clientY: 200});
                expect(popoverModel.get("loadSkeleton")).toBeDefined();
            });

            it("should generate close popover event", function ()
            {
                var popoverModel = new PopoverModel(), spy;
                expect(popoverModel.get("loadSkeleton")).toBeUndefined();
                spy = sinon.spy(mentor.publisher.eventDispatcher, "dispatchEvent");
                popoverModel.loadPopoverData({detail: {data: "testData"}, clientX: 100, clientY: 200});
                expect(mentor.publisher.eventDispatcher.dispatchEvent.getCall(0).args[0]).toBe(mentor.publisher.events.CLOSE_POPOVER);
            });

            afterEach(function(){
               mentor.publisher.filter = preFilter;
            });
        });

    });
    delete stubs.PopoverFilterModel;
    context = createContext(stubs);
    var oldCLoseEvent = mentor.publisher.events.CLOSE_POPOVER;
    mentor.publisher.events.CLOSE_POPOVER = "PopoverFilterModelClose"
    context(['PopoverFilterModel', 'PopoverModel'], function (PopoverFilterModel, PopoverModel)
    {

        describe("PopoverFilterModelTest", function ()
        {
            it("should be able to load PopoverFilterModel Module", function ()
            {
                expect(PopoverFilterModel).toBeDefined();
            });

            it("should filter popover model when searchText is set on it", function ()
            {
                var pooverFilterModel = PopoverFilterModel, modelFiltered = false;
                var popovermodel = PopoverModel;
                popovermodel.filterModel = function ()
                {
                    modelFiltered = true;
                }
                pooverFilterModel.set("popoverModel", popovermodel);
                pooverFilterModel.set("searchText", "someText");
                expect(modelFiltered).toBe(true);
            });

            it("should listen to close popover event", function ()
            {
                var pooverFilterModel = PopoverFilterModel;
                var popovermodel = PopoverModel;
                pooverFilterModel.set("searchText", "someText");
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER);
                expect(pooverFilterModel.get("searchText")).toBe('');

            });
        });

    });
    mentor.publisher.events.CLOSE_POPOVER = oldCLoseEvent;
})();
