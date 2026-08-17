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
            "router",
            "preferences",
            "UserSession",
            "PackagesViewModel",
            "currentPackage"
        ],
        function ($, _,
                Backbone,
                Router,
                preferences,
                UserSession,
                PackagesViewModel,
                currentPackage) {
            "use strict";

            var deselectPackageCells,
                    selectedClassName;

            deselectPackageCells = function (view) {
                view.$('.package-cell').removeClass(selectedClassName);
            };

            selectedClassName = "highlight";

            var PackagesView = Backbone.View.extend({

                events: {
                    "click #packages-grid-container": "discardSelection",
                    "click .package-cell": "handleCellClick"
                },

                initialize: function () {
                    this.model = new PackagesViewModel();
                    this.model.on("packagesChanged", function () {
                        this.render();
                    }, this);
                    this.model.on("highlightExactMatch", function (e) {
                        this.highlightExactMatch(e);
                    }, this);
                    this.isEffectivityPackage = this.model.packages.containSubPackages();
                    preferences.on("change:language", this.render, this);
                },

                render: function () {
                    this.model.localizedSort();
                    var subPackages = this.model.packages.containSubPackages() && this.model.subPackages;
                    this.setElement(this.container);

                    var template = _.template(this.templateHTML);

                    if (subPackages && subPackages.length > 0) {
                        this.$el.html(template(subPackages.toJSON()));
                    }
                    else {
                        this.$el.html(template(this.model.toJSON()));
                    }

                    var selectedPackage = UserSession.getActiveSession().get(UserSession.kSelectedPackageProperty);
                    if (selectedPackage) {
                        this.$('.package-cell').each(function () {
                            var range = $(this).data('range') || undefined;
                            if ($(this).attr('data-id') === selectedPackage.get('id') &&
                                    range === selectedPackage.get('effectivityRange')) {
                                $(this).addClass(selectedClassName);
                            }
                        });
                    }

                    return this;
                },

                highlightExactMatch: function (detail) {
                    var allPacketElements = this.$('.package-cell')
                    this.discardSelection();

                    if (detail.searchText) {
                        _.find(allPacketElements, function (element) {
                            var effectivityMatch,
                                    packageNameMatch;

                            var prefix = $(element).data('prefix');
                            var start = $(element).data('start');
                            var end = $(element).data('end');

                            var name = Utils.translate($(element).data('name'));

                            // Check if exact range match is present
                            var characterRegEx = new RegExp('^' + prefix, 'gi');
                            var CharaterMatch = characterRegEx.exec(detail.searchText);
                            if (CharaterMatch) {
                                var numericPart = detail.searchText.slice(CharaterMatch.index + prefix.length);
                                effectivityMatch = !numericPart.match(/[^\d]/gi) &&
                                        parseInt(numericPart, 10) >= parseInt(start, 10) &&
                                        parseInt(numericPart, 10) <= parseInt(end, 10);
                            }

                            //Check if packageName Matches
                            packageNameMatch = name.toLowerCase() === detail.searchText.toLowerCase();

                            // if either match exactly, highlight it
                            if (effectivityMatch || packageNameMatch) {
                                $(element).trigger("click");
                                return true;
                            }
                        }, this);
                    }
                },

                discardSelection: function (event) {
                    deselectPackageCells(this);
                    UserSession.getActiveSession().unset(UserSession.kSelectedPackageProperty);
                },

                shouldResetIdWhenEffRangeIsNotSameForSamePackageId: function (existingRange, range, selectedProject) {
                    if (existingRange !== range) {
                        selectedProject.set("id", "", {silent: true});
                    }
                }, handleCellClick: function (event) {
                    var selected = $(event.currentTarget).hasClass(selectedClassName),
                            packageId = $(event.currentTarget).attr('data-id'),
                            range = $(event.currentTarget).data('range'),
                            projectId = $(event.currentTarget).data('projectid');

                    var existingPackage = UserSession.getActiveSession().get(UserSession.kSelectedPackageProperty);
                    if (existingPackage && existingPackage.attributes) {
                        var existingRange = existingPackage.get("effectivityRange");
                        if (existingRange || range) {
                            this.shouldResetIdWhenEffRangeIsNotSameForSamePackageId(existingRange, range, currentPackage);
                        }
                    }
                    if (selected && event.originalEvent !== undefined) {
                        mentor.publisher.router.loadProject({
                            projectId: packageId,
                            range: range,
                            projId: projectId
                        });
                    }
                    else {
                        deselectPackageCells(this);
                        $(event.currentTarget).addClass(selectedClassName);

                        var selectedPackage;
                        if (this.isEffectivityPackage) {
                            selectedPackage = this.model.subPackages.findWhere({
                                effectivityRange: range,
                                id: packageId
                            });
                        }
                        else {
                            selectedPackage = this.model.packages.get(packageId);
                        }
                        UserSession.getActiveSession().set(UserSession.kSelectedPackageProperty, selectedPackage);
                    }

                    event.stopPropagation();
                }
            });

            return new PackagesView();
        }
);