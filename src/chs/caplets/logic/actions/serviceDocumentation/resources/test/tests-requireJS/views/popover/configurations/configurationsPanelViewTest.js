/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe, expect, it, listTemplateForTest, afterEach, createContext*/
(function () {
    "use strict";
    var context, stubs, collection, model, View, renderCount = 0;

    View = Backbone.View.extend({
        render: function () {
            renderCount += 1;
            return this;
        }
    });
    collection = new Backbone.Collection();
    model = {
        getConfigurationsCount: function () {
            return 10;
        },
        updateModel: function (evt, config) {}
    };
    stubs = {
        jquery: $,
        "underscore": _,
        "PopoverItemView": View,
        "ConfigurationsCollection": collection,
        "ConfigurationsModel": model
    };
    context = createContext(stubs);

    context(['views/p/c/configurationsPanelView'], function (view) {
        var orig_dispatchEvent, orig_REMOVE_TOOL_TIP;

        describe("configurationsPanelViewTest", function () {
            beforeEach(function () {
                renderCount = 0;
                orig_REMOVE_TOOL_TIP=mentor.publisher.events.REMOVE_TOOL_TIP;
                mentor.publisher.events.REMOVE_TOOL_TIP="";
            });

            afterEach(function () {
                mentor.publisher.events.REMOVE_TOOL_TIP=orig_REMOVE_TOOL_TIP;
            });

            it("should toggle section correctly", function () {
                var event = {
                    stopPropagation: function () {
                    }
                }
                expect(view.isExpanded()).toBeTruthy();
                view.toggleSection(event);
                expect(view.isExpanded()).toBeFalsy();
                view.toggleSection(event);
                expect(view.isExpanded()).toBeTruthy();
                expect(renderCount).toBe(2);
            });

            it("should render correctly", function () {
                var element = $("<div>Test</div>");
                view.setElement(element);
                view.setRenderedTemplateInElement("Hello");
                expect($(element).html()).toBe("Hello");
            });

            it("should test getters", function () {
                expect(view.getClassName()).toBe("configurations");
                expect(view.getTitle()).toBe("ConfigurationsTitle");
                expect(view.getData()).toBe(collection);
                expect(view.getTotalItems()).toBe(10);
            });

            it("should remove tool tip", function () {
                var evt={};
                spyOn(mentor.publisher.eventDispatcher, "dispatchEvent").andCallThrough();
                view.removeToolTip(evt);
                expect(mentor.publisher.eventDispatcher.dispatchEvent).toHaveBeenCalled();
            });

            it("should apply Configuration Filter", function () {
                var evt={};
                spyOn(model, "updateModel");
                view.applyConfigurationFilter(evt);
                expect(model.updateModel).toHaveBeenCalled();
            });

            it("should get the boolean value for shouldRenderForEmptyCollection", function () {
                expect(view.shouldRenderForEmptyCollection()).toBeTruthy();
            });

            it("should show tool tip", function () {
                var evt, targetElem=$('<div  data-type="configuration" data-name="config" data-value="value" /> '), tooltips;
                evt={
                    currentTarget: targetElem,
                    detail: {},
                };
                spyOn(mentor.publisher.eventDispatcher, "dispatchEvent").andCallThrough();
                view.showToolTip(evt);
                expect(mentor.publisher.eventDispatcher.dispatchEvent).toHaveBeenCalled();
                expect(evt.detail.getToolTips()[0].getName()).toBe('config');
                expect(evt.detail.getToolTips()[0].getValue()).toBe('value');
            });
        });
    }, function (err) {
        describe("configurationsPanelViewTest", function () {
            it("failed to load", function () {
                expect(err).toBeUndefined();
            });
        });
    });
})();