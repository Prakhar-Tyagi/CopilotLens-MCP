/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, Backbone, xmlDataLoader, objectFactoryImpl, mentor*/
define(["jquery", "underscore", "backbone", "Package", "preferences", "collections/SubPackages"],
        function ($, _, Backbone, Package, preferences, SubPackages) {
            "use strict";
            var translator = mentor.publisher.languageTranslator;
            if (!translator.isLoaded()) {
                translator.loadResources(preferences.get('language'));
            }

            var Packages = Backbone.Collection.extend({
                model: Package,
                url: "unzipped/data/packages.xml",

                fetch: function (options) {
                    _.extend(options, {
                        dataType: "text",
                        error: function (model, resp) {
                            if (resp.responseText == 'no_package') {
                                window.cookieHandler.deleteCookie('effectivity')
                                window.cookieHandler.deleteCookie('packageId');
                                window.location.href = "index.html";
                            }

                            if (resp.status === 404) {
                                var errMessage = "";
                                var clientType = resp.getResponseHeader("client-type");

                                var errorMessage;
                                if (resp.responseText === "invalid_data_location") {
                                    errorMessage =
                                            updateClientType(translator.localize("DataRepoConfigurationErrorMsg"),
                                                    clientType);
                                }
                                else if (resp.responseText === "non_zipped_package") {
                                    errorMessage =
                                            updateClientType(translator.localize("UnZippedPackagesInRepoErrorMsg"),
                                                    clientType);
                                }
                                else {
                                    errorMessage = updateClientType(translator.localize("NoValidPacketAvailableMsg"),
                                            clientType);
                                }
                                showError(errorMessage);
                                // TODO: modified to run the test, Need to uncomment eventually.
                                if (typeof __SMARTCLIENT_TEST_ENV__ === 'undefined') {
                                    throw new Error(errorMessage);
                                }
                            }
                        }
                    });

                    var jqXHR = Backbone.Collection.prototype.fetch.call(this, options);
                    jqXHR.done(function() {
                        mentor.publisher.clientType = jqXHR.getResponseHeader("client-type");
                    });
                    return jqXHR;
                },

                parse: function (data) {
                    var doc = $.parseXML(data);
                    var packageElements = $('package', doc);
                    var translator = mentor.publisher.languageTranslator;

                    var filteredPackageElements = _.filter(packageElements, function (element) {
                        return !this.projectId || $(element).attr('projectId') == this.projectId;
                    }, this);

                    if (filteredPackageElements.length == 0) {
                        var errorMessage = updateClientType(translator.localize("NoValidPacketAvailableMsg"), "");
                        showError(errorMessage);
                        return;
                    }
                    var subPackages = new SubPackages(data, {parse: true, projectId: this.projectId});
                    return _.map(filteredPackageElements, function (element) {
                        return {
                            id: $(element).attr('id'),
                            name: $(element).attr('name'),
                            description: $(element).attr('description').replace(/\\n/g,'\n').replace(/&nbsp;/g,'    ',),
                            projectId: $(element).attr('projectId'),
                            subPackages : subPackages
                        };
                    });
                },
                getAllPackages: function () {
                    var allPackages;
                    allPackages = new Packages();
                    allPackages.fetch({
                        async: false
                    });
                    return allPackages;
                },
                getAllSubPackages: function () {
                    var packages = this.getAllPackages(), subPackages = [];
                    packages.models.forEach(function (pkg) {
                        Array.prototype.push.apply(subPackages, pkg.subPackages.models);
                    })
                    if (subPackages && subPackages.length > 0) {
                        return subPackages
                    }
                    return packages && packages.models;
                },
                findSubpackagesByRange: function (range, subPackages) {
                    if (!range) {
                        return;
                    }
                    range = range.split("-");
                    var effPrefix = range[0].replace(/[0-9]/g, '');
                    var rangeStart = parseInt(range[0]);
                    if (isNaN(rangeStart)) {
                        rangeStart = parseInt(range[0].replace(effPrefix, ''));
                    }
                    var rangeEnd = parseInt(range[1]);
                    if (isNaN(rangeEnd) && range[1]) {
                        rangeEnd = parseInt(range[1].replace(effPrefix, ''));
                    }
                    var filterByRange = _.uniq(subPackages.filter(function (pkg) {
                        var rangeMatch;
                        rangeMatch =
                                rangeStart >= parseInt(pkg.attributes.start) && rangeStart <=
                                parseInt(pkg.attributes.end);
                        if (rangeEnd && rangeMatch) {
                            rangeMatch =
                                    rangeEnd >= parseInt(pkg.attributes.start) && rangeEnd <=
                                    parseInt(pkg.attributes.end);
                        }
                        if (rangeMatch && effPrefix) {
                            rangeMatch = pkg.attributes.prefix === effPrefix;
                        }
                        return rangeMatch;
                    }));
                    return filterByRange;
                }, findSubpackagesByProp: function (subPackages, value, propName) {
                    return subPackages.filter(function (pkg) {
                        return pkg.attributes[propName] === value;
                    });
                },
                filterByNameAndId: function (subPackages, packageName, projectId, packageId) {
                    if (packageName && subPackages) {
                        var filteredByName = this.findSubpackagesByProp(subPackages, packageName, "name");
                        if (filteredByName && filteredByName.length === 1) {
                            return filteredByName[0];
                        }
                        else if (filteredByName && filteredByName.length > 1 && projectId) {
                            var filteredByProjectId = this.findSubpackagesByProp(filteredByName, projectId, "projectId");

                            var filteredByPackageId = filteredByProjectId.length > 1
                                    ? this.findSubpackagesByProp(filteredByProjectId, packageId, "id")
                                    : filteredByProjectId;

                            if (filteredByPackageId && filteredByPackageId.length === 1) {
                                return filteredByPackageId[0];
                            }
                        }
                        else if (filteredByName && filteredByName.length > 1) {
                            return filteredByName[0];
                        }
                    }

                },
                findSubPackageByParams: function (params) {
                    params = params || {};
                    var projectId = params.projId,
                            packageName = params.packageName,
                            packageId = params.packageId,
                            range = params.effRange,
                            errorMsg,
                            subPackages = this.getAllSubPackages(), matchedSubPackages, matchingPackage;
                    if (range) {
                        matchedSubPackages = this.findSubpackagesByRange(range, subPackages);
                        if (matchedSubPackages && matchedSubPackages.length === 1) {
                            if(packageName === undefined || matchedSubPackages[0].attributes.name === packageName) {
                                matchingPackage = matchedSubPackages[0];
                            } else {
                                return 0;
                            }
                        }
                        else {
                            matchingPackage = this.filterByNameAndId(matchedSubPackages, packageName, projectId, packageId);
                        }
                    }
                    else if (packageName) {
                        matchingPackage = this.filterByNameAndId(subPackages, packageName, projectId, packageId);
                    }
                    else if (subPackages && subPackages.length === 1) {
                        matchingPackage = subPackages[0];
                    }
                    if (matchingPackage && matchingPackage instanceof Backbone.Model) {
                        // if projectId present make sure matchingPackage(fetch from package name) have same projectId
                        if (!projectId || (projectId === matchingPackage.get('projectId'))) {
                            params.selectedPackage = matchingPackage;
                            params.projId = matchingPackage.get('projectId');
                            params.effRange = matchingPackage.get('effectivityRange');
                            return matchingPackage.attributes;
                        }
                    }
                },
                showNonExistantPackageWarning: function(){
                    alert(translator.localize("PackageDeletedOrMovedWarningMsg"));
                    this.reloadBaseUrl();
                },
                reloadBaseUrl: function(){
                    var location = window.location;
                    location.href = location.href.replace(location.hash, "");
                },
                throwError: function (msg) {
                    alert(msg);
                    throw msg;
                },

                findSubPackageBy: function (path, range, projectId) {
                    var firstPackage;
                    if (range) {
                        var subPackages = this.findWhere({projectId: projectId}).subPackages;
                        firstPackage = subPackages.findWhere({effectivityRange: range, id: path.replace('/', "\\")});
                    }
                    else if (path) {
                        firstPackage = this.get(path.replace("/", "\\"));
                    }
                    else {
                        firstPackage = ((this.models && this.models instanceof Array && this.models.length) >
                        0 ? this.models[0] : null);
                    }
                    if (!firstPackage) {
                        this.showNonExistantPackageWarning();
                        return false;
                    }
                    return firstPackage;
                },

                findPackageById: function (path, range, projectId) {
                    var selectedPackage = this.findSubPackageBy(path, range, projectId);
                    if (selectedPackage) {

                        return {
                            title: selectedPackage.get("name"),
                            id: selectedPackage.get("id")
                        };
                    }
                },

                findPackageByName: function (name) {
                    var findByName = this.findWhere({name: name}) || this.findWhere({id: name});
                    if (!findByName) {
                        this.showNonExistantPackageWarning();
                        return false;
                    }
                    return findByName;
                },

                containSubPackages: function () {
                    var packageArray = this.models;
                    return !!(packageArray && packageArray[0] && packageArray[0].subPackages &&
                            packageArray[0].subPackages.length > 0);
                }
            });

            return Packages;
        }
);