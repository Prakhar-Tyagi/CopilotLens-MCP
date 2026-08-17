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
        currentPackage: new Backbone.Model({}),
        preferences: new Backbone.Model({}),
    };
    var context = createContext(stubs);

    context(["models/troubleshoot/FaultCodesModel", "views/troubleshoot/FaultsTable"],
            function (faultCodesModel, FaultsTableView) {
                "use strict";
                describe("faultsTableViewTest", function () {
                    beforeEach(function () {
                        this.faultObjectsModel = new Backbone.Model();
                        this.faultObjectsModel.set('commonObjects', [{
                            codes: ['B1A94', 'ActiveCode'],
                            length: 1,
                            column2Value: "13591061_N7_X2",
                            column3Value: "13591061",
                            id: "UID5908bb-1842c2e1197-b4f6d9ed0f1dbf7a42f1a4a7ea4f84d4",
                            optionExpression: "UQP && Z88",
                            type: "Connector"
                        }, {
                            codes: ['B1A96', 'PassiveCode'],
                            length: 1,
                            column2Value: "13591061_N7_X3",
                            column3Value: "13591064",
                            id: "UID5908bb-1842c2e1197-b4f6d9ed0f1dbf7a42f1a4a7ea4f84d6",
                            optionExpression: "UQP && Z89",
                            type: "Connector"
                        }]);
                        this.faultCodesModel = new faultCodesModel();
                        spyOn(console, "log");

                        this.view = new FaultsTableView({
                            faultCodesModel: this.faultCodesModel,
                            faultObjectsModel: this.faultObjectsModel
                        });
                        spyOn(this.view.faultObjectsModel, "on");
                        spyOn(this.view.faultObjectsModel, "set");
                        spyOn(this.view.faultCodesModel, "set");
                        spyOn(this.view.faultCodesModel, "on");
                        mentor.publisher.filter.applyFilter = function (commonObj) {
                            return commonObj;
                        }
                        this.view.render();
                    });

                    it("should set the fault objects model when the 'whats in common' button is clicked", function () {
                        this.view.onWhatsInCommonButtonClick({});
                        expect(this.view.faultObjectsModel.set).toHaveBeenCalledWith(this.view.computeFaultObjects());
                    });

                    it('should initialize view correctly', function () {
                        expect(this.view.faultCodesModel).toEqual(this.faultCodesModel);
                        expect(this.view.faultObjectsModel).toEqual(this.faultObjectsModel);
                    });

                    it('should call computeFaultObjects on initialize', function () {
                        spyOn(this.view, 'computeFaultObjects');
                        this.view.initialize({
                            faultCodesModel: this.faultCodesModel,
                            faultObjectsModel: this.faultObjectsModel
                        });
                        expect(this.view.computeFaultObjects).toHaveBeenCalled();
                    });

                    it('should call onWhatsInCommonButtonClick on click of the "whats-in-common" button', function () {
                        spyOn(this.view.faultCodesModel, "getActiveCodes").andReturn(['code1']);
                        this.view.faultObjectsModel = new Backbone.Model();
                        spyOn(this.view, "computeFaultObjects");
                        this.view.updateWhatsInCommonButtonState();
                        this.view.delegateEvents();
                        this.view.$el.find('#whats-in-common').click();
                        expect(this.view.computeFaultObjects).toHaveBeenCalled();
                    });

                    it('should call deleteFaultTableRow on click of the delete button', function () {
                        spyOn(this.view, "getSortedFaults").andReturn([
                            {
                                "index": 1,
                                "code": "B1300",
                                "description": "Power Door Lock Circuit Failure",
                                "checkBoxState": "passive"
                            },
                            {
                                "index": 3,
                                "code": "B1319",
                                "description": "Driver Door Ajar Circuit Failure",
                                "checkBoxState": "passive"
                            },
                            {
                                "index": 4,
                                "code": "B1327",
                                "description": "Passenger Door Ajar Circuit Failure",
                                "checkBoxState": "active"
                            }
                        ]);
                        this.view.render();
                        this.view.delegateEvents();
                        spyOn(this.view.faultCodesModel, "trigger");
                        this.view.$el.find('.faults-table-row-delete').click();
                        expect(this.view.faultCodesModel.trigger).toHaveBeenCalledWith("deleteRow");
                    });

                    it("Check if initialize method sets up the required models", function () {
                        expect(this.view.faultCodesModel).toEqual(this.faultCodesModel);
                        expect(this.view.faultObjectsModel).toEqual(this.faultObjectsModel);
                    });

                    it("Check if sectionToggleHandler method toggles the section correctly", function () {
                        var $ele = this.view.$('.orient-inner');
                        $ele.click();
                        expect(this.view.$('.orient-inner').hasClass("expanded")).toBeTruthy();
                        $ele = this.view.$('.orient-inner');
                        $ele.click();
                        expect($ele.hasClass("collapsed")).toBeTruthy();
                    });

                    it("Check if onClearButtonClick method clears the codes correctly", function () {
                        spyOn(this.faultCodesModel, "clear");
                        this.view.onClearButtonClick();
                        expect(this.faultCodesModel.clear).toHaveBeenCalled();
                    });

                    it("Check if updateClearCodesButtonState method updates the clear button correctly", function () {
                        spyOn(this.view.faultCodesModel, "isEmpty").andReturn(false);
                        this.view.updateClearCodesButtonState();
                        expect(this.view.$('#clear-codes-button').prop('disabled')).toBeFalsy();
                        expect(this.view.$('#all-fault-codes').prop('disabled')).toBeFalsy();
                    });

                    it("should update the what's in common button state", function () {
                        this.view.updateWhatsInCommonButtonState();
                        expect(this.view.$('#whats-in-common').prop('disabled')).toBe(true);
                        spyOn(this.view.faultCodesModel, "getActiveCodes").andReturn(['code1'])
                        this.view.faultObjectsModel = new Backbone.Model();
                        this.view.updateWhatsInCommonButtonState();
                        expect(this.view.$('#whats-in-common').prop('disabled')).toBe(false);
                    });
                });
            }
            , function (err) {
                describe("faultsTableTest - module load Error", function () {
                    it("Module load failed", function () {
                        console.log(err);
                        expect(false).toBeTruthy();
                    });
                });
            });
})();

