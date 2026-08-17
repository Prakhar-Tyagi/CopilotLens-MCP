/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global require, describe, it, expect, beforeEach, Backbone, afterEach, createContext*/
(function () {
    "use strict";
    var mockPack = new (Backbone.Model.extend())(), context, stubs;

    stubs = {
        currentPackage : mockPack
    };
    context = createContext(stubs);

    context(['models/selectedSystem', "currentPackage"], function (selectedSystem, currentPackage) {

        describe("selectedSystemTest", function () {

            it("should be able to load selectedSystem Module", function () {
                expect(selectedSystem).toBeDefined();
            });
            //
            // it("should clear its data when currentPackage changes", function () {
            //     selectedSystem.set("systemId", "testSystemId", {silent : true});
            //     expect(selectedSystem.get("systemId")).toBeDefined();
            //     currentPackage.set("id", "someId");
            //     expect(selectedSystem.get("systemId")).toBeUndefined();
            // });

        });
    });
})();
