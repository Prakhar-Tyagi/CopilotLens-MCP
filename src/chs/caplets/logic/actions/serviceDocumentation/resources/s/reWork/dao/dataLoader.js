/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global xmlDataLoader, mentor, objectFactoryImpl, createObjectUIDToOptionExpressionMap*/
mentor.publisher.dataLoader = (function (p) {
    "use strict";
    var projectRoot;

    require(["currentPackage"], function (selectedPackage) {
        if (selectedPackage) {
            selectedPackage.on("change:id", function () {
                mentor.publisher.dataLoader.objectMap = undefined;
            });
        }
    });

    return {
        getObjectByName:function (name, type, diagramName) {
            return this.dataLoader.getObjectByName(name, type, diagramName);
        },
        createOptionExpressionMap:function () {
            this.objectMap = createObjectUIDToOptionExpressionMap(mentor.publisher.project.getId());
        },
        dataLoader:xmlDataLoader(objectFactoryImpl()),
        loadProject:function (projectRootParam) {
            //if the project is already loaded, do not load it again
            if (p.project && (p.project.getId() === projectRootParam)) {
                return p.project;
            }
            var project;
            projectRoot = projectRootParam;
            project = this.dataLoader.getProject(projectRoot);
            // cache systems
            p.cache.storeObjectInCache(project.getSystems());
            p.project = project;
            this.loadConfigurationData(project.getId(), function (configData) {
                if (configData.textValue !== 'failure') {
                    mentor.publisher.colors = configData.dataArray;
                    mentor.publisher.config = configData.dataArray;
                    mentor.publisher.config.navHidden = mentor.publisher.config['hide-navigation-panel'] === 'true';
                    window.heavySVGs = mentor.publisher.config['only-show-tables-on-click'] === 'true';
                    window.heavySVGs = window.heavySVGs || mentor.publisher.config['only-show-symbols-on-click'] === 'true';
                    window.heavySVGs = (mentor.publisher.clientType === "CapitalChangeExplorer" && !p.project.getSystems().length) ? window.heavySVGs : false;
                }

            });
            mentor.publisher.project = project;
            return project;
        },

        loadServerConfig:function (callback){
            var self = this;
            Promise.all([this.fetchPrivacyPolicy(), this.fetchCookieDuration()])
                .then(([privacyPolicyData, cookieDurationData]) => {
                    mentor.publisher.serverConfig={}
                    mentor.publisher.serverConfig['privacy-policy'] = privacyPolicyData;
                    mentor.publisher.serverConfig['cookies-duration'] = cookieDurationData;
                    self.changeDurationOfEulaAcceptedCookie();

                    if (typeof callback === 'function') {
                        callback();
                    }
                })
                .catch(error => {
                    console.error('There has been a problem with your fetch operations:', error);
                });
        },

        changeDurationOfEulaAcceptedCookie:function (){
            Utils.createCookie("eula_rev_210520", "", -1);
            Utils.createCookie("eula_rev_210520", "accepted", Utils.getCookiesDuration());
        },

        fetchPrivacyPolicy: function () {
            return fetch('privacyPolicy')
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Network response was not ok for privacy policy');
                    }
                    return response.text();
                });
        },

        fetchCookieDuration: function () {
            return fetch('cookieDuration')
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Network response was not ok for cookie duration');
                    }
                    return response.text();
                });
        },

        getNavigationPanelOrder:function (project) {
            project = project || projectRoot;
            return this.dataLoader.getNavigationPanelOrder(project);
        },
        getNavigationPanels:function (project) {
            project = project || projectRoot;
            return this.dataLoader.getNavigationPanels(project);
        },
        getNavigationPanelObjectMap:function () {
            return this.dataLoader.getNavigationPanelObjectMap();
        },
        getRelatedDataPanelOrder : function (projectRoot, designRoot, diagramId) {
            return this.dataLoader.getRelatedDataOrder(projectRoot, designRoot, diagramId);
        },
        getDesignObjects:function (systemId, objectType) {
            var xmlNodeName = objectType === p.contentType.SIGNALS ? p.contentType.SIGNAL : "";
            return p.filter.applyFilter(this.dataLoader.getDesignObjects(systemId, objectType, xmlNodeName));
        },
        getFaceViewSymbol: function (symbol, systemId, projectId) {
            return this.dataLoader.getFaceViewSymbol(symbol, systemId, projectId);
        },
        getCavityTable: function (table, systemId, projectId) {
            return this.dataLoader.getCavityTable(table, systemId, projectId);
        },
        getWindowTitleConfigData: function () {
            return this.dataLoader.getWindowTitleConfigData();
        },
        getPopoverOrder: function () {
            return this.dataLoader.getPopoverOrder();
        },
        getCustomPopoverSectionOrder: function () {
            return this.dataLoader.getCustomPopoverSectionOrder();
        },
        getObjectPropertyToUseForTitle: function (objectType) {
            return this.dataLoader.getObjectPropertyToUseForTitle(objectType);
        },
        getSignalObjects: function (signalName, systemId) {

            return this.dataLoader.getSignalObjects(signalName, systemId);
        },
        loadPackages:function () {
            return this.dataLoader.loadPackages();
        },
        get3dXmlId:function (currentProject, currentFolder, fileName, callback) {
            this.dataLoader.get3dXmlId(currentProject, currentFolder, fileName, callback);
        },
        loadConfigurationData:function (currentProject, callback) {
            this.dataLoader.loadConfigurationData(currentProject, callback);

        },
        loadFaultCodeById:function (faultCodeId) {
            return this.dataLoader.loadFaultCodeById(p.project.getId(), faultCodeId);
        },
        getSignalDataForHighlightInRenderedSVG:function (signalName, callback) {
            this.dataLoader.getSignalDataForHighlightInRenderedSVG(signalName, callback);
        },
        loadOptionFilterInfo:function () {
            return this.dataLoader.loadOptionFilterInfo(mentor.publisher.project.getId());
        },
        getObjectById : function (id, type, diagramName) {
            return this.dataLoader.getObjectById(id, type);
        },
        getProjectPreferences: function () {
            return this.dataLoader.getProjectPreferences(mentor.publisher.project.getId());
        }
    };

}(mentor.publisher));




