/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global mentor, $, ConfigurationFilterPopup, Msg, Constants, configurationPopup, VINSearchHandler, Utils, resetVINFilter, resetConfigurationFilter*/
mentor.publisher.optionFilterPanel = {

    changeVINFilterText: function (event) {
        var previousVal = $("#vinFilterText").val(), vinOptions = event.detail.vinOptions ||
                "", fromConfigurationBuilderFlow = event.detail.fromConfigurationBuilderFlow,
                vinNumber = event.detail.vinNumber;
        if (!fromConfigurationBuilderFlow) {

            if (vinOptions.trim() === "") {
                require(["models/Effectivity"], function (Effectivity) {
                    if (Effectivity.isEffectivityProj) {
                        $("#vinFilterText").val(mentor.publisher.languageTranslator.localize('SelectConfigText'));
                    }
                    else {
                        $("#vinFilterText").prop("disabled", false);
                        $("#vinFilterText").prop("readonly", false);
                        $("#vinFilterText").val(mentor.publisher.languageTranslator.localize('EnterVintext'));
                    }
                });

                $("#vinFilterText").addClass("placeHolderText");
                $("#vinTextHolder").attr("title", "")
            }
            else {
                //if the vin text field is clear (we clear the field when previous login failed)
                if (!previousVal) {
                    previousVal = vinNumber;
                }
                var vinAndOptions = previousVal + ":" + event.detail.vinOptions;
                //this function is not idempotent, it depends on the state of the text field.
                //it appends to the value in the text field.
                // So 2 consequetive calls with same vins can result in showing duplicate options in the text field
                //This workaround is to take care of this, if the options are already shown in the text field, then do not append.
                var strs = previousVal.split(':');
                if (strs.length == 2) {
                    //strs[0] is supposed to be the VIN number
                    //strs[1] is supposed to be the VIN options
                    var vinOptions = strs[1];
                    if (vinOptions == event.detail.vinOptions) {
                        return;
                    }
                }
                $("#vinFilterText").val(vinAndOptions);
                $("#vinFilterText").prop("disabled", true);
                $("#vinFilterText").prop("readonly", true);
                $("#vinTextHolder").attr("title", vinAndOptions)
            }
        }
    }, setVINFilterBoxText: function () {
        var vinfilterTextField;
        var p = mentor.publisher;
        var VINFilterText = p.languageTranslator.localize('EnterVintext');
        var selectConfigText = p.languageTranslator.localize('SelectConfigText');


        vinfilterTextField = $("#vinFilterText").val(VINFilterText);
        require(['models/Effectivity'], function (Effectivity) {
            var currentText = $("#vinFilterText").val();
            if (!currentText || currentText === VINFilterText || currentText === selectConfigText) {
                if (Effectivity.isEffectivityProj) {
                    vinfilterTextField = $("#vinFilterText").val(selectConfigText);
                }
            }
        });

        return vinfilterTextField;
    }, VINFilterView: function (config) {
        "use strict";
        var p = mentor.publisher;
        var vinfilterTextField, textFieldValueChanged, listeners, panel,
                textpanel, configurationBuilder, crossButton,
                configureAndResetButtonBox, vinFilterType, that = this;
        vinFilterType = p.dataLoader.loadOptionFilterInfo();
        if (!config) {
            config = {};
        }

        listeners = [];
        panel = $('#vinSearchToolbar');
        textpanel = $('<div id="vinTextHolder" class=vinTextHolder></div>');
        if (!vinFilterType.vin && !vinFilterType.config) {
            $("#resetBox").hide();
        }
        else {
            $("#resetBox").show();
        }
        vinfilterTextField = this.setVINFilterBoxText();

        p.eventDispatcher.attachEventListener(p.events.VIN_FILTER_APPLIED, function (event, vinfilterTextField) {
            that.changeVINFilterText(event);
        });

        configureAndResetButtonBox = $('<div id="resetBox"></div>');

        crossButton = $("#resetVin");

        configurationBuilder = $("#configButton");

        function hidePlaceHolderText(elementId, placeHolder)
        {
            var textValueField = $("#" + elementId);
            if (textValueField.val() === placeHolder) {
                $("#" + elementId).removeClass("placeHolderText");
                textValueField.val("");
            }
        }

        function showPlaceHolderText(elementId, placeHolder)
        {
            var textValueField = $("#" + elementId);
            if (textValueField.val() === "") {
                textValueField.val(placeHolder);
                $("#" + elementId).addClass("placeHolderText");
            }
        }

        textFieldValueChanged = function (thisField, onError) {
            var itemDetail = {};
            itemDetail.name = '';
            itemDetail.contentType = mentor.publisher.constants.VINText;
            itemDetail.vinNumber = thisField;
            itemDetail.onError = onError;
            new VINSearchHandler(itemDetail);
        };

        this.setVINValue = function (vinValue)
        {
            $(vinfilterTextField).removeClass("placeHolderText");
            $("#vinTextHolder").attr("title", "");
            $(vinfilterTextField).val(vinValue);
            var onError = function ()
            {
                $(vinfilterTextField).val('');
            };
            textFieldValueChanged($(vinfilterTextField).val(), onError);
        };

        if (!vinFilterType.vin) {
            vinfilterTextField.off("keypress");
        }

        this.onKeyPressOfTextField = function (evt) {
            if (vinFilterType.vin) {
                $('body').off('keypress');
                if (evt.keyCode === 13) {
                    //Enter key press
                    textFieldValueChanged($(vinfilterTextField).val());
                }
            }
        };

        this.hidePlaceHolderText = function (e) {
            hidePlaceHolderText("vinFilterText", mentor.publisher.languageTranslator.localize('EnterVintext'));
        };

        this.showPlaceHolderText = function (e) {
            showPlaceHolderText("vinFilterText", mentor.publisher.languageTranslator.localize('EnterVintext'));
        };

        this.addListeners = function (list) {
            listeners.push(list);
        };

        this.getFilter = function () {
            return panel;
        };

        this.setTextExternally = function (textValue) {
            vinfilterTextField.val(textValue);

            vinfilterTextField.attr("data-name", textValue);
            if (Utils.notNull(textValue) && textValue !== '') {
                $("#vinFilterText").removeClass("placeHolderText");
                //vinfilterTextField.attr("disabled", true);
                vinfilterTextField.prop("readonly", true);
                mentor.publisher.toolTip.registerToolTipBasedOnLength($(vinfilterTextField));
            }
            else {
                //vinfilterTextField.removeAttr("disabled");
                vinfilterTextField.prop("readonly", false);
            }
            textpanel[0].setAttribute("data-name", textValue);
            //crossButton[0].setAttribute("data-name", textValue);
        };

        this.getValue = function () {
            $(vinfilterTextField).val();
        };

        this.showInterativeBuilderButton = function (show) {
            if (show) {
                configurationBuilder.removeClass('display_none');
            }
            else {
                configurationBuilder.addClass('display_none');
            }
        };

        this.crossButtonClicked = function (evt, isEffectivityProj) {
            if (isEffectivityProj) {
                showPlaceHolderText("vinFilterText", mentor.publisher.languageTranslator.localize('SelectConfigText'));
            }
            else {
                showPlaceHolderText("vinFilterText", mentor.publisher.languageTranslator.localize('EnterVintext'));
            }

            resetVINFilter();
            resetConfigurationFilter();
            //todo handle it for configuration filter
            $("#vinFilterText").blur();
        };

    }
};

var resizeVinFilterBox = function () {
    "use strict";
    var isConfigEnabled = true, configCss, configButtonWidth, radiusWidth, containerWidth, vinBoxWidth,
            crossButtonWidth;
    configCss = $("#configButton").attr("style");
    if ($("#configButton").length === 0 ||
            (typeof (configCss) !== "undefined" && configCss !== null && configCss.indexOf("display: none;") >= 0)) {
        isConfigEnabled = false;
    }

    configButtonWidth = 0;
    radiusWidth = 8;
    if (isConfigEnabled) {
        configButtonWidth = $("#configButton").width();
        radiusWidth = 10;
    }
    containerWidth = $("#resetBox").parent().width();
    vinBoxWidth = containerWidth - configButtonWidth;
    crossButtonWidth = $("#resetVin").width();
    $('#resetBox').width(vinBoxWidth - radiusWidth);
    $('#vinTextHolder').width(vinBoxWidth - crossButtonWidth - radiusWidth);
    $("#vinFilterText").width("100%");
};



