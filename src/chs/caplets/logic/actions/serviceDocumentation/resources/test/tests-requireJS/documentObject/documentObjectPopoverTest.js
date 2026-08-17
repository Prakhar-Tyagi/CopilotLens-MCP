/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

describe("documentObjectPopoverTest", function(){

    beforeEach(function(){
        var popoverFilterModel = new (Backbone.Model.extend({}))();
        var isWaiting = true;
        runs(function(){
            mentor.publisher.designObjectPopover.showPopover("testPopver", 0, 0, popoverFilterModel, "text!/s/templates/p/popoverTemplate.html");
            setTimeout(function() {
                isWaiting = false;
            }, 2500);
        });
        waitsFor(function(){
            return !isWaiting;
        }, 2501);
    });

    it("should be able show popover with title", function(){

        runs(function(){
            expect($("#detailPopup .component-label").text().trim()).toBe("testPopver");
        });
    });


    it("should be able show popover with filter", function(){
        runs(function(){
            expect($("#relateddata_filter input").attr("placeholder")).toBe("FilterPlaceholderText");
        });

    });
});