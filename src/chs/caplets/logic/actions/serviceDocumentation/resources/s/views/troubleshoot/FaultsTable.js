/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

define(
        [
            'backbone',
            'underscore',
            'text!templates/troubleshoot/faultsTable.html',
            'preferences',
            'models/troubleshoot/guidedDiagnosticsConfig',
            'collections/faults',
            'functions/troubleshoot/computeFaultObjects',
            'functions/troubleshoot/openGuidedDiagnostics',
            'functions/troubleshoot/generateDiagram',
            'currentPackage',
            'functions/troubleshoot/getFaultTableColumnBasedComparator',
        ],
        function (
                Backbone,
                _,
                html,
                preferences,
                guidedDiagnosticsConfig,
                faults,
                computeFaultObjects,
                openGuidedDiagnostics,
                generateDiagram,
                selectedPackage,
                getFaultTableColumnBasedComparator
        ) {

            return Backbone.View.extend({

                events: {
                    "click #whats-in-common": "onWhatsInCommonButtonClick",
                    "click #troubleshoot": "onDiagnosticsButtonClick",
                    "click input": "onToggleClick",
                    "click label.checker": "onToggleClickByLabelClick",
                    "click #clear-codes-button": "onClearButtonClick",
                    "click #fault-table-header-container": "sectionToggleHandler",
                    "click .faults-table-row-delete": "deleteFaultTableRow",
                    "click .sortable-column,sort-indicator": 'onSortableColumnClick',
                },

                sort: {
                    columnIndex: 2,
                    ascending: true,
                },

                initialize(options)
                {
                    const {faultCodesModel, faultObjectsModel} = options;

                    this.faultCodesModel = faultCodesModel;
                    this.faultObjectsModel = faultObjectsModel;
                    this.isSectionCollapsed = true;

                    this.faultCodesModel.on({
                        didAddCode: this.render,
                        didRemoveCode: this.render,
                        didClearCodes: this.render,
                        didUpdateCodes: this.updateUIState,
                    }, this);

                    this.faultObjectsModel.on('change', this.updateWhatsInCommonButtonState, this);

                    preferences.on('change:language', this.render, this);
                    selectedPackage.on('change:language', this.render, this);

                    this.faultObjectsModel.set(this.computeFaultObjects());
                },

                onWhatsInCommonButtonClick(event)
                {
                    mentor.publisher.stopEventFlow(event);

                    this.faultObjectsModel.set(this.computeFaultObjects());
                    this.faultObjectsModel.trigger("didWhatsInCommonClicked");
                },

                onDiagnosticsButtonClick(event)
                {
                    mentor.publisher.stopEventFlow(event);

                    LoadMask.addLoadMask('detail');
                    openGuidedDiagnostics(this.faultCodesModel.getActiveCodes())
                            .fail(function (jqXHR, textStatus, errorThrown) {
                                alert("Unable to start diagnostics session.");
                                console.log(textStatus);
                                console.log(errorThrown);
                            })
                            .always(function () {
                                LoadMask.removeLoadMask();
                            });
                },

                sectionToggleHandler(evt)
                {
                    const $ele = $(evt.target).parent().find('.orient-inner');
                    $ele.hasClass('expanded') ? this.sectionHide() : this.sectionShow();
                    this.$('#fault-objects-table-header-container > div').removeClass('hover');
                },

                isExpanded()
                {
                    return !this.isSectionCollapsed;
                },

                updateSectionCollapsed()
                {
                    if (this.isSectionCollapsed) {
                        this.isSectionCollapsed = !(this.faultCodesModel.getAllCodes().length > 0);
                    }
                },

                sectionHide()
                {
                    this.$('.orient-inner').removeClass('expanded').addClass('collapsed');
                    $('.fullFaultCodeTable, #troubleshootingPanel > div.faults-table > div.controls').hide();
                    this.isSectionCollapsed = true;
                },

                sectionShow()
                {
                    this.$('.orient-inner').removeClass('collapsed').addClass('expanded');
                    $('.fullFaultCodeTable, #troubleshootingPanel > div.faults-table > div.controls').show();
                    this.isSectionCollapsed = false;
                },

                onToggleClick(event) {
                    const target = $(event.target);
                    const checked = target.is(':checked');
                    const id = target.attr('id');
                    const keys = id === 'all-fault-codes' ? this.faultCodesModel.keys() : [faults.findById(id.replace('fault-code-', '')).get('code')];

                    this.faultCodesModel.update(keys, checked);
                },


                onToggleClickByLabelClick(event) {
                    const target = $(event.target);
                    if (target.is('label.checker') && event.triggeredByCode) {
                        const input=document.getElementById(target.attr('for'));
                        setTimeout(() => {
                            const checked = input.checked;
                            const id = input.id;
                            const keys = id === 'all-fault-codes' ? this.faultCodesModel.keys() : [faults.findById(id.replace('fault-code-', '')).get('code')];
                            this.faultCodesModel.update(keys, checked);
                        }, 0);
                    }
                },


                deleteFaultTableRow(event)
                {
                    const rowToBeDeleted = $(event.target).closest('tr');
                    const index = rowToBeDeleted.find('input').attr('id').replace('fault-code-', '');
                    const fault = faults.findById(index);
                    const code = fault && fault.get('code');
                    rowToBeDeleted.remove();
                    this.faultCodesModel.remove(code);
                    this.faultCodesModel.trigger("deleteRow");
                },

                onClearButtonClick()
                {
                    this.faultCodesModel.clear();
                },

                render()
                {
                    this.updateSectionCollapsed();
                    this.$el.html(_.template(html)({
                        faults: this.getSortedFaults(),
                        localize: mentor.publisher.languageTranslator.localize.bind(
                                mentor.publisher.languageTranslator),
                        translatePlainText: Utils.translatePlainText.bind(Utils),
                        getSortIndicator: this.getSortIndicator.bind(this),
                        isSectionCollapsed: this.isSectionCollapsed,
                    }));
                    this.updateUIState();
                    return this;
                },

                updateUIState()
                {
                    this.updateClearCodesButtonState();
                    this.updateWhatsInCommonButtonState();
                    this.updateGuidedDiagnosticsButtonState();
                    this.updateCheckboxes();
                },

                updateClearCodesButtonState()
                {
                    const faultCodesModelIsEmpty = this.faultCodesModel.isEmpty();
                    this.$('#clear-codes-button').prop('disabled', faultCodesModelIsEmpty);
                    this.$('#all-fault-codes').prop('disabled', faultCodesModelIsEmpty);
                },

                updateWhatsInCommonButtonState()
                {
                    const activeCodesLength = this.faultCodesModel.getActiveCodes().length;
                    const faultObjectsModelKeysLength = this.faultObjectsModel.keys().length;
                    this.$('#whats-in-common').prop('disabled',
                            activeCodesLength === 0 || faultObjectsModelKeysLength !== 0);
                },

                updateGuidedDiagnosticsButtonState()
                {
                    const config = guidedDiagnosticsConfig();
                    if (config) {
                        const activeCodes = this.faultCodesModel.getCodes(false, true);
                        const supportsMultipleCodes = config.supportsMultipleCodes;
                        const activeCodesLength = activeCodes.length;
                        this.$('#troubleshoot').prop('disabled',
                                activeCodesLength === 0 || (!supportsMultipleCodes && activeCodesLength > 1));
                    }
                    else {
                        this.$('#troubleshoot').remove();
                    }
                },

                onSortableColumnClick(event)
                {
                    mentor.publisher.stopEventFlow(event);
                    var element = $(event.target);
                    if (element[0].tagName.toLowerCase() == "span") {
                        element = element.parent();
                    }
                    var columnIndex = element.data('column-index');
                    if (columnIndex == this.sort.columnIndex) {
                        this.sort.ascending = !this.sort.ascending;
                    }
                    else {
                        this.sort.columnIndex = columnIndex;
                        this.sort.ascending = true;
                    }
                    this.render();
                },

                getSortIndicator(columnIndex)
                {
                    if (columnIndex !== this.sort.columnIndex) {
                        return "";
                    }
                    return this.sort.ascending ? "&#9650;" : "&#9660;"
                },

                updateCheckboxes()
                {
                    const allCodes = this.faultCodesModel.getCodes();

                    allCodes.forEach((code) => {
                        const isActive = this.faultCodesModel.isActive(code);
                        const index = faults.get(code).get('index');
                        const codeElem = this.$(`#fault-code-${index}`);
                        const labelElem = $(`label[for='fault-code-${index}']`);
                        codeElem.prop('checked', isActive);
                        labelElem.attr('title', isActive
                                ? mentor.publisher.languageTranslator.localize(
                                        'TroubleshootingPanel.FaultsTable.FaultsRow.ToInActiveToolTip')
                                : mentor.publisher.languageTranslator.localize(
                                        'TroubleshootingPanel.FaultsTable.FaultsRow.ToActiveToolTip'));
                    });

                    const activeCodes = this.faultCodesModel.getCodes(false, true);
                    const passiveCodes = this.faultCodesModel.getCodes(true, false);
                    const allCodesElem = this.$('#all-fault-codes');
                    allCodesElem.prop('checked', allCodes.length == activeCodes.length && allCodes.length > 0);
                    allCodesElem.prop('indeterminate',
                            allCodes.length != activeCodes.length && allCodes.length > 0 && allCodes.length !=
                            passiveCodes.length);
                    allCodesElem.prop('disabled', allCodes.length == 0);
                    const allCodesLabelElem = $(`label[for='all-fault-codes']`);
                    allCodesLabelElem.attr('title', activeCodes.length == allCodes.length && allCodes.length > 0
                            ? mentor.publisher.languageTranslator.localize(
                                    'TroubleshootingPanel.FaultsTable.FaultsRow.DeSelectAll.ToolTip')
                            : mentor.publisher.languageTranslator.localize(
                                    'TroubleshootingPanel.FaultsTable.FaultsRow.SelectAll.ToolTip'));
                },

                getFaults()
                {
                    return faults.filter((fault) => {
                        return this.faultCodesModel.has(fault.get('code'));
                    });
                },

                getSortedFaults()
                {
                    const faultTableFaults = this.getFaults();
                    const faultCodesModel = this.faultCodesModel;
                    return mentor.publisher.filter.applyFilter(faultTableFaults)
                            .map((fault) => {
                                const code = fault.get('code');
                                const description = fault.get('description');
                                const checkBoxState = faultCodesModel.get(code);
                                const index = fault.get('index');
                                return {index, code, description, checkBoxState};
                            })
                            .sort(getFaultTableColumnBasedComparator(this.sort.columnIndex, this.sort.ascending));
                },

                computeFaultObjects()
                {
                    const allCodes = this.faultCodesModel.getAllCodes();
                    const activeCodes = this.faultCodesModel.getActiveCodes();
                    return computeFaultObjects(allCodes, activeCodes);
                }

            });
        }
);
