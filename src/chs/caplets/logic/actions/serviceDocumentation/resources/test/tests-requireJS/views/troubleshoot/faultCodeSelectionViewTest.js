/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023 Siemens
 */
(function () {
    "use strict";

    var stubs = {
        jquery: $,
        underscore: _,
        backbone: Backbone,
        preferences: new Backbone.Model({}),
        select2: new Backbone.Model({}),
        currentPackage: new Backbone.Model({}),
    };
    var context = createContext(stubs);

    context(["models/troubleshoot/FaultCodesModel", "views/troubleshoot/faultCodeSelectionView"],
            function (faultCodesModel, FaultCodeSelectionView) {
                "use strict";
                describe('faultCodeSelectionViewTest', function () {

                    beforeEach(function () {
                        this.faultCodesModel = new faultCodesModel();
                        this.view = new FaultCodeSelectionView({faultCodesModel: this.faultCodesModel});
                        $('body').append(this.view.el);
                    });

                    afterEach(function () {
                        this.view.remove();
                    });

                    it('should initialize with the correct parameters', function () {
                        expect(this.view.faultCodesModel).toEqual(this.faultCodesModel);
                        expect(this.view.selectedPackage).toEqual(this.selectedPackage);
                    });

                    it('should call the addClearSearchIconEvent function', function () {
                        spyOn(this.view, 'addClearSearchIconEvent');
                        this.view.render();
                        expect(this.view.addClearSearchIconEvent).toHaveBeenCalled();
                    });

                    it('should expand the section when collapsed', function () {
                        this.view.render();
                        this.view.$el.find('#fault-code-selection-header').click();
                        this.view.$el.find('#fault-code-selection-header').click();
                        expect(this.view.$el.find('.orient-inner').hasClass('expanded')).toBeTruthy();
                        expect(this.view.$el.find('.orient-inner').hasClass('collapsed')).toBeFalsy();
                    });

                    it('should collapse the section when expanded', function () {
                        this.view.render();
                        this.view.$el.find('#fault-code-selection-header').click();
                        expect(this.view.$el.find('.orient-inner').hasClass('expanded')).toBeFalsy();
                        expect(this.view.$el.find('.orient-inner').hasClass('collapsed')).toBeTruthy();
                    });

                    it('should call the configureDataForSelection function', function () {
                        spyOn(this.view, 'configureDataForSelection');
                        this.view.render();
                        expect(this.view.configureDataForSelection).toHaveBeenCalled();
                    });

                    it('should register the correct events for fault selection', function () {
                        spyOn(this.view, 'registerSelectEventsForFaultSelection');
                        this.view.render();
                        expect(this.view.registerSelectEventsForFaultSelection).toHaveBeenCalled();
                    });

                    it('should call the clickClearEvent function', function () {
                        spyOn(this.view, 'clickClearEvent');
                        this.view.render();
                        expect(this.view.clickClearEvent).toHaveBeenCalled();
                    });

                    afterEach(function () {
                    });

                });

            }, function (err) {
                describe("faultCodeSelectionTest - module load Error", function () {
                    it("Module load failed", function () {
                        console.log(err);
                        expect(false).toBeTruthy();
                    });
                });
            });
})();
