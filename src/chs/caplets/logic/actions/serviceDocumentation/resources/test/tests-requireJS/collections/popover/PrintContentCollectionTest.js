require(["PrintContentCollection"], function (printContentCollection)
{
    describe("PrintContentCollectionTest", function ()
    {
        it("should be able to load PrintContentCollection", function ()
        {
            expect(printContentCollection).toBeDefined();
        });

        it("should be able to hold multiple elements with same id", function ()
        {
            var data = [];
            data.push({id: "id", mainText: "mainText1"});
            data.push({id: "id", mainText: "mainText2"})
            printContentCollection.fetch(data)
            expect(printContentCollection.length).toBe(2);
        });

        it("should be able to filter pdf in 2dLocations view", function ()
        {
            var data = [];
            data.push({id: "id1", mainText: "mainText1", type:"locationViewSVGLoadArea" , url:"abcd.pdf"});
            data.push({id: "id2", mainText: "mainText2", type:"locationViewSVGLoadArea" , url:"abcd.html"});
            data.push({id: "id3", mainText: "mainText3", type:"locationView" , url:"abcd.pdf"});
            printContentCollection.fetch(data)

            var dataThatShouldntPresent = printContentCollection.models; //dataThatShouldntPresent will be used to store elements with (type = locationViewSVGLoadArea and is a pdf)
            if (dataThatShouldntPresent) {
                dataThatShouldntPresent = dataThatShouldntPresent.filter(function (item) {
                    return (item.attributes.type && item.attributes.type === "locationViewSVGLoadArea" && item.attributes.url && item.attributes.url.endsWith(".pdf"));
                });
            }
            expect(printContentCollection.length).toBe(2);
            expect(dataThatShouldntPresent.length).toBe(0);
        });

    });

});
