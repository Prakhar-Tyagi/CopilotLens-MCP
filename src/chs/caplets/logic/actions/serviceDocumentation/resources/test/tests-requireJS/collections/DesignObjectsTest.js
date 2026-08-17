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
        getObjects : function (type) {
            describe("fecthWiresByType", function () {
                it("wires should get fetched by type", function () {
                    expect(type).toBe("wires");
                });
            });
        }
    };
    // TODO: There is no module with name 'DesignObjects'.
    // collectionTest("DesignObjectsTest", fakeProject, "getObjects", "DesignObjects", function (Collection) {
    //     var wires = new Collection();
    //     wires.type = "wires";
    //     return wires;
    // })();
})();
