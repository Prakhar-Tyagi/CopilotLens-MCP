/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

define(['jquery', 'underscore', 'backbone', 'text!templates/component/indeterminateProgressDialog.html'],
        function ($, _, Backbone, templateHTML) {
            "use strict";

            return function (options) {
                var ModelDialog = Backbone.View.extend({

                    el: '#modal-container',

                    events: {
                        "click .cancel,.close": "onCancel",
                        "click .modal-overlay": "doNothing"
                    },

                    initialize: function () {
                        this.options = options;
                        mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.CLOSE_MODAL,
                                this.onCancel.bind(this));
                    },

                    onCancel: function (event) {
                        mentor.publisher.stopEventFlow(event);

                        this.options.onCancelFn && this.options.onCancelFn.call(this);
                        this.destroy();
                    },

                    onError: function (errorOptions) {
                        this.options = errorOptions;
                        this.render();
                        this.$(".indeterminate-progress-bar").hide();
                        this.$(".cancel").addClass("aw-button");
                        this.$(".indeterminate-activity-message").addClass("font-weight_bold");
                        this.$(".display-none").removeClass("display-none");
                        this.$(".error-icon").show();
                    },
                    doNothing: function (event) {
                        mentor.publisher.stopEventFlow(event);
                    },

                    render: function () {
                        var localizedOptions = {
                            title: mentor.publisher.languageTranslator.localize(this.options.title),
                            message: mentor.publisher.languageTranslator.localize(this.options.message),
                            cancel: mentor.publisher.languageTranslator.localize(this.options.cancel),
                            guidance: mentor.publisher.languageTranslator.localize(this.options.guidance),
                            implication: mentor.publisher.languageTranslator.localize(this.options.implication)
                        };

                        this.$el.html(_.template(templateHTML)(localizedOptions));
                        return this;
                    },

                    destroy: function () {
                        this.undelegateEvents();
                        this.$el.children().each(function () {
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
                    onError: modelDialog.onError.bind(modelDialog),
                    show: modelDialog.show.bind(modelDialog),
                    close: modelDialog.close.bind(modelDialog)
                };
            };
        });
