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
    var mockPack = new (Backbone.Model.extend())(), context, stubs;

    stubs = {
        PopoverFilterModel : mockPack,
        textSearch : function (collections) {
            return {
                matchText : function (text, searchText) {

                },
                filterByText : function (searchText) {
                    return [];
                }
            };

        }
    };
    context = createContext(stubs);

    context(['PopoverItem'], function (PopoverItem) {
        describe("PopoverItemTest", function () {
            it("should be able to load PopoverItem Module", function () {
                expect(PopoverItem).toBeDefined();
            });

            it("should be able fetch the data", function () {
                var data, popoverItem, Item = PopoverItem.extend({
                    getData : function () {
                        return [
                            {name : "testData"}
                        ];
                    }
                });
                popoverItem = new Item();
                popoverItem.fetch();
                expect(popoverItem.models[0].get("name")).toBe("testData");
                expect(popoverItem.getModels().length).toBe(1);
            });

            it("should be able filter data", function () {
                var data, popoverItem, Item = PopoverItem.extend({
                    getData : function () {
                        return [
                            {name : "testData"}
                        ];
                    },
                    applyFilter : true
                });
                popoverItem = new Item();

                popoverItem.fetch();
                mockPack.set("searchText", "someText");
                expect(popoverItem.getModels().length).toBe(0);
            });
        });
    });
})();
