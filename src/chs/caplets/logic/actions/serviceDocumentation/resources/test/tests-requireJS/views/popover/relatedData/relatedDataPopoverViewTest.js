/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

/*global define, Utils, window, require, describe, expect, it, listTemplateForTest*/
(function () {
    "use strict";
    var mockPack = new (Backbone.Model.extend())(), context, stubs, xrefContent;

    stubs = {
        currentPackage: mockPack,
        jquery: $,
        underscore: _,
        backbone: Backbone,
        PopoverFilterModel: mockPack,
    }
    context = createContext(stubs);

    context(['views/p/relatedData/relatedDataPopoverView', "RelatedDataPopoverModel"], function (relatedDataPopoverView, relatedDataPopoverModel) {
        describe("relatedDataPopoverViewTest", function () {

            beforeEach(function () {
                stubs.currentPackage.set("id", "projectId");
                $('body').append($('<div class="relateddata_filter"><div class="listItem"></div> </div>'));
            });
            it("should be able to load relatedDataPopoverView Module", function () {
                expect(relatedDataPopoverView).toBeDefined();
            });
            it("should render", function () {

                $("body").html("");
                relatedDataPopoverView.container = 'body';
                relatedDataPopoverModel.get = function (key) {
                    if (key == "popoverModel") {
                        return true;
                    }
                    if ('x' || 'y') {
                        return 10;
                    }
                }
                relatedDataPopoverView.templateHTML = "<div><%=title%></div><div><%=x%></div><div><%=y%></div>"
                relatedDataPopoverView.render();
                expect($("body").html()).toBe("<div>RelatedDataPopoverViewTitle</div><div>10</div><div>10</div>");

            })


        });
    }, function (err) {
        describe("RelatedDataPopoverTest - module load Error", function () {
            it("Module load failed", function () {
                console.log(err);
                expect(false).toBeTruthy();
            });
        });
    });
})();

