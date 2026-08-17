/*global require*/
describe("PlainSearchTest", function ()
{
    var plainSearch = new PlainSearch();
    var obj1 = {
        mainText: "obj1",
        subText: "objectOne",
        nameAttr: "obj1Name",
        optionExpression: "op1",
        getToolTips: function ()
        {
            return [{
                getName: function ()
                {
                    return "obj1-tooltip-name";
                },
                getValue: function ()
                {
                    return "obj1-tt-value";
                }
            }];

        }
    };
    var obj2 = {
        mainText: "obj2",
        subText: "objectTwo",
        nameAttr: "obj2Name",
        tooltips: [{name: "color", value: "red"}],
        optionExpression: "op2"
    };

    var obj3 = {
        mainText: "Logic Design 1",
        subText: "itemThree",
        folders: "quebec\\romeo\\sierra\\golf"
    };

    var obj4 = {
        mainText: "Harness Design 1",
        subText: "itemFour",
        folders: ["tango\\uniform forxtrot oscar\\victor\\whisky", "tango\\uniform forxtrot oscar\\victor\\golf"]
    };

    var objects = [obj1, obj2, obj3, obj4];
    it("should be able to load the PlainSearch module", function ()
    {
        expect(plainSearch).toBeDefined();
    });

    it("should be able to search objects by their name", function ()
    {
        var filteredObject = plainSearch.filter(objects, "obj2Name");
        expect(JSON.stringify(filteredObject[0])).toBe(JSON.stringify(obj2));
    });

    it("should be able to search objects by their mainText", function ()
    {
        var filteredObject = plainSearch.filter(objects, "obj2");
        expect(JSON.stringify(filteredObject[0])).toBe(JSON.stringify(obj2));
    });

    it("should be able to search objects by their subText", function ()
    {
        var filteredObject = plainSearch.filter(objects, "objectTwo");
        expect(JSON.stringify(filteredObject[0])).toBe(JSON.stringify(obj2));
    });

    it("should be able to search objects by their tooltip", function ()
    {
        var filteredObject = plainSearch.filter(objects, "red");
        expect(JSON.stringify(filteredObject[0])).toBe(JSON.stringify(obj2));
    });

    it("should be able to search objects by mainText and should apply VIN", function ()
    {
        var filteredObject = plainSearch.filter(objects, "obj", "op1");
        expect(JSON.stringify(filteredObject[0])).toBe(JSON.stringify(obj1));

        filteredObject = plainSearch.filter(objects, "obj", "op2");
        expect(JSON.stringify(filteredObject[0])).toBe(JSON.stringify(obj2));
    });

    it("should be able to search objects by partial name", function ()
    {
        var filteredObject = plainSearch.filter(objects, "obj");
        expect(filteredObject.length).toBe(2);
    });

    it("should be able to search objects by tooltip value", function ()
    {
        var filteredObject = plainSearch.filter(objects, "obj1-tt-value");
        expect(JSON.stringify(filteredObject[0])).toBe(JSON.stringify(obj1));
    });

    it("should be able to search objects by folder attribute - as String", function ()
    {
        var filteredObject = plainSearch.filter(objects, "sierra");

        expect(filteredObject.length).toBe(1);
        expect(JSON.stringify(filteredObject[0])).toBe(JSON.stringify(obj3));
    });

    it("should be able to search objects by folder attribute - as array", function ()
    {
        var filteredObject = plainSearch.filter(objects, "forxtrot");
        expect(JSON.stringify(filteredObject[0])).toBe(JSON.stringify(obj4));

        filteredObject = plainSearch.filter(objects, "tango");
        expect(JSON.stringify(filteredObject[0])).toBe(JSON.stringify(obj4));
    });

    it("should be able to search objects by folder attribute - mixed objects", function ()
    {
        var filteredObject = plainSearch.filter(objects, "golf");

        expect(filteredObject.length).toBe(2);
        expect(JSON.stringify(filteredObject[0])).toBe(JSON.stringify(obj3));
        expect(JSON.stringify(filteredObject[1])).toBe(JSON.stringify(obj4));
    });

});
