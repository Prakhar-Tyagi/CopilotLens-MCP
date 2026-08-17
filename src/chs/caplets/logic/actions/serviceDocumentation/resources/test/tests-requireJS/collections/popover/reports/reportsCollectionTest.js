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

    require(["DiagramsCollection"], function (diagramsCollection) {
        var collection = diagramsCollection.clone();
        describe("ReportsCollectionTest", function () {
            it(" should keep the collection sorted.", function () {
                collection.reset([{
                    "mainText": "Zero"
                }, {
                    "mainText": "Dark"
                }, {
                    "mainText": "Thirty"
                }]);

                var result = collection
                    .map(function (x) {
                        return x.get("mainText");
                    })
                    .toString();

                expect(result).toBe('Dark,Thirty,Zero');
            });
        });
    }, function (err) {
        describe("ReportsCollectionTest", function () {
            it(" should load the collection.", function () {
                expect(err).toBeFalsy();
            });
        });
    });
})();
