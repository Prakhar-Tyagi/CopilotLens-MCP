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
    };
    var context = createContext(stubs);

    context(["models/troubleshoot/FaultCodesModel", "views/troubleshoot/FaultObjectsTable"],
            function (FaultCodesModel, FaultObjectsTableView) {
                "use strict";
                describe('Fault Objects Table Test', function () {
                    beforeEach(function () {
                        mentor.publisher.LanguageFilteredProject.setCurrentLanguage("EN");
                        this.faultObjectsModel = new Backbone.Model();
                        this.faultCodesModel = new FaultCodesModel();
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
                        this.view = new FaultObjectsTableView({
                            faultObjectsModel: this.faultObjectsModel,
                            faultCodesModel: this.faultCodesModel,
                            poppedOutFaultObjectTable: false
                        });
                        mentor.publisher.filter.applyFilter = function (commonObj) {
                            return commonObj;
                        }
                        spyOn(this.faultObjectsModel, 'on');
                        spyOn(this.view, "initializeEventForReportHover");
                    });

                    it('should initialize correctly', function () {
                        expect(this.view.faultObjectsModel).toEqual(this.faultObjectsModel);
                        expect(this.view.poppedOutFaultObjectTable).toEqual(false);
                        expect(this.view.reportHandler instanceof ReportEventHandler).toBeTruthy();
                    });

                    it("Should have a sort property", function () {
                        expect(this.view.sort).toEqual({
                            columnIndex: 4,
                            ascending: false
                        });
                    });

                    it("Should have events defined", function () {
                        expect(this.view.events).toEqual({
                            "click #fault-objects-diagram-button": "onDiagramButtonClick",
                            "click .sortable-column": 'onSortableColumnClick',
                            "click #fault-objects-table-header-container": "sectionToggleHandler",
                        });
                    });

                    it('should render when the faultObjectsModel changes', function () {
                        spyOn(this.view, 'getContentInfoKey');
                        spyOn(this.view, 'getCommonObjects');
                        spyOn(this.view, 'updateDiagramButtonState');
                        spyOn(this.view, 'updateSectionCollapsed');
                        this.view.faultObjectsModel.trigger('change:commonObjects');
                        expect(this.view.getCommonObjects).toHaveBeenCalled();
                    });

                    it('should render correctly', function () {
                        spyOn(this.view, 'render').andCallThrough();
                        spyOn(this.view, 'getCommonObjects');
                        spyOn(Utils, 'translatePlainText');
                        spyOn(this.view, 'getContentInfoKey');
                        spyOn(this.view, 'updateDiagramButtonState');
                        spyOn(this.view, 'updateSectionCollapsed');
                        spyOn(mentor.publisher.languageTranslator, 'localize');
                        this.view.render();
                        expect(this.view.getCommonObjects).toHaveBeenCalled();
                        expect(mentor.publisher.languageTranslator.localize).toHaveBeenCalled();
                    });

                    it('renders the common objects correctly', function () {
                        this.view.render();
                        expect(this.view.$el.html()).toContain('Connector');
                    });

                    it('should disable the diagram button if there are no common objects', function () {
                        this.view.faultObjectsModel.set('commonObjects', []);
                        this.view.updateDiagramButtonState([]);
                        expect(this.view.$('#fault-objects-diagram-button').prop('disabled')).toBe(true);
                    });

                    it('should enable the diagram button if there are common objects', function () {
                        this.view.faultObjectsModel.set('commonObjects',
                                [{
                                    id: 1,
                                    type: 'type1',
                                    column2Value: 'value2',
                                    column3Value: 'value3',
                                    codes: [1, 2, 3]
                                }]);
                        this.view.updateDiagramButtonState(
                                [{
                                    id: 1,
                                    type: 'type1',
                                    column2Value: 'value2',
                                    column3Value: 'value3',
                                    codes: [1, 2, 3]
                                }]);
                        expect(this.view.$('#fault-objects-diagram-button').prop('disabled')).toBe(false);
                    });

                    it('should disable the pop out button if there are no common objects', function () {
                        this.view.faultObjectsModel.set('commonObjects', []);
                        this.view.render();
                        expect(this.view.$('#fault-objects-table-popout').prop('disabled')).toBe(true);
                    });

                    it('should enable the pop out button if there are common objects', function () {
                        this.view.faultObjectsModel.set('commonObjects',
                                [{
                                    id: 1,
                                    type: 'type1',
                                    column2Value: 'value2',
                                    column3Value: 'value3',
                                    codes: [1, 2, 3]
                                }]);
                        this.view.render();
                        expect(this.view.$('#fault-objects-table-popout').prop('disabled')).toBe(false);
                    });

                    it('calls the onSortableColumnClick function when a sortable column is clicked', function () {
                        this.view.render();
                        spyOn(this.view, 'render').andCallThrough();
                        this.view.delegateEvents();
                        this.view.$('.sortable-column').first().trigger('click');
                        expect(this.view.render).toHaveBeenCalled();
                    });

                    it('sorts the common objects correctly when the sortable column is clicked', function () {
                        this.view.render();
                        spyOn(this.view, 'onSortableColumnClick').andCallThrough();
                        this.view.$('.sortable-column').first().click();
                        expect(this.view.$el.html()).toContain('Connector');
                        expect(this.view.$el.html()).toContain('P');
                    });

                    it('should update the state of the diagram button when common objects change', function () {
                        spyOn(this.view, 'updateDiagramButtonState');
                        this.faultObjectsModel.trigger('change:commonObjects');
                        expect(this.view.updateDiagramButtonState).toHaveBeenCalled();
                    });

                    it("Should have a getCommonObjects function", function () {
                        this.view.faultObjectsModel.set('commonObjects', [{
                            id: 1,
                            type: 'Type',
                            column2Value: 'Value 2',
                            column3Value: 'Value 3',
                            codes: [1, 2, 3]
                        }]);
                        var commonObjects = this.view.getCommonObjects();
                        expect(commonObjects.length).toEqual(1);
                        expect(commonObjects[0]).toEqual({
                            id: 1,
                            type: 'TroubleshootingPanel.FaultObjectTable.ObjectType.Type',
                            column2Value: 'Translated{Value 2}',
                            column3Value: 'Translated{Value 3}',
                            codes: [1, 2, 3]
                        });
                    });

                    it("should call translatePlainText for every commonObjects element's column2Value and column3Value",
                            function () {
                                spyOn(Utils, 'translatePlainText');
                                this.view.render();
                                expect(Utils.translatePlainText).toHaveBeenCalledWith('13591061_N7_X2');
                                expect(Utils.translatePlainText).toHaveBeenCalledWith('13591061');
                                expect(Utils.translatePlainText).toHaveBeenCalledWith('13591061_N7_X3');
                                expect(Utils.translatePlainText).toHaveBeenCalledWith('13591064');
                            });

                    it("Should have a getContentInfoKey function", function () {
                        this.view.faultObjectsModel.set('commonObjects', []);
                        expect(this.view.getContentInfoKey([])).toEqual(
                                'TroubleshootingPanel.ObjectsTable.NoCommonObjects');
                        this.view.faultObjectsModel.clear();
                        expect(this.view.getContentInfoKey([])).toEqual(
                                'TroubleshootingPanel.ObjectsTable.SelectionChanged');
                        this.view.faultObjectsModel.set('commonObjects', [{
                            id: 1,
                            type: 'Type',
                            column2Value: 'Value 2',
                            column3Value: 'Value 3',
                            codes: [1, 2, 3]
                        }]);
                        expect(this.view.getContentInfoKey([{
                            id: 1,
                            type: 'Type',
                            column2Value: 'Value 2',
                            column3Value: 'Value 3',
                            codes: [1, 2, 3]
                        }])).toEqual(null);
                    });
                    afterEach(function () {

                    });

                });
            }, function (err) {
                describe("faultObjectTableTest - module load Error", function () {
                    it("Module load failed", function () {
                        console.log(err);
                        expect(false).toBeTruthy();
                    });
                });
            });
})();




