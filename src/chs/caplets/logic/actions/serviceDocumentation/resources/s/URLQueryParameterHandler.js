/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global mentor, define, $ */
define([
    'backbone', "componentRouter", "PackagesInSession"
], function (Backbone, componentRouter, packagesInSession)
{
	"use strict";
    return {
        isPackageSpecifiedInQueryParam: function ()
        {
            var availablePackages;
            /**
             * if there are query parameters and no project is specified then take first project if there is only one package at the location.
             * if there are more than one project exported then dont show anything.
             *
             */
            var allPackages = packagesInSession.get("packages");
            availablePackages = allPackages.models;
            if (!$.isEmptyObject(mentor.publisher.urlParams) && !mentor.publisher.urlParams.project && !mentor.publisher.urlParams.projId) {
                // availablePackages = mentor.publisher.dataLoader.loadPackages();
                if (availablePackages && availablePackages.length >= 1) {
                    mentor.publisher.urlParams.project = availablePackages[0].get("name");
                }
            }

            return mentor.publisher.urlParams.project;

        },

        handleQueryParameters: function ()
        {
            var targetUrl, matchingSubPackage;
            var urlParams = mentor.publisher.urlParams;
            if (urlParams && this.isPackageSpecifiedInQueryParam()) {
                if (urlParams.system) {
                    if (urlParams.componentType === "report") {
                        mentor.publisher.router.showSystemReport(urlParams.project, urlParams.system,
                                urlParams);
                    }
                    else {
                        // Use of view=plugin is deprecated view=objectlink is a better alternative.
                        if (urlParams.view === "plugin" || urlParams.view === "objectlink" || urlParams.view === "showpowertoground") {
                            targetUrl = urlParams.view + "/" +
                                    encodeURIComponent(urlParams.project) + "/" +
                                    encodeURIComponent(urlParams.system) + "/" +
                                    encodeURIComponent(urlParams.componentType) + "/" +
                                    encodeURIComponent(urlParams.component);
                            if (urlParams.viewName) {
                                targetUrl = targetUrl + "/" + encodeURIComponent(urlParams.viewName);
                            }
                            Backbone.history.loadUrl(targetUrl);
                        } else {
                            mentor.publisher.router.showDiagram(urlParams.project, urlParams.system,
                                    urlParams);
                        }
                    }
                }
                else if (urlParams.harnesslayout) {
                        mentor.publisher.router.showLinkedHarnessLayoutDiagram(urlParams);
                }
                else if (urlParams.component || urlParams.componentUID) {
                    if (urlParams.view) {
                        urlParams.view = urlParams.view.toLowerCase();
                        targetUrl = encodeURIComponent(urlParams.project) + "/" +
                                urlParams.view + "/" +
                                encodeURIComponent(urlParams.component) + "/" +
                                encodeURIComponent(urlParams.componentType);
                        Backbone.history.loadUrl(targetUrl);
                    }
                    else if (!urlParams.viewName && !urlParams.view) {
                        componentRouter.findAndShowComponentByType(urlParams.project,
                                urlParams.component || urlParams.componentUID,
                                urlParams);
                    }
                }
                else if (urlParams.viewName) {
                    urlParams.view =
                            urlParams.view && urlParams.view.toLowerCase();
                    if (urlParams.view === 'information') {
                        targetUrl = encodeURIComponent(urlParams.project) + "/" +
                                urlParams.view + "/" +
                                encodeURIComponent(urlParams.viewName);
                    } else if (urlParams.view === mentor.publisher.contentType.RA_3D_MODEL.toLowerCase()) {
                        targetUrl = "rathreedview/" +
                                encodeURIComponent(urlParams.project) + "/" +
                                encodeURIComponent(urlParams.viewName);
                    } else if (urlParams.view === mentor.publisher.contentType.JT_3D_MODEL.toLowerCase()) {
                        targetUrl = "jtthreedview/" +
                                encodeURIComponent(urlParams.project) + "/" +
                                encodeURIComponent(urlParams.viewName);
                    } else {
                        // check if view is custompanel
                        const customPanel = this.getCustomPanel(urlParams.project, urlParams.view);
                        if (customPanel && customPanel.length) {
                            targetUrl = "customView/" +
                                    encodeURIComponent(urlParams.project) + "/" +
                                    encodeURIComponent(urlParams.viewName);
                        } else {
                            targetUrl = urlParams.view + "/" +
                                    encodeURIComponent(urlParams.project) + "/" +
                                    encodeURIComponent(urlParams.viewName);
                        }
                    }
                    Backbone.history.loadUrl(targetUrl);
                }
                else if (urlParams.activeFaultCodes || urlParams.passiveFaultCodes) {
                    matchingSubPackage = componentRouter.findProjectIdByName(urlParams.package, urlParams.effRange, urlParams.projId) || "";
                    mentor.publisher.router.showTroubleshoot(urlParams.package, {
                        projId: matchingSubPackage.projectId,
                        activeFaultCodes: urlParams.activeFaultCodes,
                        passiveFaultCodes: urlParams.passiveFaultCodes
                    });
                }
                else {
                    //load the specified project
                    matchingSubPackage = componentRouter.findProjectIdByName(urlParams.package, urlParams.effRange, urlParams.projId) || "";
                    if (matchingSubPackage) {
                        var projectId = matchingSubPackage.id || "";
                        var options = {
                            projectId: projectId,
                            language: urlParams.language,
                            information: urlParams.information,
                            config: urlParams.config,
                            VIN: urlParams.VIN,
                            query: urlParams.q,
                            navPanel: urlParams.navPanel,
                            projId: matchingSubPackage.projectId,
                            range: matchingSubPackage.effectivityRange,
                        };
                        mentor.publisher.router.loadViewer(options);
                    }
                    else {
                        alert(mentor.publisher.languageTranslator.localize("AlertProjCanNotBeLoaded").format(mentor.publisher.urlParams.project));
                        window.location.href = "index.html";
                    }
                }
                return false;
            }
            return true;
        },
        getCustomPanel: function(project, panel) {
            let navPanelObject = [];
            const matchingSubPackage = componentRouter.findProjectIdByName(project, "", "", "") || "";
            if (matchingSubPackage && matchingSubPackage.id) {
                var packageId = matchingSubPackage.id.replace(/\\/g, "/");
                var effSetter = require("filehandlers/effectivitySetter");
                effSetter.initializeEffectivity({
                    projId: "",
                    projectId: packageId,
                    range: ""
                });
                navPanelObject = mentor.publisher.dataLoader.getNavigationPanels(matchingSubPackage.id);
                if (navPanelObject) {
                    navPanelObject = navPanelObject.filter(nav => nav.type && nav.type.toLowerCase() === panel && nav.name === 'custompanel');
                }
            }
            return navPanelObject;
        }
    };
});
