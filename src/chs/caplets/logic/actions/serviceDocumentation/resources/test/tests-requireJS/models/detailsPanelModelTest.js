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
    var context, stubs;

    stubs = {
        currentPackage : {
            getFirstSection : function () {
                return {
                    listItems : function () {
                        return [
                            {mainText : "someText"}
                        ];
                    }
                }
            }
        }
    };
    context = createContext(stubs);

    context(['models/detailsPanelModel'], function (detailPanelModel) {

        describe("detailPanelModelTest", function () {
            it("should be able to load SectionCollection Module", function () {
                expect(detailPanelModel).toBeDefined();
            });

            it("should be able to load item of first navigation panel section", function () {
                detailPanelModel.fetch();
                expect(detailPanelModel.firstItem.get("mainText")).toBe("someText");
            });
        });

    });
})();


