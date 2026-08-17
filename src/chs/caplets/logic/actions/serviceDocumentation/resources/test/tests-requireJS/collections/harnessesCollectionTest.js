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
    var reportType, fakeProject;
    fakeProject = {
        getByType : function (type) {
            describe("type should be harness", function () {
                it("harness should get fetched by type", function () {
                    expect(type).toBe("harness");
                });
            });
        }
    };
    collectionTest("harnessCollectionTest", fakeProject, "getByType", "Harnesses")();

    fakeProject = {
        getByType : function (type) {
            describe("type should be faultcode", function () {
                it("faultcode should get fetched by type", function () {
                    expect(type).toBe("faultcode");
                });
            });
        }
    };
    collectionTest("faultcodesCollectionTest", fakeProject, "getByType", "FaultCodes")();
})();
