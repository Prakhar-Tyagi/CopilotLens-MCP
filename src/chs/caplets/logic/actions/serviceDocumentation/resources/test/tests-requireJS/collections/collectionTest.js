/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*define([], function () {
 "use strict";*/
var collectionTest = function (testName, fakeProject, funName, collection, createCollection) {
    "use strict";
    createCollection = createCollection || function (CollectionInstance) {
        return CollectionInstance;
    };
    return function () {
        var mockSectionCollection = Backbone.Collection.extend(), context, stubs;


        stubs = {
            SectionCollection : mockSectionCollection,
            currentPackage : new (Backbone.Model.extend())()
        };
        context = createContext(stubs);

        context([collection], function (CollectionInstance) {
            describe(testName, function () {
                it("should be able to load Collection Module.", function () {
                    expect(createCollection(CollectionInstance)).toBeDefined();
                });

                it("should be able fetch data using getData method.", function () {
					var spy = sinon.spy(fakeProject, funName); createCollection(CollectionInstance).getData(fakeProject);
                    expect(spy.withArgs().calledOnce).toBeTruthy();
                });

                it("should be an instance of SectionCollection.", function () {
                    expect(createCollection(CollectionInstance) instanceof  mockSectionCollection).toBeTruthy();
                });
            });
        });

    };
};
/*});*/
