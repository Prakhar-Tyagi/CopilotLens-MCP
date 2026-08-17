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
        'backbone',
        'UserSession',
        'Packages'
    ],
    function ($, _, Backbone, UserSession, Packages) {

        return Backbone.Model.extend({
            initialize: function () {
                var activeSession = UserSession.getActiveSession();
                activeSession.on("change:" + UserSession.kSelectedProjectProperty, function () {
                    this.updatePackages();
                }, this);

                var eventDispatcher = mentor.publisher.eventDispatcher;
                eventDispatcher.attachEventListener('publisher:filterPackages', this.filterPackages.bind(this));
                eventDispatcher.attachEventListener('publisher:resetPackages', this.resetPackages.bind(this));
                this.updatePackages();
            },

            updatePackages: function (filterFn) {
                var activeSession = UserSession.getActiveSession();
                var selectedProjectId = activeSession.get(UserSession.kSelectedProjectProperty).get('id');

                this.packages = new Packages();
                this.packages.projectId = selectedProjectId;
                this.packages.fetch({
                    async: false
                });

                if (this.packages.containSubPackages()) {
                    this.subPackages = this.packages.models[0].subPackages
                }

                if (filterFn) {
                    if (this.packages.containSubPackages()) {
                        this.subPackages.models =  _.filter(this.subPackages.models, filterFn, this);
                    }
                    else {
                        this.packages.models = _.filter(this.packages.models, filterFn, this);
                    }
                }

                this.trigger('packagesChanged');
            },

            resetPackages: function () {
                this.updatePackages();
            },

            filterPackages: function (e) {
                var detail = e.detail;

                if (detail.searchText === '') {
                    UserSession.getActiveSession().set(UserSession.kSelectedPackageProperty, null);
                    this.updatePackages.call(this);
                    return;
                }

                this.updatePackages.call(this, this.filterFunction.bind(this, detail.searchText));
                this.trigger('highlightExactMatch', e.detail);
            },

            filterFunction : function (searchText, model) {
                var prefix = model.get('prefix');
                var start = model.get('start');
                var end = model.get('end');

                var rangeMatch = false;
                var characterRegEx = new RegExp('^' + prefix, 'gi');
                var CharaterMatch = characterRegEx.exec(searchText);
                if(CharaterMatch){
                    var numericPart = searchText.slice(CharaterMatch.index + prefix.length);

                    if (!numericPart.match(/[^\d]/gi) && parseInt(numericPart, 10) >= parseInt(start, 10) &&
                            parseInt(numericPart, 10) <= parseInt(end, 10)) {
                        rangeMatch = true;
                    }
                }

                var effectivityContainSearch = model.get('effectivityRange') &&
                        model.get('effectivityRange').search(new RegExp(searchText.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, '\\$&'), 'gi')) !== -1;

                var name = Utils.translate(model.get('name'));
                return rangeMatch || name.search(new RegExp(searchText.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, '\\$&'), 'gi')) !== -1
                        || effectivityContainSearch;
            },

            localizedSort: function () {
                this.packages.comparator = function (thisPackage) {
                    return Utils.translate(thisPackage.get('name'));
                };
                this.packages.sort();
            },

            toJSON: function () {
                var packageArray = this.packages.models;

                return {
                    packages: packageArray,
                    isEffectivityPackage: false
                }
            }
        });
    }
);