/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

(function ()
{
    "use strict";
    var selectedPackage = new (Backbone.Model.extend())(), UserSession =new (Backbone.Model.extend({kSelectedPackageProperty: 'property'}))(), configurationsModel = new (Backbone.Model.extend())(), context, stubs, customViewRendered = false;

    stubs = {
        currentPackage: selectedPackage,
        jquery: $,
        underscore: _,
        backbone: Backbone,
        ConfigurationsModel: configurationsModel,
        UserSession: UserSession,
    };
    context = createContext(stubs);

    context(['views/filters/vin/vinFilterView'], function (vinFilterView)
    {
        describe("vinFilterViewTest", function ()
        {

            beforeEach(function ()
            {
                $('body').html(
                        '<input id="vinFilterText" class="placeHolderText" value="Enter Vin and press Enter key"/>')
            });
            it("should be able to load vinFilterView Module", function ()
            {
                expect(vinFilterView).toBeDefined();
            });

            it("should be able to change VIN text when language changes", function ()
            {
                selectedPackage.trigger("change:language");
                expect($("#vinFilterText").val()).toBe("Enter Vin and press Enter key");

            });

            it("should not reset when language changes when VIN is set", function ()
            {
                $("#vinFilterText").val('vin');
                vinFilterView.translate({options: "op1"});
                expect($("#vinFilterText").val()).toBe("vin");

            });

            it("should  reset when language changes when VIN is not set", function ()
            {
                $("#vinFilterText").val('vin');
                vinFilterView.translate({options: ""});
                expect($("#vinFilterText").val()).toBe("vin");
            });

            it("should fetch the configurations upon click of the config button", function () {
                var evt = {
                    stopPropagation: function () {},
                };
                var origFetch=configurationsModel.fetch;
                configurationsModel.fetch = function (evt) {};
                spyOn(evt, "stopPropagation");
                spyOn(configurationsModel, "fetch");
                vinFilterView.onClickOfConfigButton(evt);
                expect(configurationsModel.fetch).toHaveBeenCalled();
                expect(evt.stopPropagation).toHaveBeenCalled();
                configurationsModel.fetch = origFetch;
            });

            it("should be able to call appropriate functions when the onKeyPress, onBlur, onFocus, onClickOfCrossButton events are triggered", function () {
                var origOnKeyPressOfTextField=mentor.publisher.optionFilterPanel.onKeyPressOfTextField,
                    origHidePlaceHolderText=mentor.publisher.optionFilterPanel.hidePlaceHolderText,
                    origShowPlaceHolderText=mentor.publisher.optionFilterPanel.showPlaceHolderText,
                    origCrossButtonClicked=mentor.publisher.optionFilterPanel.crossButtonClicked,
                    evt = {
                        stopPropagation: function () {}
                    }
                ;
                mentor.publisher.optionFilterPanel.onKeyPressOfTextField=function (evt) {};
                mentor.publisher.optionFilterPanel.hidePlaceHolderText=function (evt) {};
                mentor.publisher.optionFilterPanel.showPlaceHolderText=function (evt) {};
                mentor.publisher.optionFilterPanel.crossButtonClicked=function (evt, isEffectivityProj) {};
                spyOn(mentor.publisher.optionFilterPanel, "onKeyPressOfTextField");
                spyOn(evt, "stopPropagation");
                vinFilterView.onKeyPress(evt);
                expect(mentor.publisher.optionFilterPanel.onKeyPressOfTextField).toHaveBeenCalled();
                expect(evt.stopPropagation).toHaveBeenCalled();
                spyOn(mentor.publisher.optionFilterPanel, "hidePlaceHolderText");
                vinFilterView.onFocus(evt);
                expect(mentor.publisher.optionFilterPanel.hidePlaceHolderText).toHaveBeenCalled();
                spyOn(mentor.publisher.optionFilterPanel, "showPlaceHolderText");
                vinFilterView.onBlur(evt);
                expect(mentor.publisher.optionFilterPanel.showPlaceHolderText).toHaveBeenCalled();
                spyOn(mentor.publisher.optionFilterPanel, "crossButtonClicked");
                vinFilterView.onClickOfCrossButton(evt);
                expect(mentor.publisher.optionFilterPanel.crossButtonClicked).toHaveBeenCalled();
                mentor.publisher.optionFilterPanel.onKeyPressOfTextField=origOnKeyPressOfTextField;
                mentor.publisher.optionFilterPanel.hidePlaceHolderText=origHidePlaceHolderText;
                mentor.publisher.optionFilterPanel.showPlaceHolderText=origShowPlaceHolderText;
                mentor.publisher.optionFilterPanel.crossButtonClicked=origCrossButtonClicked;

            });

            it("should be able to call appropriate functions when clicked", function () {
                var origTrigger=selectedPackage.trigger;
                selectedPackage.trigger=function (config) {};
                var isWaiting = true;
                runs(function() {
                    var evt = {};
                    vinFilterView.onClick(evt);
                    spyOn(selectedPackage, 'trigger');
                    setTimeout(function() {
                        isWaiting = false;
                    }, 100);
                });

                waitsFor(function() {
                    return !isWaiting;
                }, 150);

                runs(function () {
                    expect(selectedPackage.trigger).toHaveBeenCalled();
                });

                selectedPackage.trigger=origTrigger;
            });

            it("should be able to update the html element when the onFocusOut event is triggered", function () {
                var evt = {};
                $("#vinFilterText").val('');
                vinFilterView.onFocusOut(evt);
                expect($("#vinFilterText").hasClass("placeHolderText")).toBeTruthy();
                expect($("#vinFilterText").val()).toBe('EnterVintext');
            });

            it("should be able to render", function () {
                var selPackage=new (Backbone.Model.extend({
                    effectivityRange: 'abc-def-ghi',
                    start: '0',
                    end: '0',
                }))();
                UserSession.getActiveSession = function () {
                    return {
                        get: function (id) {
                            if(id==='property'){
                                return {
                                    get: function (param) {
                                        if(param==="effectivityRange"){
                                            return 'abc-def-ghi';
                                        }
                                        if(param==='start'){
                                            return '0';
                                        }
                                        if(param=='end'){
                                            return '0';
                                        }
                                    }
                                }
                            }
                        },
                    };
                };
                vinFilterView.templateHTML = "";
                var origVINFilterView=mentor.publisher.optionFilterPanel.VINFilterView;
                mentor.publisher.optionFilterPanel.VINFilterView = function () {};
                var isWaiting = true;
                runs(function() {
                    spyOn(mentor.publisher.optionFilterPanel, "VINFilterView");
                    vinFilterView.render();
                    setTimeout(function() {
                        isWaiting = false;
                    }, 1);
                });

                waitsFor(function() {
                    return !isWaiting;
                }, 2);

                runs(function () {
                    expect(mentor.publisher.optionFilterPanel.VINFilterView).toHaveBeenCalled();
                });

                mentor.publisher.optionFilterPanel.VINFilterView=origVINFilterView;
            });

            afterEach(function ()
            {
                $("#vinFilterText").remove();
            })

        });
    });
})();


