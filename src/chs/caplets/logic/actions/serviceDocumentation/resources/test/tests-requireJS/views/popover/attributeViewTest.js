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
    var mockPack = new (Backbone.Model.extend())(), context, stubs, xrefContent;

    stubs = {
        currentPackage : mockPack,
        jquery : $,
        underscore : _,
        backbone : Backbone,
        AttributesCollection : new (Backbone.Collection.extend())()
    };
    context = createContext(stubs);

    context(['views/p/attributesView'], function (attributesView) {
        describe("attributesViewTest", function () {

            beforeEach(function () {
                stubs.currentPackage.set("id", "projectId");
            });
            it("should be able to load attributesView Module", function () {
                expect(attributesView).toBeDefined();
            });

            it("should return the correct attributeView Title", function () {
                expect(attributesView.getTitle()).toBe("AttributesTitle");
            });

            it("should return the correct attributeView ClassName", function () {
                expect(attributesView.getClassName()).toBe("attributes");
            });

            it("should be able to show toolTip", function () {
                var eventNameArg;
                attributesView.generateEvent = function (event, eventName) {
                    eventNameArg = eventName;
                };
                //stubs.AttributesCollection.add({mainText : "name"});
                stubs.AttributesCollection.get = function () {
                    return mockPack;
                };
                attributesView.showToolTip({currentTarget : ""});
                expect(eventNameArg).toBe(mentor.publisher.events.SHOW_TOOL_TIP);
            });

            it("should be able to remove toolTip", function () {
                var eventNameArg;
                attributesView.generateEvent = function (event, eventName) {
                    eventNameArg = eventName;
                };
                attributesView.removeToolTip({});
                expect(eventNameArg).toBe(mentor.publisher.events.REMOVE_TOOL_TIP);
            });

        });
    });
})();