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
            'underscore',
            'backbone',
            'text!templates/troubleshoot/troubleshootPanel.html',
            'models/troubleshoot/FaultCodesModel',
            'views/contentpanel/toolbar/contentToolBar',
            'views/troubleshoot/faultCodeSelectionView',
            'views/troubleshoot/FaultsTable',
            'views/troubleshoot/FaultObjectsTable',
            'currentPackage',
        ],
        function (_, Backbone, html, FaultCodesModel, Toolbar, FaultSelectionView, FaultsTable, FaultObjectsTable,
                currentPackage) {
            "use strict";

            var View = Backbone.View.extend({

                el: '#splitter3',

                events: {
                    "click .closeBtn": "onCloseButtonClick",
                    "click #fault-objects-table-popout": "onFaultObjectsTablePopoutButtonClick",
                    "click #troubleshootingPanel": "resetHighlightingInFaultObjectTable",
                },

                onCloseButtonClick: function () {
                    this.undelegateEvents();
                },

               /* onPopoutButtonClick: function (event) {
                    mentor.publisher.stopEventFlow(event);

                    var popoutHandler = mentor.publisher.popoutHandler;
                    var popoutURL = popoutHandler.createURL({
                        projectId: currentPackage.get('id'),
                        type: mentor.publisher.contentType.TROUBLESHOOT,
                        activeCodes: this.faultCodesModel.getActiveCodes().join(','),
                        passiveCodes: this.faultCodesModel.getPassiveCodes().join(','),
                    })
                    popoutHandler.openPopout(popoutURL);
                },*/

                resetHighlightingInFaultObjectTable: function (event) {
                    var element = event.target;
                    if (!($('.clickable-column').find(element).length && element.tagName == "span")) {
                        $('#scrollableFaultObjectTable .highlighted').removeClass('highlighted');
                        $('#scrollableFaultObjectTable .hovered').removeClass('hovered');
                    }
                },

                onFaultObjectsTablePopoutButtonClick: function (event) {
                    mentor.publisher.stopEventFlow(event);

                    var popoutHandler = mentor.publisher.popoutHandler;
                    var popoutURL = popoutHandler.createURL({
                        projectId: currentPackage.get('id'),
                        type: mentor.publisher.contentType.FAULT_OBJECT_TABLE,
                        activeCodes: this.faultCodesModel.getActiveCodes().join(','),
                        passiveCodes: this.faultCodesModel.getPassiveCodes().join(','),
                    })
                    popoutHandler.openPopout(popoutURL);
                },

                appendToolbar(options)
                {
                    const toolbar = new Toolbar();
                    const toolbarView = toolbar.render({
                        type: options.type,
                        title: mentor.publisher.languageTranslator.localize('TroubleshootingPanel.Title'),
                        computeTitle()
                        {
                            return mentor.publisher.languageTranslator.localize('TroubleshootingPanel.Title');
                        },
                    }).$el;

                    this.$el.append(toolbarView);
                    if (this.$el.find('.popOutBtn').length) {
                        this.$el.find('.popOutBtn').hide();
                    }
                },

                appendContent(options)
                {
                    if (this.faultsSelectionView) {
                        this.faultsSelectionView.remove();
                    }
                    if (this.faultsTable) {
                        this.faultsTable.remove();
                    }
                    if (this.faultObjectsTable) {
                        this.faultObjectsTable.remove();
                    }
                    this.$el.append(_.template(html)({poppedOutFaultObjectTable: options.poppedOutFaultObjectTable}));

                    this.faultCodesModel = FaultCodesModel.fromCodes(options.activeCodes, options.passiveCodes);
                    const faultObjectsModel = new Backbone.Model();

                    this.faultCodesModel.on('all', faultObjectsModel.clear, faultObjectsModel);
                    this.faultCodesModel.on('all', this.layoutContentPanel, this);

                    this.faultsSelectionView = new FaultSelectionView({
                        el: this.$('.faults-selection'),
                        faultCodesModel: this.faultCodesModel,
                    });

                    if (!options.poppedOutFaultObjectTable) {
                        this.faultsSelectionView.render();
                    }

                    this.faultsTable = new FaultsTable({
                        el: this.$('.faults-table'),
                        faultCodesModel: this.faultCodesModel,
                        faultObjectsModel,
                    });

                    if (!options.poppedOutFaultObjectTable) {
                        this.faultsTable.render();
                    }

                    this.faultObjectsTable = new FaultObjectsTable({
                        el: this.$('.fault-objects-table'),
                        faultObjectsModel,
                        faultCodesModel: this.faultCodesModel,
                        poppedOutFaultObjectTable: options.poppedOutFaultObjectTable,
                    });

                    this.faultObjectsTable.render();
                    setTimeout(() => this.layoutContentPanel(), 100);
                },

                layoutContentPanel: function (saveInHistory) {
                    var isSameContentOpen = false;
                    if (saveInHistory) {
                        isSameContentOpen = true;
                    }
                    mentor.publisher.contentArea.layoutContentPanel({
                        title: mentor.publisher.languageTranslator.localize('TroubleshootingPanel.Title'),
                        systemId: currentPackage.get('systemId'),
                        type: mentor.publisher.contentType.TROUBLESHOOT,
                        activeCodes: this.faultCodesModel.getActiveCodes(),
                        passiveCodes: this.faultCodesModel.getPassiveCodes(),
                    }, isSameContentOpen);
                    if (saveInHistory) {
                        require(["routers/multipleDocumentRouter"], function (multipleDocumentRouter) {
                            multipleDocumentRouter.save(true, currentPackage.get('objectId'));
                        });
                    }
                },

                render: function (options) {
                    this.container = $('#splitter3');
                    this.setElement(this.container);
                    mentor.publisher.contentArea.closeExistingPanel(options, this);
                    this.appendToolbar(options);
                    this.appendContent(options);
                    this.delegateEvents();
                    return this;
                }
            });

            return new View();
        }
);

