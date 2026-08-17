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
            'text!templates/troubleshoot/faultObjectsTable.html',
            'text!templates/troubleshoot/faultObjectsTablePopout.html',
            'views/component/IndeterminateProgressDialog',
            'currentPackage',
            'models/troubleshoot/guidedDiagnosticsConfig',
            'collections/faults',
            'collections/faultSignals',
            'functions/troubleshoot/generateDiagram',
            'functions/troubleshoot/getFaultObjectsTableColumnBasedComparator',
        ],
        function (
                Backbone,
                _,
                html,
                faultObjectsTablePopoutHtml,
                IndeterminateProgressDialog,
                selectedPackage,
                guidedDiagnosticsConfig,
                faults,
                faultSignals,
                generateDiagram,
                getFaultObjectsTableColumnBasedComparator
        ) {

            return Backbone.View.extend({

                sort: {
                    columnIndex: 4,
                    ascending: false,
                },

                events: {
                    "click #fault-objects-diagram-button": "onDiagramButtonClick",
                    "click .sortable-column": 'onSortableColumnClick',
                    "click #fault-objects-table-header-container": "sectionToggleHandler",
                },

                initialize(options)
                {
                    const {faultObjectsModel, faultCodesModel, poppedOutFaultObjectTable} = options;
                    faultSignals.fetch();
                    this.faultObjectsModel = faultObjectsModel;
                    this.faultCodesModel = faultCodesModel;
                    this.poppedOutFaultObjectTable = poppedOutFaultObjectTable;
                    this.reportHandler = new ReportEventHandler();
                    this.isSectionCollapsed = true;

                    this.faultObjectsModel.on('change:commonObjects', this.render.bind(this));
                    this.faultCodesModel.on('didClearCodes', this.sectionHide.bind(this));
                    this.faultObjectsModel.on('didWhatsInCommonClicked', this.sectionShow.bind(this));
                    selectedPackage.on("change:language change:config change:vin", this.render.bind(this));
                },

                render()
                {
                    const commonObjects = this.getCommonObjects();
                    this.updateSectionCollapsed();

                    const template = this.poppedOutFaultObjectTable ? faultObjectsTablePopoutHtml : html;
                    const localize = mentor.publisher.languageTranslator.localize.bind(
                            mentor.publisher.languageTranslator);
                    const translatePlainText = Utils.translatePlainText.bind(Utils);
                    const getSortIndicator = this.getSortIndicator.bind(this);
                    const contentInfoKey = this.getContentInfoKey(commonObjects);

                    this.$el.html(_.template(template)({
                        commonObjects,
                        localize,
                        translatePlainText,
                        getSortIndicator,
                        contentInfoKey,
                        isSectionCollapsed: this.isSectionCollapsed,
                    }));

                    this.updateDiagramButtonState(commonObjects);
                    this.initializeEventForReportHover();
                },

                sectionToggleHandler(evt)
                {
                    this.isExpanded() ? this.sectionHide() : this.sectionShow();
                },

                isExpanded()
                {
                    return !this.isSectionCollapsed;
                },

                updateSectionCollapsed()
                {
                    this.isSectionCollapsed = this.isSectionCollapsed && this.getCommonObjects().length <= 0;
                },

                sectionHide()
                {
                    const collapsableSection = this.$('.collapsable-section');
                    const orientInner = this.$('.orient-inner');
                    collapsableSection.hide();
                    orientInner.removeClass('expanded').addClass('collapsed');
                    this.isSectionCollapsed = true;
                },

                sectionShow()
                {
                    const collapsableSection = this.$('.collapsable-section');
                    const orientInner = this.$('.orient-inner');
                    collapsableSection.show();
                    orientInner.removeClass('collapsed').addClass('expanded');
                    this.isSectionCollapsed = false;
                },

                getSortIndicator(columnIndex)
                {
                    if (columnIndex !== this.sort.columnIndex) {
                        return "";
                    }
                    return this.sort.ascending ? "&#9650;" : "&#9660;";
                },

                getContentInfoKey(commonObjects)
                {
                    if (this.faultObjectsModel.keys().length === 0) {
                        return 'TroubleshootingPanel.ObjectsTable.SelectionChanged';
                    }
                    if (commonObjects.length === 0) {
                        return 'TroubleshootingPanel.ObjectsTable.NoCommonObjects';
                    }
                    return null;
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

                initializeEventForReportHover()
                {
                    this.reportHandler.initialiseEvents("scrollableFaultObjectTable");
                },

                updateDiagramButtonState(commonObjects)
                {
                    const isDisabled = commonObjects.length === 0;
                    this.toggleButtonState('#fault-objects-diagram-button', isDisabled);

                    const toolTipKey = isDisabled ?
                            'TroubleshootingPanel.ObjectsTable.DisabledPopout' :
                            'TroubleshootingPanel.ObjectsTable.Popout';
                    const toolTipText = mentor.publisher.languageTranslator.localize(toolTipKey);
                    this.toggleButtonState('#fault-objects-table-popout', isDisabled, toolTipText);
                },

                toggleButtonState: function (selector, isDisabled, toolTipText) {
                    const $button = this.$(selector);
                    $button.prop('disabled', isDisabled);

                    if (toolTipText) {
                        $button.attr('title', toolTipText);
                    }
                },

                getCommonObjects()
                {
                    const commonObjects = this.faultObjectsModel.get('commonObjects');

                    if (!commonObjects) {
                        return [];
                    }

                    const filteredObjects = mentor.publisher.filter.applyFilter(commonObjects);

                    const translatedObjects = filteredObjects.map((object) => ({
                        id: object.id,
                        type: this.translateObjectTypeKey(object.type),
                        column2Value: Utils.translatePlainText(object.column2Value),
                        column3Value: Utils.translatePlainText(object.column3Value),
                        codes: object.codes,
                    }));

                    const sortedObjects = translatedObjects.sort(getFaultObjectsTableColumnBasedComparator(
                            this.sort.columnIndex,
                            this.sort.ascending,
                    ));

                    return sortedObjects;
                },

                translateObjectTypeKey(objectType)
                {
                    const localizedKey = "TroubleshootingPanel.FaultObjectTable.ObjectType." +
                            objectType.trim().replace(/\s/g, "");
                    return mentor.publisher.languageTranslator.localize(localizedKey);
                },

                onDiagramButtonClick(event)
                {
                    mentor.publisher.stopEventFlow(event);

                    const dialog = createProgressDialog();
                    let cancelledDialog = false;
                    const renderObjectIds = this.getRenderObjectIds();
                    let codes = this.faultCodesModel.getCodes(false,true);
                    const generateDiagramPromise = generateDiagram(codes, faultSignals);

                    generateDiagramPromise
                            .then(function (data, textStatus, jqXHR) {
                                displayRenderedSVG(jqXHR.responseText, undefined, undefined,
                                        'commonFaultCode.BuildFromWhatsInCommon');
                                mentor.publisher.detailLayoutManager.resetContentPanel();
                                dialog.close();
                            })
                            .catch(function (errorThrown) {
                                        if (!cancelledDialog) {
                                            dialog.onError({
                                                title: mentor.publisher.languageTranslator.localize(
                                                        'TroubleshootingPanel.GenerateDiagram.Progress.ErrorTitle'),
                                                message: mentor.publisher.languageTranslator.localize(
                                                        'TroubleshootingPanel.GenerateDiagram.Progress.ErrorMessage'),
                                                cancel: mentor.publisher.languageTranslator.localize(
                                                        'TroubleshootingPanel.GenerateDiagram.Progress.Close'),
                                                guidance: mentor.publisher.languageTranslator.localize(
                                                        'TroubleshootingPanel.GenerateDiagram.Progress.ErrorGuidance'),
                                                implication: mentor.publisher.languageTranslator.localize(
                                                        'TroubleshootingPanel.GenerateDiagram.Progress.ErrorImplication'),
                                                onCancelFn: function () {
                                                    dialog.close();
                                                }
                                            });

                                        }
                                        console.log(errorThrown);
                                    }
                            )

                    function createProgressDialog()
                    {
                        const dialog = new IndeterminateProgressDialog({
                            title: mentor.publisher.languageTranslator.localize(
                                    'TroubleshootingPanel.GenerateDiagram.Progress.Title'),
                            message: mentor.publisher.languageTranslator.localize(
                                    'TroubleshootingPanel.GenerateDiagram.Progress.Message'),
                            cancel: mentor.publisher.languageTranslator.localize('Cancel'),
                            guidance: mentor.publisher.languageTranslator.localize(
                                    'TroubleshootingPanel.GenerateDiagram.Progress.ErrorGuidance'),
                            implication: mentor.publisher.languageTranslator.localize(
                                    'TroubleshootingPanel.GenerateDiagram.Progress.ErrorImplication'),
                            onCancelFn: function () {
                                cancelledDialog = true;
                                generateDiagramPromise.abort(); // Abort the promise if the dialog is cancelled
                                dialog.close();
                            },
                        });
                        dialog.show();
                        return dialog;
                    }
                },

                getRenderObjectIds()
                {
                    const renderObjects = this.faultObjectsModel.get('renderObjects') || [];
                    return mentor.publisher.filter.applyFilter(renderObjects)
                            .map(object => object.id);
                }

            });
        }
);
