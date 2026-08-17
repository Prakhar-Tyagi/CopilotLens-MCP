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

    require(["DiagramsCollection", "backbone"], function (diagramsCollection, Backbone) {
        var collection = diagramsCollection.clone();
        describe("DiagramsCollectionTest", function () {
            var origTranslate = Utils.translatePlainText;
            beforeEach(function() {
                Utils.translatePlainText = function(val) {
                    return val;
                }
            });

            afterEach(function() {
                Utils.translatePlainText = origTranslate;
            });

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
        describe("DiagramCollectionTest", function () {
            it(" should keep the collection sorted.", function () {
                expect(err).toBeFalsy();
            });
        });
    });
})();
