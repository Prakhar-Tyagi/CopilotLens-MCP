/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, mentor*/
define([
    'jquery',
    'underscore',
    'backbone',
    "currentPackage",
    "models/selectedSystem"
], function ($, underscore, Backbone, currentPackage, selectedSystem) {
    "use strict";
    var VinLoginPopoverView = Backbone.View.extend({
        initialize : function () {
        },

        events : {

        },


        render : function (show) {
            if (!show) {
                return;
            }
            //this.remove();
            this.setElement(this.container);
            $('#loginPopover', this.$el).remove();
            this.$el.append(underscore.template(this.templateHTML)({
                show : true
            }));
            return this;
        },

        changeVin : function (evt) {
            var vin = evt.detail.vinOptions || "";
            currentPackage.set("vin", vin);
           // selectedSystem.set("optionExpression", vin);
        }

    }), vinLoginPopover = new VinLoginPopoverView(), renderVinLogin = function (evt) {
        vinLoginPopover.render(true);
    }, changeVin = function (evt) {
        vinLoginPopover.changeVin(evt);
    };
    mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.SHOW_VIN_LOGIN_POPUP, renderVinLogin);
    mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.VIN_FILTER_APPLIED, changeVin);
    return vinLoginPopover;
});