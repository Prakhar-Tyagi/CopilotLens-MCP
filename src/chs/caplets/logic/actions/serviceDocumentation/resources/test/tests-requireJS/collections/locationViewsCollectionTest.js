/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
(function () {
    "use strict";
    var reportType, fakeProject, stubs, context;
    fakeProject = {
        getByType : function (type) {
            describe("type should be LocationViews", function () {
                it("LocationViews should get fetched by type", function () {
                    expect(type).toBe("LocationViews");
                });
            });
        }
    };
    collectionTest("LocationViewsCollectionTest", fakeProject, "getByType", "LocationViews")();

    stubs = {
        SectionCollection : Backbone.Collection,
        currentPackage : new Backbone.Model()
    };
    context = createContext(stubs);

    context(["LocationViews"], function (collection) {
        describe("LocationViewsOrderingTest", function () {
            it("should sort alphanumerically.", function () {
                var project, data, actual;

                project = {
                    getByType : function (type) {
                        return [{
                            mainText: "01-A"
                        }, {
                            mainText: "101-A"
                        }, {
                            mainText: "12-A"
                        }];
                    }
                }
                data = collection.getData(project);
                actual = data.map(function (it)
                {
                    return it.mainText;
                });

                expect(actual[0]).toBe('01-A');
                expect(actual[1]).toBe('12-A');
                expect(actual[2]).toBe('101-A');
            });
        });
    });
})();
