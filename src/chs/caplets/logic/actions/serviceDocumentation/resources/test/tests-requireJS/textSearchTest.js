/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, Utils, window, require, describe, expect, it, Backbone, mentor, beforeEach*/
require(["textSearch"], function (textSearch) {
    "use strict";
    var mockModel = new (Backbone.Model.extend())(), mockCollection = new (Backbone.Collection.extend())({model : mockModel}), textFilter;
    mockCollection.category = "dummy";
    mockCollection.set(mentor.publisher.project.getObjects("wires"));
    textFilter = textSearch(mockCollection);

    describe("textSearchTest", function () {
		beforeEach(function(){
			textFilter.indexSearchEnabled = false;
		});
        it("should be able to load textSearch module", function () {
            expect(textFilter).toBeDefined();
        });

        it("should be able to search wire based on wire mainText", function () {
            var filteredWire = textFilter.filterByText("wire3");
            expect(filteredWire.length).toBe(2);
        });

        it("should be able to search wire based on wire subText", function () {
            var filteredWire = textFilter.filterByText("subText123");
            expect(1).toBe(filteredWire.length);
        });

        it("should be able to search wire based on wire toolTips", function () {
            var filteredWire = textFilter.filterByText("partNo1234");
            expect(2).toBe(filteredWire.length);
        });

        it("should return all wires when text filter is removed", function () {
            var filteredWire = textFilter.filterByText("");
            expect(5).toBe(filteredWire.length);
        });
		it("should search the text using indexed data when it is enabled", function () {
			var indexSearch = {id:"1"};
			var attrSearch = {id:"2"};

			textFilter.setIndexBasedSearch(indexSearch);
			textFilter.setAttrBasedSearch(attrSearch);
			textFilter.indexSearchEnabled = true;
			var whenEnabled = textFilter.getFilter();
			expect(whenEnabled).toBe(indexSearch);

			textFilter.indexSearchEnabled= false;
			var whenDisabled = textFilter.getFilter();
			expect(whenDisabled).toBe(attrSearch);
		});

        it("should search the text using attribute data collection is undefined", function () {
            var indexSearch = {id:"1"};
            var attrSearch = {id:"2"};

            textFilter.setIndexBasedSearch(indexSearch);
            textFilter.setAttrBasedSearch(attrSearch);
            textFilter.indexSearchEnabled = true;
            mockCollection.category = undefined;
            var whenEnabled = textFilter.getFilter();
            expect(whenEnabled).toBe(attrSearch);
        });

        it("should search the text using attributes data when it forced", function () {
            var textSearchUsingAttrSearch = textSearch(mockCollection, true);
            var indexSearch = {id:"1"};
            var attrSearch = {id:"2"};

            textSearchUsingAttrSearch.setIndexBasedSearch(indexSearch);
            textSearchUsingAttrSearch.setAttrBasedSearch(attrSearch);
            textSearchUsingAttrSearch.indexSearchEnabled = true;
            var whenEnabled = textSearchUsingAttrSearch.getFilter();
            expect(whenEnabled).toBe(attrSearch);
        });
    });
});
