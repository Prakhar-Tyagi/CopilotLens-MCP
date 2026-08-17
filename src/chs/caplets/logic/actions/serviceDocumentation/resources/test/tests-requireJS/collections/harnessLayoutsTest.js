require(["harnessLayouts"], function (harnessLayouts)
{
    describe("harnessLayoutsTest", function ()
    {
        it("should be able to load harnessLayouts", function ()
        {
            expect(harnessLayouts).toBeTruthy();

        });

        it("should  show all haress-diagrams in diagrams popover", function ()
        {
            var dataContainer = {
                getDocumentSetId: function ()
                {
                    return "doc-id";
                },
                getDocumentSetById: function (documentSetId)
                {
                    return {
                        getDocumentsInGroupTitled: function (documentGroup)
                        {
                            expect(documentGroup).toBe("harness-diagrams");
                            return [{
                                mainText: "harnes-design1", clone: function ()
                                {
                                    return this;
                                }
                            }];
                        }
                    }
                },
                getDocumentGroup: function ()
                {
                    return "harness-diagrams";
                }
            };
            var itemsToShow = harnessLayouts.getDataToRender(dataContainer);
            expect(JSON.stringify(itemsToShow)).toBe(
                    '{' +
                    '"expand":true,' +
                    '"items":[{"mainText":"harnes-design1","isActive":""}],' +
                    '"showPopup":true,"showTitle":false,"title":"",' +
                    '"totalItems":[{"mainText":"harnes-design1","isActive":""}]}');
        });
    });

});
