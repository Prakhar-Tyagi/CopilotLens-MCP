/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, ?SISW?), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer?s 
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */
require(["CookieBasedePersistenceModel"], function (CookieBasedePersistenceModel) {
        describe("CookieBasedePersistenceModel", function () {
            it("should use default cookie duration when window.mentor is undefined", function () {
                var original = window.mentor;
                window.mentor = undefined;
                var attributes = { value: "red" };
                spyOn(Utils, 'createCookie');
                var model = new CookieBasedePersistenceModel();
                model.save(attributes);
                expect(Utils.createCookie).toHaveBeenCalledWith('', JSON.stringify(attributes), 365);
                window.mentor = original;
            });

            it("should use default cookie duration when serverConfig is missing", function () {
                var original = window.mentor;
                window.mentor = { publisher: {} };
                var attributes = { value: "red" };
                spyOn(Utils, 'createCookie');
                var model = new CookieBasedePersistenceModel();
                model.save(attributes);
                expect(Utils.createCookie).toHaveBeenCalledWith('', JSON.stringify(attributes), 365);
                window.mentor = original;
            });
        });
});





