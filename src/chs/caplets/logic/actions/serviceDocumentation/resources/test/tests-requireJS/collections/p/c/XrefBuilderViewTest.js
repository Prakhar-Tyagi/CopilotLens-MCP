/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*
* TODO: This test Module has loading dependency on some non AMD modules. Which causes this module to timeout on load.
*/
/*
(function () {
    require(["views/p/c/xRefsBuilderView"], function (xRefBuilderView) {
        describe("XrefBuilderViewTest", function () {
            it("should fail", function () {
                expect(null).toBeTruthy();
            });
        });
    }, function (err) {
        describe("XrefBuilderViewTest - module load Error", function () {
            it("Module load failed", function () {
                console.log(err.message + "::\n" + err.stack);
                console.dir(err);
                expect(err).toBeUndefined();
            });
        });
    });
})();
*/