describe("designObjectsDataLoaderTest", function ()
{
    "use strict";
    var obj1 = {mainText: "obj1", subText: "obj1-subText", optionExpression: "op1"};
    var obj2 = {mainText: "obj2", subText: "obj2-subText"};
    var obj3 = {mainText: "obj3", subText: "obj3-subText"};
    var mockDesignObjects = JSON.stringify([obj1, obj2, obj3]);
    var objectURL;
    var url = '';
    var indexedBasedSearchCalled = '';
    var textBasedSearchCalled = '';
    var vinOptions = '';
    beforeEach(function ()
    {
        objectURL = '';
        indexedBasedSearchCalled = '';
        textBasedSearchCalled = '';
        vinOptions = '';

        handlers.ajax = function (url, callback, async)
        {
            objectURL = url;
            callback(mockDesignObjects);
        };
        handlers.textSearch = {
            getFilter: function (isIndexSearchAvailable)
            {
                if (isIndexSearchAvailable) {
                    return {
                        filter: function (data, indexes, options)
                        {
                            indexedBasedSearchCalled = true;
                            vinOptions = options;
                            return data;
                        }
                    };
                }
                else {
                    return {
                        filter: function (data, searchText, options)
                        {
                            textBasedSearchCalled = true;
                            vinOptions = options;
                            return data;
                        }
                    };
                }
            }
        };
        url = "test/url/";
        var initMethod = getDataLoader("init");
        initMethod(["", url]);
    });
    it("should be able to load design objects when init is called", function ()
    {
        expect(objectURL).toBe(url)
        expect(handlers.loaded).toBeTruthy();
    });
    it("getObjectByAttribute should return element matching given attribute name and value", function ()
    {
        var attr = {name: "mainText", value: "obj2"};
        var options = {attributes: attr};
        var message = "getObjectByAttribute"
        var getObjectByAttribute = getDataLoader("getObjectByAttribute");
        var itemsLoaded = false;
        var onsuccess = function (payload)
        {
            itemsLoaded = true;
            expect(payload[0]).toBe(message);
            expect(payload[1].items.mainText).toBe("obj2");
            expect(payload[1].items.subText).toBe("obj2-subText");
        }
        getObjectByAttribute(message, options, onsuccess);
        expect(itemsLoaded).toBe(true);
    });

    it("getObjectByAttribute should not return element when matching element is not active for given options",
            function ()
            {
                //object obj2 has op1 option expression
                var attr = {name: "mainText", value: "obj2"};
                var options = {attributes: attr, options: "op2"};
                var message = "getObjectByAttribute"
                var getObjectByAttribute = getDataLoader("getObjectByAttribute");
                var onsuccess = function (payload)
                {

                }
                getObjectByAttribute(message, options, onsuccess);
                expect(vinOptions).toBe('op2');
            });
    it("getItems should return loaded objects", function ()
    {
        var getItemMethod = getDataLoader("getItems");
        var itemsLoaded = false;
        getItemMethod("", {
            start: 1, end: 2
        }, function (message)
        {
            expect(message[0], "getItems");
            expect(JSON.stringify(message[1])).toBe(
                    '{"start":1,"end":2,"size":3,"items":[{"mainText":"obj2","subText":"obj2-subText","totalObjects":3}]}');
            itemsLoaded = true;
        });
        expect(itemsLoaded).toBeTruthy();
    });

    it("getItems should filter by search text when index based search data is available", function ()
    {
        var getItemMethod = getDataLoader("getItems");
        var itemsLoaded = false;
        getItemMethod
        (
                "",
                {
                    start: 0, end: 1, indexes: {}
                },
                function (message)
                {
                    expect(message[0], "getItems");
                    expect(JSON.stringify(message[1])).toBe(
                            '{"start":0,"end":1,"indexes":{},"size":3,"items":[{"mainText":"obj1","subText":"obj1-subText","optionExpression":"op1","totalObjects":3}]}');
                    itemsLoaded = true;
                }
        );
        expect(indexedBasedSearchCalled).toBeTruthy();
    });

    it("getItems should filter by search text using plain filter when index based search is not available", function ()
    {
        var getItemMethod = getDataLoader("getItems");
        var itemsLoaded = false;
        getItemMethod("", {
            start: 0, end: 1, searchText: "fakeText"
        }, function (message)
        {
            expect(message[0], "getItems");
            expect(JSON.stringify(message[1])).toBe(
                    '{"start":0,"end":1,"searchText":"fakeText","size":3,"items":[{"mainText":"obj1","subText":"obj1-subText","optionExpression":"op1","totalObjects":3}]}');
            itemsLoaded = true;
        });
        expect(indexedBasedSearchCalled).toBeFalsy();
        expect(textBasedSearchCalled).toBeTruthy();
    });

    it("getItems should also filter by vinOptions if they are passed", function ()
    {
        var getItemMethod = getDataLoader("getItems");
        var itemsLoaded = false;
        getItemMethod("", {
            start: 0, end: 1, searchText: "fakeText", options: "op1"
        }, function (message)
        {
            expect(message[0], "getItems");
            expect(JSON.stringify(message[1])).toBe(
                    '{"start":0,"end":1,"searchText":"fakeText","options":"op1","size":3,' +
                    '"items":[{"mainText":"obj1","subText":"obj1-subText","optionExpression":"op1","totalObjects":3}]}');
            itemsLoaded = true;
        });
        expect(indexedBasedSearchCalled).toBeFalsy();
        expect(textBasedSearchCalled).toBeTruthy();
        expect(vinOptions).toBeTruthy("op1");
    });

});
