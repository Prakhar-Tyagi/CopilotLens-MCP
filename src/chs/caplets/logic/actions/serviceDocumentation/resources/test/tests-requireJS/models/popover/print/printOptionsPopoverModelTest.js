/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe*/
require(["PrintOptionsPopoverModel"], function (popoverModel) {

    describe("PrintOptionsPopoverModelTest", function () {
        it("should set the dimensions correctly", function () {
            var x = 10, y = 20;
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.OPEN_PRINT_POPUP,
                {x : x || x, y : y || y});
            expect(popoverModel.get('x')).toBe(10);
            expect(popoverModel.get('y')).toBe(20);
        });
    });

    describe("PrintOptionsPopoverModelTest", function () {
        it("should set the model correctly", function () {
            var packages = ["d1", "d2"];
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.OPEN_PRINT_POPUP,
                {models : packages});
            expect(popoverModel.get('popoverModel').length).toBe(2);
            expect(popoverModel.get('popoverModel')[0]).toBe("d1");
            expect(popoverModel.get('popoverModel')[1]).toBe("d2");
        });
    });

    describe("PrintOptionsPopoverModelTest", function () {
        it("should call the callback method correctly", function () {
            var check = false, callBack = function () {
                check = true;
            };
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.OPEN_PRINT_POPUP,
                {callBack : callBack});
            expect(check).toBe(true);
        });
    });

});