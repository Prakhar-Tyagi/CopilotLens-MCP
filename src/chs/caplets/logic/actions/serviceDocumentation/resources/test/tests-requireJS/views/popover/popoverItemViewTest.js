/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe, expect, it, listTemplateForTest*/
(function ()
{
    "use strict";
    var mockPack = new (Backbone.Model.extend())(), template = "<%=items.length%><%=className%><%=showTitle%><%=title%><%=JSON.stringify(items)%>", context, stubs, createDummyView = function (noData)
    {
        return {
            container: ".popoverItem",
            templateHTML: template,
            getClassName: function ()
            {
                return "testClass";
            },
            getData: function ()
            {
                var Model = Backbone.Model.extend({
                    getModels: function ()
                    {
                        if (noData) {
                            return [];
                        }
                        return [
                            {
                                get: function ()
                                {
                                    return function ()
                                    {
                                        return 'diagram1';
                                    };
                                }
                            },
                            {
                                get: function ()
                                {
                                    return function ()
                                    {
                                        return 'diagram2';
                                    };
                                }
                            }
                        ];
                    }
                });
                return new Model();
            }
        };
    };

    stubs = {
        currentPackage: mockPack,
        jquery: $,
        underscore: _,
        backbone: Backbone
    };
    context = createContext(stubs);

    context(['PopoverItemView', "models/selectedSystem"], function (PopoverItemView, selectedSystem)
    {
        describe("PopoverItemViewTest", function ()
        {

            beforeEach(function ()
            {
                $('body').append($('<div class="popoverItem"></div>'));
            });
            it("should be able to load PopoverItemView Module", function ()
            {
                expect(PopoverItemView).toBeDefined();
            });

            it("should be able to render view using template", function ()
            {
                const mockGetData=[
                    { get: () => 'apple' },
                    { get: () => '_orange' },
                    { get: () => 'banana' }
                ];
                const data={
                    title: "",
                    showTitle: "",
                    items: mockGetData,
                    className: "testClass",
                    showPopup: false,
                    expand: true,
                    totalItems: 3
                }
                var instanceView, dummyView = PopoverItemView.extend({
                    container: ".popoverItem",
                    templateHTML: template,
                    getClassName: function ()
                    {
                        return "testClass";
                    },
                    getData: function ()
                    {
                        var Model = Backbone.Model.extend({
                            getModels: function ()
                            {
                                return [
                                    {},
                                    {}
                                ];
                            }
                        });
                        return new Model();
                    },
                    getModel:function (mockGetData){
                        return data;
                    }
                });
                instanceView = new dummyView();
                instanceView.render();
                expect(instanceView.$el.html()).toBe("3testClass[{},{},{}]");
            });

            it("should not render view when there is no data", function ()
            {
                var instanceView, dummyView = PopoverItemView.extend(createDummyView(true));
                instanceView = new dummyView();
                instanceView.render();
                expect(instanceView.$el.html()).toBe("");
            });

            it("should be able to mark items active or inactive based on the fact that they are present in currentDiagram or not",
                function ()
                {
                    const mockGetData=[
                        { get: () => 'apple' },
                        { get: () => '_orange' },
                        { get: () => 'banana' }
                    ];
                    const data={
                        title: "",
                        showTitle: "",
                        items: [{"isActive":""},{"isActive":"panelitem_hide"}],
                        className: "testClass",
                        showPopup: false,
                        expand: true,
                        totalItems: 2
                    }
                    var instanceView, dummyView = PopoverItemView.extend({
                        container: ".popoverItem",
                        templateHTML: template,
                        getClassName: function () {
                            return "testClass";
                        },
                        getData: function () {
                            var Model = Backbone.Model.extend({
                                getModels: function () {
                                    return [
                                        {},
                                        {}
                                    ];
                                }
                            });
                            return new Model();
                        },
                        getModel: function (mockGetData) {
                            return data;
                        }
                    });
                    selectedSystem.set("diagramId", "diagram1", {silent: true});
                    instanceView = new dummyView();
                    instanceView.render();
                    expect(instanceView.$el.html()).toBe("2testClass[{\"isActive\":\"\"},{\"isActive\":\"panelitem_hide\"}]");
                });

            it("should be able to open Popout from popoutHandler", function ()
            {
                var instanceView,
                    origOpenPopout=mentor.publisher.popoutHandler.openPopout,
                    dummyView = PopoverItemView.extend(createDummyView(true));
                instanceView = new dummyView();
                mentor.publisher.popoutHandler.openPopout = function (url) {};
                spyOn(mentor.publisher.popoutHandler, "openPopout");
                instanceView.openPopout("testURL");
                expect(mentor.publisher.popoutHandler.openPopout).toHaveBeenCalled();
                mentor.publisher.popoutHandler.openPopout = origOpenPopout;
            });

            it("should generate event", function () {
                var instanceView,
                    dummyView = PopoverItemView.extend(createDummyView(true)),
                    origDispatchEvent = mentor.publisher.eventDispatcher.dispatchEvent;
                instanceView=new dummyView();
                spyOn(mentor.publisher.eventDispatcher, "dispatchEvent").andCallThrough();
                instanceView.generateEvent("tempEventName", {});
                expect(mentor.publisher.eventDispatcher.dispatchEvent).toHaveBeenCalled();
            });

            it("should popover the clicked item", function () {
                var instanceView,
                    dummyView = PopoverItemView.extend(createDummyView(true));
                instanceView = new dummyView();
                instanceView.displaySelectedItem=function (id) {};
                spyOn(instanceView, "displaySelectedItem");
                var evt = {
                    currentTarget: $('<div data-id="testId"/>')
                };
                instanceView.popoverItemClicked(evt);
                expect(instanceView.displaySelectedItem).toHaveBeenCalledWith('testId');
            });

            it("should sort links items", function () {
                var items;
                debugger;
                const mockGetData=[
                    { get: () => 'apple' },
                    { get: () => '_orange' },
                    { get: () => 'banana' }
                ];
                var instanceView,
                        dummyView = PopoverItemView.extend({
                            container: ".popoverItem",
                            templateHTML: template,
                            getClassName: function ()
                            {
                                return "testClass";
                            },
                            getData: function ()
                            {
                                var Model = Backbone.Model.extend({
                                    getModels: function ()
                                    {
                                        return [
                                            {},
                                            {}
                                        ];
                                    }
                                });
                                return new Model();
                            },

                        filter: function ()
                {
                    return mockGetData;
                }
            });
                instanceView = new dummyView();

               var data= instanceView.getModel(mockGetData);
                expect(data.items[0].get()).toBe("_orange");
                expect(data.items[1].get()).toBe("apple");
                expect(data.items[2].get()).toBe("banana");
            });


            afterEach(function ()
            {
                $('.popoverItem').remove();
                selectedSystem.clear();
            });

        });
    });
})();


