/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

define(
        ['underscore', 'backbone'],
        function (_, Backbone) {

            var FaultCodesModel = Backbone.Model.extend({

                isActive: function (code) {
                    return this.get(code) === 'active';
                },

                isPassive: function (code) {
                    return this.get(code) === 'passive';
                },

                add: function (code) {
                    if (this.has(code)) {
                        return;
                    }
                    this.set(code, 'active', {silent: true});
                    this.trigger('didAddCode', this, code);
                },

                remove: function (code) {
                    if (!this.has(code)) {
                        return;
                    }
                    this.unset(code, {silent: true});
                    this.trigger('didRemoveCode', this, code);
                },

                update: function (codes, active) {
                    codes.forEach(function (code) {
                        this.set(code, active ? 'active' : 'passive', {silent: true});
                    }.bind(this));
                    this.trigger('didUpdateCodes', this, codes);
                },

                clear: function () {
                    Backbone.Model.prototype.clear.apply(this, {silent: true});
                    this.trigger("didClearCodes", this);
                },

                isEmpty: function () {
                    return this.keys().length == 0;
                },

                getAllCodes: function () {
                    return this.getCodes();
                },

                getActiveCodes: function () {
                    return this.getCodes(false, true);
                },

                getPassiveCodes: function () {
                    return this.getCodes(true);
                },

                getCodes: function (excludeActive, excludePassive) {
                    return this.keys().filter(function (code) {
                        return (this.isActive(code) && !excludeActive) ||
                                (this.isPassive(code) && !excludePassive);
                    }.bind(this));
                },

            }, {
                fromCodes: function (activeCodes, passiveCodes) {
                    var content = new FaultCodesModel();
                    activeCodes.forEach(function (code) {
                        content.set(code, 'active');
                    });
                    passiveCodes.forEach(function (code) {
                        content.set(code, 'passive');
                    });
                    return content;
                }
            });

            return FaultCodesModel;
        }
)