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
        'views/troubleshoot/faultCodeSelectionView': Backbone.View.extend({}),
        'views/troubleshoot/FaultObjectsTable': Backbone.View.extend({}),
        'views/troubleshoot/FaultsTable': Backbone.View.extend({}),
    };
    var context = createContext(stubs);

    context(["models/troubleshoot/FaultCodesModel", "views/troubleshoot/troubleshootPanel"],
            function (FaultCodesModel, TroubleShootPanel) {
                "use strict";
                describe('Troubleshoot Panel View Test', function () {
                    beforeEach(function () {
                        this.view = TroubleShootPanel;
                        this.faultCodesModel = new FaultCodesModel();
                        spyOn(mentor.publisher.contentArea, "closeExistingPanel");
                    });

                    afterEach(function () {
                        this.view = null;
                        this.faultCodesModel = null;

                    });

                    it("should exist", function () {
                        expect(this.view).toBeDefined();
                    });

                    it("should have a render method", function () {
                        expect(typeof this.view.render).toEqual('function');
                    });

                    it("should render the toolbar and content elements", function () {
                        spyOn(this.view, 'appendToolbar');
                        spyOn(this.view, 'appendContent');
                        this.view.render({
                            activeCodes: [],
                            passiveCodes: [],
                            poppedOutFaultObjectTable: false,
                            type: ''
                        });
                        expect(this.view.appendToolbar).toHaveBeenCalled();
                        expect(this.view.appendContent).toHaveBeenCalled();
                    });

                    it("should have an onCloseButtonClick method", function () {
                        expect(typeof this.view.onCloseButtonClick).toEqual('function');
                    });

                    // This methos is commented in troubleshootPanel.js
                    /*it("should have an onPopoutButtonClick method", function () {
                        expect(typeof this.view.onPopoutButtonClick).toEqual('function');
                    });*/

                    it("should have a resetHighlightingInFaultObjectTable method", function () {
                        expect(typeof this.view.resetHighlightingInFaultObjectTable).toEqual('function');
                    });

                    it("should have an onFaultObjectsTablePopoutButtonClick method", function () {
                        expect(typeof this.view.onFaultObjectsTablePopoutButtonClick).toEqual('function');
                    });

                    it("should have an appendToolbar method", function () {
                        expect(typeof this.view.appendToolbar).toEqual('function');
                    });

                    it("should have an appendContent method", function () {
                        expect(typeof this.view.appendContent).toEqual('function');
                    });

                    it("should have a layoutContentPanel method", function () {
                        expect(typeof this.view.layoutContentPanel).toEqual('function');
                    });

                    it("should create a FaultCodesModel instance in appendContent method and set faultCodesModel property to it",
                            function () {
                                this.view.appendContent({
                                    activeCodes: [],
                                    passiveCodes: [],
                                    poppedOutFaultObjectTable: false,
                                    type: ''
                                });
                                expect(this.view.faultCodesModel instanceof FaultCodesModel).toBeTruthy();
                            });

                    it("should call layoutContentPanel method in appendContent method", function (done) {
                        var origViewLayoutContentPanel = this.view.layoutContentPanel, layoutContentPanelCalled;
                        runs(function() {
                            layoutContentPanelCalled = false;
                            this.view.layoutContentPanel = function () {
                                layoutContentPanelCalled = true;
                            };
                            this.view.appendContent({
                                activeCodes: [],
                                passiveCodes: [],
                                poppedOutFaultObjectTable: false,
                                type: ''
                            });
                            setTimeout(() => {}, 101);
                        });

                        waitsFor(function() {
                            return !layoutContentPanelCalled;
                        }, 102);

                        runs(function() {
                           expect(layoutContentPanelCalled).toBeTruthy;
                        });
                        this.view.layoutContentPanel = origViewLayoutContentPanel;
                    });

                    it("should call layoutContentPanel method with saveInHistory argument set to true when layoutContentPanel method is called with an argument",
                            function () {
                                spyOn(this.view, 'layoutContentPanel');
                                this.view.layoutContentPanel(true);
                                expect(this.view.layoutContentPanel).toHaveBeenCalledWith(true);
                            });
                });
            }
            , function (err) {
                describe("troubleshootingPanelTest - module load Error", function () {
                    it("Module load failed", function () {
                        console.log(err);
                        expect(false).toBeTruthy();
                    });
                });
            });
})();