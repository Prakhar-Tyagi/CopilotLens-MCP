/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe, expect, it, listTemplateForTest*/
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
    model = new Backbone.Model({});
    stubs = {
        "PopoverItemView" : View,
        "ConfigurationsCollection" : collection,
        "ConfigurationsModel" : model,
        "BaseConfigurationsBuilderView": View,
        "currentPackage" : model,
        "views/component/ModalDialog": View
    };
    context = createContext(stubs);

    context(['views/p/c/configurationsView'], function (view) {
        describe("configurationsViewTest", function () {

            beforeEach(function () {
                stubs.currentPackage.set("id", "projectId");
            });
            it('should check if the tooltip is visible', () => {

                var event = {
                    currentTarget: $('<span id=\"tooltip\" data-value=\"Delete Configuration\"></span>')
                };

                view.showTooltipForDelete(event);

                expect(event.detail.toolTip).toEqual("Delete Configuration");
            });
        });
    }, function (err) {
        describe("configurationsViewTest", function () {
            it("failed to load", function () {
                expect(err).toBeUndefined();
            });
        });
    });
})();

