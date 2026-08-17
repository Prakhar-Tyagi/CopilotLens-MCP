/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global*/
describe("UtilsTest", function ()
{
    it("test Utils module is loaded successfully", function ()
    {
        expect(Utils !== undefined).toBeTruthy();
    });
    it("test prepare path should  correct file path", function ()
    {
        var msie = Utils.is_msie, isHTTP = Utils.isHTTP;
        Utils.is_msie = function ()
        {
            return true;
        };

        Utils.isHTTP = function ()
        {
            return true;
        };
        var correctedPath = Utils.prepareFilePath("file://path\\subPath/" + "text with space");
        expect(correctedPath).toBe('file://path/subPath/text%20with%20space?packageId=12da');
        Utils.isHTTP = isHTTP;
        Utils.is_msie = msie;
    });
    it("test Utils should be able to create namespace", function ()
    {
        (typeof testnamespace !== undefined ? testnamespace = undefined : "");
        Utils.namespace("testnamespace.subname.node");
        expect(testnamespace.subname.node).toBeDefined();
    });

    it("test Utils should be able sort an array", function ()
    {
        var array = [
            {name: "zz"},
            {name: 'aa'},
            {name: 'test'},
            {name: 'tast'}
        ];
        Utils.sortArrayData(array);
        expect(JSON.stringify(array)).toBe(JSON.stringify([
            {"name": "aa"},
            {"name": "tast"},
            {"name": "test"},
            {"name": "zz"}
        ]));
    });

    it("Utils.sort should return zero if specified key not present in either object", function ()
    {
        var obj1 = {
            name: "aaa"
        };
        var obj2 = {
            name: "bbb"
        };
        var result = Utils.sort(obj1, obj2, "noSuchKey");
        expect(result).toBe(0);
    });

    it("test Utils should be able fetch URL parameters", function ()
    {
        var paramValue;
        Utils.getURL = function ()
        {
            return "file://somePar.html?param1=value&param2=value2";
        };
        paramValue = Utils.getUrlParameter("param1");
        expect(paramValue).toBe("value");
        Utils.createCookie("testCookie", "value", 1);
        //assertEquals("Utils is able to read cookie ", Utils.readCookie("testCookie"), "value");
        expect(JSON.stringify(Utils.getUrlVars())).toBe(JSON.stringify(["param1", "param2"]));
    });

    it("test Utils should be able to translate an text using dictionary", function ()
    {
        var dictionary = [], translatedValue;
        dictionary = {
            translations: {}
        };

        dictionary.translations['someText'] = "translatedValue";

        translatedValue = Utils.translateText(dictionary, "{someText}", "EN");
        expect(translatedValue).toBe("translatedValue");

        expect(Utils.translateText(dictionary, "{notTranslatable}", "EN")).toBe("{notTranslatable}");

    });

    it("test Utils be able to compare arrays", function ()
    {
        expect(Utils.compareArrayContents(["one", "two"], ["one", "two"])).toBeTruthy();

        expect(Utils.findIndexOfObject(["one", "two"], "two")).toBe( 1);
        expect(Utils.stripX("a &amp; b")).toBe("a & b");
    });

    it("should be able to show design object popover for twoD view when text match with shared object", function ()
    {
        var dataToSHow, model = new Map(), xrefs = [{sharedUID : 1, id :2}, {sharedUID : 1, id :3}];
        model.set("ILC1",xrefs);
        display2DViewsAttributes("ILC1", 0, 0, "UID", model, {
            showDesignObjectPopover: function(data) {
                dataToSHow = data;
            }
        });
        expect(dataToSHow.matches.size).toBe(1);
        expect(Array.from(dataToSHow.matches.values()).flat().length).toBe(2);
    });

    it("should be able to show design object popover for twoD view when text match with single non shared object", function ()
    {
        var dataToSHow, model = new Map(), xrefs = [{ id :2}];
        model.set("ILC1",xrefs);
        display2DViewsAttributes("ILC1", 0, 0, "UID", model, {
            showDesignObjectPopover: function(data) {
                dataToSHow = data;
            }
        });

        expect(dataToSHow.matches.size).toBe(1);
        expect(Array.from(dataToSHow.matches.values()).flat().length).toBe(1);
    });

    it("should be able to show design object popover for twoD view when text match with shared object", function ()
    {
        var dataToSHow, model = new Map(), xrefs = [{sharedUID : 1, id :2}, {sharedUID : 2, id :3}];
        model.set("ILC1",xrefs);
        display2DViewsAttributes("ILC1", 0, 0, "UID", model, {
            showLinks: function(data) {
                dataToSHow = data;
            }
        });

        expect(dataToSHow.matches.size).toBe(1);
        expect(Array.from(dataToSHow.matches.values()).flat().length).toBe(2);
    });

    it("test to verify prepareFilePath handles undefined file paths", function ()
    {
        var correctedPath = Utils.prepareFilePath(undefined);
        expect(correctedPath).toBe("");
    });

});