/**
 * Created by mukumar on 09-02-2017.
 */
describe("SearchUsingIndex tests", function ()
{
    var objUnderTest;
    beforeEach(function ()
    {
        objUnderTest = SearchUsingIndex();
    });
    it("should be able to create SearchUsingIndex object", function ()
    {
        expect(objUnderTest).toBeDefined();
    });
    it("should be able to filter data using indexes", function ()
    {
        var objects, itemsOfType, options;
        objects = [{id: "obj1", mainText: "object1", optionExpression: "op1"},
            {id: "obj2", mainText: "object2", optionExpression: "op2"}
        ];
        itemsOfType = [{id: "obj1"}];
        options = "";
        var filtered = objUnderTest.filter(objects, itemsOfType, options);
        expect(filtered.length).toBe(1);
        expect(filtered[0].mainText).toBe("object1");
    });

    it("should be able to filter data using indexes and options", function ()
    {
        var objects, itemsOfType, options;
        objects = [{id: "obj1", mainText: "object1", optionExpression: "op1"},
            {id: "obj2", mainText: "object2", optionExpression: "op2"}
        ];
        itemsOfType = [{id: "obj1"}];
        options = "op2";
        var filtered = objUnderTest.filter(objects, itemsOfType, options);
        expect(filtered.length).toBe(0);
    });

    it("should be able to filter data using options", function ()
    {
        var objects, itemsOfType, options;
        objects = [{id: "obj1", mainText: "object1", optionExpression: "op1"},
            {id: "obj2", mainText: "object2", optionExpression: "op2"}
        ];
        options = "op2";
        var filtered = objUnderTest.filter(objects, itemsOfType, options);
        expect(filtered.length).toBe(1);
        expect(filtered[0].mainText).toBe("object2");
    });
});
