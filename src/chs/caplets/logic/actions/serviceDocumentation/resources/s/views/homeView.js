/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(
    [
        'jquery',
        'underscore',
        'backbone'
    ],
    function ($, _, Backbone) {
        "use strict";

        var HomeView = Backbone.View.extend({
            closePopover : function(){
                $("#home-screen .popover").hide();
            },

            render : function () {
                this.setElement(this.container);
                this.$el.html(_.template(this.templateHTML)());

                mentor.publisher.eventDispatcher.removeEventListener(mentor.publisher.events.CLOSE_POPOVER, this.closePopover);
                mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.CLOSE_POPOVER, this.closePopover);
                return this;
            }
        });

        return new HomeView();
    }
);