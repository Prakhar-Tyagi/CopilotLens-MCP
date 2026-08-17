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
    var mockPack = new (Backbone.Model.extend())(), context, stubs, xrefContent, basicPopoverView;

    stubs = {
        currentPackage: mockPack,
        jquery: $,
        underscore: _,
        backbone: Backbone,
        PopoverFilterModel: mockPack
    };
    context = createContext(stubs);

    context(["BasicPopoverView"], function (BasicPopoverView) {
        describe("basicPopoverViewTest", function () {

            beforeEach(function () {
                stubs.currentPackage.set("id", "projectId");
                $('body').append($('<div id="detailPopup"><div id="targetEle" class="placeHolderText">Filter</div><div class="relateddata_filter"><div class="listItem"></div> </div> </div>'));
            });
            it("should be able to load basicPopoverView Module", function () {
                expect(BasicPopoverView).toBeDefined();
            });


            //TODO: X,Y are dependent are environment and they are fluctuating
            xit("should get coordinates", function () {
                var obj = new BasicPopoverView().getCoordinates(200, 200);
                expect(obj['x']).toBe(50);
                expect(obj['y']).toBe(-172);

                obj = new BasicPopoverView().getCoordinates(158, 200);
                expect(obj['x']).toBe(10);
                expect(obj['y']).toBe(-172);

                obj = new BasicPopoverView().getCoordinates(1000, 200);
                expect(obj['x']).toBe(448);
                expect(obj['y']).toBe(-172);
            });


            it("should be able to remove view", function () {
                var basicPopoverViewObj=new BasicPopoverView();
                expect(basicPopoverViewObj.getPopoverDiv()).toBeDefined();
                spyOn(basicPopoverViewObj, "getPopoverDiv");
                basicPopoverViewObj.removeView();
                expect(basicPopoverViewObj.getPopoverDiv).toHaveBeenCalled();
            });

            it("should be able to add text place holder", function () {
                var basicPopoverViewObj=new BasicPopoverView(),
                    evt ={
                        target:"#targetEle",
                        stopPropagation: function () {},
                    };
                spyOn(evt, "stopPropagation");
                basicPopoverViewObj.addTextPlaceHolder(evt);
                expect(evt.stopPropagation).toHaveBeenCalled();
            });

            it("should be able to remove text place holder", function () {
                var basicPopoverViewObj=new BasicPopoverView(),
                    evt ={
                        target:"#targetEle",
                        stopPropagation: function () {},
                    };
                spyOn(basicPopoverViewObj, "triggerFilter");
                spyOn(evt, "stopPropagation");
                basicPopoverViewObj.removeTextPlaceHolder(evt);
                expect(evt.stopPropagation).toHaveBeenCalled();
                expect(basicPopoverViewObj.triggerFilter).toHaveBeenCalled();
            });

            it("should trigger filter event text entered", function () {
                var basicPopoverViewObj=new BasicPopoverView(),
                    evt ={
                        target:"#targetEle",
                        stopPropagation: function () {},
                    };
                spyOn(evt, "stopPropagation");
                basicPopoverViewObj.textEntered(evt);
                expect(evt.stopPropagation).toHaveBeenCalled();
            });


        });
    }, function (err) {
        describe("basicPopoverTest - module load Error", function () {
            it("Module load failed", function () {
                console.log(err);
                expect(false).toBeTruthy();
            });
        });
    });
})();