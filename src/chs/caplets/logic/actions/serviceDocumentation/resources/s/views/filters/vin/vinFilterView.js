/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, mentor, Msg, setTimeout*/
define([
    'jquery',
    'underscore',
    'backbone',
    "ConfigurationsModel",
    "currentPackage",
    "models/Effectivity",
    "UserSession",
], function ($, underscore, Backbone, configurationsModel, selectedPackage, Effectivity, UserSession) {
    "use strict";
    var VinFilterView;
    VinFilterView = Backbone.View.extend({

        initialize: function () {
            selectedPackage.on("change:language", this.translate, this);
        },

        translate: function (config) {
            config = config || {}
            var options = config.options || mentor.publisher.filter && mentor.publisher.filter.vinOptions;
            var vinfilterElement = $("#vinFilterText");
            if ($(vinfilterElement).hasClass("placeHolderText") && !options) {
                $(vinfilterElement).val(mentor.publisher.languageTranslator.localize('EnterVintext'));
            }
        },

        events: {
            "keypress #vinFilterText": "onKeyPress",
            "focus #vinFilterText": "onFocus",
            "blur #vinFilterText": "onBlur",
            "click #vinFilterText": "onClick",
            "focusout #vinFilterText": "onFocusOut",
            "click #resetVin": "onClickOfCrossButton",
            "click #configButton": "onClickOfConfigButton"
        },

        onClickOfConfigButton: function (evt) {
            var filterVal = $("#vinFilterText").val();
            $("#vinFilterText").val(mentor.publisher.languageTranslator.localize('NotReadyPleaseWait'));
            setTimeout(function () {
                selectedPackage.trigger("loadAllObjects");
                $("#vinFilterText").val(filterVal);
            }, 100);
            configurationsModel.fetch(evt);
            evt.stopPropagation();
        },

        onKeyPress: function (evt) {

            mentor.publisher.optionFilterPanel.onKeyPressOfTextField(evt);
            evt.stopPropagation();
            //evt.preventDefault();
        },

        onFocus: function (evt) {
            mentor.publisher.optionFilterPanel.hidePlaceHolderText(evt);
        },

        onBlur: function (evt) {
            mentor.publisher.optionFilterPanel.showPlaceHolderText(evt);
        },

        onClick: function (evt) {
            if ($("#vinFilterText").hasClass("placeHolderText")) {
                // alert(mentor.publisher.languageTranslator.localize('NotReadyPleaseWait'));

                $("#vinFilterText").val(mentor.publisher.languageTranslator.localize('NotReadyPleaseWait'));
                setTimeout(function () {
                    selectedPackage.trigger("loadAllObjects");
                    $("#vinFilterText").val("");
                }, 100);
            }
            /*
             if ($("#vinFilterText").hasClass("placeHolderText")) {
             $("#vinFilterText").val("");
             }
             */
        },

        onFocusOut: function (evt) {
            if ($("#vinFilterText").val().trim() === "") {
                $("#vinFilterText").addClass("placeHolderText");
                // alert(8);
                $("#vinFilterText").val(mentor.publisher.languageTranslator.localize('EnterVintext'));
            }
        },

        onClickOfCrossButton: function (evt) {
            mentor.publisher.optionFilterPanel.crossButtonClicked(evt, Effectivity.isEffectivityProj);
        },

        render: function () {
            this.remove();

            this.setElement(this.container);
            var selectedPackage = UserSession.getActiveSession().get(UserSession.kSelectedPackageProperty);
            if (selectedPackage) {
                var range = selectedPackage.get('effectivityRange');
                if (range && selectedPackage.get('start') === selectedPackage.get('end')) {
                    range = range.split('-')[0];
                }
            }
            //todo licence should be correctly checked for
            this.$el.append(underscore.template(this.templateHTML)({
                vinLicence: true,
                range: range,
                configLicence: mentor.publisher.configurationsManager.hideOrShowConfigBuilderButton(),
                isEffectivityPacket: Effectivity.isEffectivityProj
            }));
            setTimeout(function () {
                mentor.publisher.optionFilterPanel.VINFilterView();
                resizeVinFilterBox();
            }, 1);
            return this;
        }

    });

    return new VinFilterView();
});