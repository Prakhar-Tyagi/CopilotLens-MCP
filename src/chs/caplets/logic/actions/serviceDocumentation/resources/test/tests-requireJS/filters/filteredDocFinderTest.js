/*global define, describe, it, beforeEach, expect*/
define(["views/filteredDocFinder"], function (filteredDocFinder)
{
    "use strict";
    describe("filteredDocFinderTest", function ()
    {
        var searchText,
            testItem;
        it("should use indexed search when it is enabled", function ()
        {
            searchText = "testDev";
            testItem = {id: "testID123", mainText: "testDev", type: "svg"};
            var result = filteredDocFinder.loadFilteredDocs(/*ignored*/"testID123", /*ignored*/"svg");
            expect(result.mainText).toBe("testDev");
        });
        it("should return first item when there is no text search", function ()
        {
            searchText = "";
            testItem = {id: "testID123", mainText: "testDev", type: "svg"};
            var result = filteredDocFinder.loadFilteredDocs(/*ignored*/"testID123", /*ignored*/"svg");
            expect(result).toBe(filteredDocFinder.SHOW_FIRST_SECTION_ITEM);
        });
        it("should be able to return first filtered diagram for systems", function ()
        {
            searchText = "sampleText";
            testItem = {id: "testID123", diagrams: [{id: "systemDia"}], reports: [{id: "systemReport"}]};
            var filteredDoc = filteredDocFinder.getFirstFilteredDoc("testID123");
            expect(filteredDoc.type).toBe("svg");
            expect(filteredDoc.id).toBe("systemDia");
        });
        it("should be able to return first filtered report  for systems when no diagram is present", function ()
        {
            searchText = "sampleText";
            testItem = {id: "testID123", reports: [{id: "systemReport"}]};
            var filteredDoc = filteredDocFinder.getFirstFilteredDoc("testID123");
            expect(filteredDoc.type).toBe("reports");
            expect(filteredDoc.id).toBe("systemReport");
        });

        it("should be able to return first filtered diagram for harness layouts", function ()
        {
            searchText = "sampleText";
            testItem = {id: "testID123", diagrams: [{id: "harnessDia"}], reports: [{id: "harnessReport"}]};
            var filteredDoc = filteredDocFinder.getFirstFilteredDoc("testID123", "harness-layouts", "diagrams",
                "group");
            expect(filteredDoc.group).toBe("diagrams");
            expect(filteredDoc.id).toBe("harnessDia");
        });
        it("should be able to return first filtered report  for harness layouts when no diagram is present", function ()
        {
            searchText = "sampleText";
            testItem = {id: "testID123", reports: [{id: "harnessReport"}]};
            var filteredDoc = filteredDocFinder.getFirstFilteredDoc("testID123", "harness-layouts", "diagrams",
                "group");
            expect(filteredDoc.group).toBe("reports");
            expect(filteredDoc.id).toBe("harnessReport");
        });

        it("should be able to return first filtered custom design data when no diagram/report matches",
            function ()
            {
                searchText = "sampleText";
                testItem = {"splice reports": [{id: "splice report", path:"test.html"}]};
                var filteredDoc = filteredDocFinder.
                    getFirstFilteredDoc("testID123", "systems");
                expect(filteredDoc.type).toBe("customView");
                expect(filteredDoc.id).toBe("splice report");
                expect(filteredDoc.customDataType).toBe("splice reports");
            });
        beforeEach(function ()
        {
            testItem = "";
            filteredDocFinder.setCurrentPackage({
                get: function ()
                {
                    return searchText;
                }
            });
            filteredDocFinder.areIndexesGen = function ()
            {
                return true;
            };

            filteredDocFinder.setDocFinder({
                getFilteredDataById: function ()
                {
                    return testItem;
                }
            });
        });
    });
})
;