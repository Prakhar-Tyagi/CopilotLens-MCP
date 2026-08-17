/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, describe, it, beforeEach, afterEach, expect, Backbone*/
define(["filters/documentContentBasedFilter"], function (contentBasedFilter)
{
    "use strict";
    describe("documentContentBasedFilterTest", function ()
    {
        var filteredContentFromServer;
        var collectionToFilter;

        it("should be able filter a section by a search text using json response from server", function ()
        {
            initTestData();
            var filtered = contentBasedFilter.filter(collectionToFilter, "sys");
            expect(JSON.stringify(filtered)).toBe('[{"id":"sys1","mainText":"system1"},{"id":"sys2","mainText":"system2"}]');
        });
        it("should be able get an item by id", function ()
        {
            initTestData();
            var system = contentBasedFilter.getFilteredDataById("sys1", "systems", "sys");
            expect(JSON.stringify(system)).toBe('{"diagrams":[{"id":"dia1"}]}');
        });

        it("should be able filter system based on diagram id for diagram as systems flow", function ()
        {
            initTestData();
            var isValid = contentBasedFilter.filterItems(filteredContentFromServer.systems[0], "sys1", {
                get: function ()
                {
                    return "dia1";
                }
            }, true);
            expect(isValid).toBeTruthy();
        });


        it("getFilteredDocById should return SHOW_FIRST_SECTION_ITEM for diagram as system flow", function ()
        {
            initTestData();
            window.diagramAsSystemsFlow = true;
            var doc = contentBasedFilter.getFilteredDataById("sys1", "systems", "sys");
            expect(doc).toBeTruthy(contentBasedFilter.SHOW_FIRST_SECTION_ITEM);
            window.diagramAsSystemsFlow = false;
        });

        function initTestData()
        {
            filteredContentFromServer = {
                systems: [
                    {
                        id: "sys1",
                        items: {
                            diagrams: [
                                {
                                    id: "dia1"
                                }
                            ]
                        }
                    },
                    {
                        id: "sys2",
                        items: {
                            diagrams: [
                                {
                                    id: "dia1"
                                }
                            ]
                        }
                    }
                ]
            };

            var Collection = Backbone.Collection.extend({category: "systems"});
            collectionToFilter = new Collection();
            var models = [{id: "sys1", mainText: "system1"}, {id: "sys2", mainText: "system2"},
                {id: "des", mainText: "des3"}];
            collectionToFilter.reset(models);
        }

        beforeEach(function ()
        {

            contentBasedFilter.fetchSearchIndexes = function ()
            {
            };
            contentBasedFilter.getFilteredDataFor = function ()
            {
                return filteredContentFromServer.systems;
            };
        });
        afterEach(function ()
        {
            filteredContentFromServer = [];
        });

    });

});
