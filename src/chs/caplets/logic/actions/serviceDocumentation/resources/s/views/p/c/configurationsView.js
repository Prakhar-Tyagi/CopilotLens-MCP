/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define([
    "BaseConfigurationsBuilderView",
    "ConfigurationsModel",
    "currentPackage",
    "ConfigurationsCollection",
    "views/component/ModalDialog",
    "text!templates/p/c/configPopupBtnControls.html"
], function (
    BaseConfigurationsBuilderView,
    popoverModel,
    currentPackage,
    configurationsCollection,
    ModalDialog,
    configBtnControlsTemplate) {
    "use strict";

    function createConfigurationObject(configName, selectedOptions)
    {
        var publisherConstants = mentor.publisher.constants;
        var configObject = {
            type: "configuration",
            name: configName.trim(),
            value: selectedOptions.join(", "),
            isLocal: true
        };
        configObject[publisherConstants.customToolTipArrayLength] = 1;
        configObject[publisherConstants.customToolTip + '-0'] = configObject.name + "==" + configObject.value;
        return configObject;
    }
    var translator = mentor.publisher.languageTranslator;

    var ConfigurationsBuilderView = BaseConfigurationsBuilderView.extend({
        events: {
            "click .save-config": "createConfiguration",
            "click .filter-config": "applyOptions",
            "click .close-config": "cancelAndClose",
            "click .delete-config": "deleteConfig",
            "mouseover .delete-config": "showTooltipForDelete",
            "click .listPanel.options .listItem": "toggleOptionCheckBox",
            "click .listItem>.configPanelCheckBox": "updateConfigurationPanels",
            "click #toolbar_closebtn": "closePopover",
            "click #configPopup": "popoverClickHandle",
            "keyup #config-filter-name": "validateConfigName"
        },

        popoverClickHandle: function (e) {
            e.stopPropagation();
        },

        showTooltipForDelete: function (event) {
            event.detail = {};
            event.detail.toolTip = $(event.currentTarget).attr('data-value');
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.SHOW_TOOL_TIP, event);
        },

        createConfiguration: function (event) {
            var configName = this.getConfigTextFieldValue();
            var selected = [];
            $("#configPopup .listPanel").find("input[type='checkbox']:checked").each(function (idx, ele) {
                selected.push($(ele).attr('data-value'));
            });

            var configObject = createConfigurationObject(configName, selected);
            var isCreated = popoverModel.createNewModel(configObject);
            if (isCreated) {
                setTimeout(function () {
                    this.$el.find("#config-popover-groupedlist").animate({
                        scrollTop: 0
                    }, function () {
                        this.$el.find("#config-filter-name")
                            .prop('disabled', true)
                            .css('color', '#000')
                            .addClass('matched-config')
                            .attr('title', translator.localize('duplicate.option.selection.message'));
                        this.$el.find(".save-config").prop('disabled', true);
                    }.bind(this));
                }.bind(this), 100);
            }
            event && event.stopPropagation();
        },

        applyOptions: function (event) {
            filterByOptions();
            $("#configPopup").hide();
            event && event.stopPropagation();
        },

        cancelAndClose: function (event) {
            popoverModel.rollbackToLastAppliedOptions();
            this.lastEnteredConfigName = null;
            this.undelegateEvents();
            $("#configPopup").remove();
        },

        deleteConfig: function (event) {
            var configElement = $(event.currentTarget).closest(".listItem");
            var configNameToDelete = ("" + configElement.data('name')); // data() can return number.

            function onConfirm()
            {
                this.deleteConfigByName(configNameToDelete);
                configElement.remove();
            }

            var modalDialog = new ModalDialog({
                title: translator.localize("delete.config.title"),
                message: translator.localize("delete.config.message"),
                implication: translator.localize("delete.config.implication"),
                guidance: translator.localize("delete.config.guidance"),
                primaryButton: "Delete",
                secondaryButton: "Cancel",
                dialogFlag: mentor.publisher.modalDialogFlag.QUESTION,
                onConfirmFn: onConfirm.bind(this),
                onCancelFn: function () {}.bind(this)
            });
            modalDialog.show();
            event && event.stopPropagation();
        },

        deleteConfigByName: function (configurationName) {
            return popoverModel.delete(configurationName);
        },

        getModel: function () {
            return popoverModel;
        },

        updateConfigurationPanels: function (event) {
            this.toggleConfigSavePanel();
            popoverModel.updateModel(event, "option");
            var configTextFieldValue = this.getConfigTextFieldValue();
            var configNameText = configTextFieldValue || popoverModel.generateConfigName();
            this.validateTextAndSelection(configNameText);
            event.stopPropagation();
        },

        renderConfigSavePanel: function (autoConfigName) {
            var compiledContent = _.template(configBtnControlsTemplate)({
                configFilterName: autoConfigName || ""
            });
            $("#config-btn-controls", this.$el).html(compiledContent);
        },

        validateTextAndSelection: function (configNameText) {
            var configTextToValidate = configNameText || "";
            var existingConfigName = popoverModel.getExistingConfigForSelectedOptions();
            if (existingConfigName) {
                this.setConfigTextFieldValue(existingConfigName);
                this.setEnabledConfigSavePanel(false);
                this.$el.find("#config-filter-name").addClass('matched-config');
                this.$el.find("#config-filter-name").attr('title', translator.localize('duplicate.option.selection.message'));
            }
            else if (popoverModel.checkIfNameAlreadyExist(configTextToValidate.trim()) ||
                configTextToValidate.trim() === '') {
                this.setConfigTextFieldValue(configTextToValidate);
                this.$el.find("#config-filter-name")
                    .prop('disabled', false)
                    .css('color', "red")
                    .removeClass('matched-config')
                    .attr('title', translator.localize('duplicate.configuration.name.message'));
                this.$el.find(".save-config").prop('disabled', true);
                this.$el.find(".filter-config").prop('disabled', false);
            }
            else {
                this.setConfigTextFieldValue(configTextToValidate);
                this.setEnabledConfigSavePanel(true);
                this.$el.find("#config-filter-name")
                    .removeClass('matched-config')
                    .attr('title', "");
            }
        },

        setEnabledConfigSavePanel: function (enabled) {
            this.$el.find("#config-filter-name")
                .prop('disabled', !enabled)
                .css('color', '#000');
            this.$el.find(".save-config").prop('disabled', !enabled);
            this.$el.find(".filter-config").prop('disabled', !enabled);
        },

        shouldShowConfigSavePanel: function () {
            var clientType = mentor.publisher.clientType || ""
            return clientType.toLowerCase() !== 'smartflow' && popoverModel.shouldShowConfigSavePanel();
        },

        toggleConfigSavePanel: function () {
            var btnControlsPanel = this.$el.find("#config-btn-controls");
            var scrollableListPanel = this.$el.find("#config-popover-groupedlist");
            var checkedOptions = $("input[type='checkbox']:checked", this.$el.find(".listPanel.options"));
            if (checkedOptions.length > 0 && this.shouldShowConfigSavePanel()) {
                btnControlsPanel.show();
                scrollableListPanel.css({
                    height: '310px'
                });
            }
            else {
                btnControlsPanel.hide();
                scrollableListPanel.css({
                    height: '375px'
                });
            }
        },

        validateConfigName: function (event) {
            var enteredConfigName = $("#config-filter-name.configTextBox").val();
            this.lastEnteredConfigName = enteredConfigName;
            this.validateTextAndSelection(enteredConfigName);
        },

        getConfigTextFieldValue: function () {
            return this.$el.find("#config-filter-name").val();
        },

        setConfigTextFieldValue: function (configurationName) {
            this.$el.find("#config-filter-name").val(configurationName);
        },

        render: function () {
            if ($('#configPopup', this.$el).length > 0) {
                return;
            }

            BaseConfigurationsBuilderView.prototype.render.call(this);
            var configNameText = this.lastEnteredConfigName || popoverModel.generateConfigName();
            this.renderConfigSavePanel(configNameText);
            this.validateTextAndSelection(configNameText);

            setTimeout(function () {
                this.toggleConfigSavePanel();
            }.bind(this), 100);
            return this;
        }
    }), configurationsBuilderView;
    configurationsBuilderView = new ConfigurationsBuilderView();
    mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.CONFIGURATION_FILTER_APPLIED,
        function (evt) {
            var config = evt.detail.data ||
                "", selectedSystemId, selectedSystem, configurationOfSelectedSystem = config;
            //this will re-render the navigation panel
            currentPackage.set("config", config);
            //the content should be filtered based on the configuration of the system selected , and not based on the options selected in the dynamic mode
            selectedSystemId = mentor.publisher.selectedSystem.get("systemId");
            if (selectedSystemId) {
                selectedSystem = mentor.publisher.project.getObjectById(selectedSystemId);
                if (selectedSystem && selectedSystem.getActiveConfiguration) {
                    configurationOfSelectedSystem = selectedSystem.getActiveConfiguration();
                }
            }
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.ITEM_CLICKED_IN_DYNAMIC_MODE,
                {vinOptions: configurationOfSelectedSystem});
            //console.log("configuration filter is applied " + config);
            //this will re-render the detail panel
            mentor.publisher.selectedSystem.set("optionExpression", config);
        });

    return _.extend(configurationsBuilderView, Backbone.Events);
});
