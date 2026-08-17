/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

define(["jquery", "underscore", "backbone", "text!templates/modalTemplate.html"], function ($, _, Backbone, modelTemplate) {
    "use strict";

    mentor.publisher.modalDialogFlag = {
        ERROR: "error",
        QUESTION: "question"
    };

    return function (options) {
        var ModelDialog = Backbone.View.extend({
            events: {
                "click .confirm": "onConfirm",
                "click .cancel": "onCancel",
                "click .modal-overlay": "doNothing"
            },

            initialize: function() {
                mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.CLOSE_MODAL, function () {
                    this.onCancel();
                }.bind(this));
            },

            onConfirm: function (evt) {
                options.onConfirmFn.call(this);
                this.destroy();
                evt && evt.stopPropagation();
            },

            onCancel: function (evt) {
                options.onCancelFn.call(this);
                this.destroy();
                evt && evt.stopPropagation();
            },

            doNothing: function (evt) {
                evt && evt.stopPropagation();
            },

            render: function () {
                this.setElement("#modal-container");
                /**
                 * Pass the buttons required in ModalDialog as primaryButton and secondaryButton, If the button isn't required then pass false
                 */
                var modelHtml = _.template(modelTemplate)({
                    title: options.title,
                    message: options.message,
                    implication: options.implication,
                    guidance: options.guidance,
                    primaryButton: options.primaryButton,
                    secondaryButton: options.secondaryButton,
                    dialogFlag: options.dialogFlag
                });
                this.$el.append(modelHtml);
                return this;
            },

            destroy: function () {
                this.undelegateEvents();
                $(this.$el).children().each(function () {
                    $(this).remove();
                });
            },

            show: function () {
                this.render();
            },

            close: function () {
                this.destroy();
            }
        });

        var modelDialog = new ModelDialog();
        return {
            show: modelDialog.show.bind(modelDialog),
            close: modelDialog.close.bind(modelDialog)
        };
    };
});
