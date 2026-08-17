/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, _*/
define(function ()
{
    "use strict";
    var filteredIndexedData = {};
    var searchEndPoint = "search";
    var searchIndexProgress = "searchindexprogress";
    var searchCache = false;
    var indexGenerationStatusURL = "packageconfig";
    var p = mentor.publisher;

    return {
        SHOW_FIRST_SECTION_ITEM: -1,
        areIndexesGenerated: function ()
        {
            this.fetchSearchIndexProgress();
            var url = indexGenerationStatusURL;
            var effSetter = require("filehandlers/effectivitySetter");
            effSetter.addEffectivitAndZipLocationInURLAsParameters(url);
            var status = p.xmlLoader.loadGlobalFile(effSetter.addEffectivitAndZipLocationInURLAsParameters(url),
                            /*async?*/
                            false,
                            /*cache?*/
                            searchCache,
                            /*content type*/
                            "json").data || {};
            if (status) {
                return status['full-document-search'];
            }
        },
        logMessage: function (message)
        {
            if (window.console) {
                window.console.log(message);
            }
        },

        fetchSearchIndexProgress: function ()
        {
            var url = searchIndexProgress;
            var effSetter = require("filehandlers/effectivitySetter");
            effSetter.addEffectivitAndZipLocationInURLAsParameters(url);
            var status = p.xmlLoader.loadGlobalFile(effSetter.addEffectivitAndZipLocationInURLAsParameters(url),
                            /*async?*/
                            false,
                            /*cache?*/
                            searchCache,
                            /*content type*/
                            "json").data || {};
            if (status) {
                searchCache = status['is-search-indexing-completed'];
                return searchCache;
            }
        },
        fetchSearchIndexes: function (searchText)
        {
            var projectId, lang, options;
            projectId = p.project.getId().replace("/", "\\");
            lang = p.LanguageFilteredProject.getCurrentLanguage();
            options = p.filter.vinOptions;

            var params = {
                packageId: projectId,
                language: lang,
                q: searchText,
                options: options
            };
            filteredIndexedData = p.xmlLoader.callAPI(searchEndPoint, params,
                            /*async?*/
                            false,
                            /*cache?*/
                            true,
                            /*content type*/
                            "json").data || {};
            this.logMessage(filteredIndexedData);
            return filteredIndexedData;
        },
        filter: function (objects, searchText)
        {
            this.sectionToFilter = objects.category;
            this.fetchSearchIndexes(searchText);
            var that = this;
            if (objects) {
                return objects.filter(function (item)
                {
                    return that.filterUsingIndexedData(item, objects);
                });
            }
            return objects;
        },
        getFilteredDataById: function (id, category, searchText)
        {
            searchText = searchText || "";
            if (searchText.trim()) {
                if (window.diagramAsSystemsFlow &&
                        category === p.documentCategory.SYSTEMS) {
                    return this.SHOW_FIRST_SECTION_ITEM;
                }
                this.fetchSearchIndexes(searchText);
                var sectionDataArr = this.getFilteredDataFor(category);
                var item = _.findWhere(sectionDataArr, {id: id});
                if (item) {
                    return item.items || {};
                }
            }
        },
        getFilteredDataFor: function (category)
        {
            var data = filteredIndexedData[category];
            if (!data) {
                var lowerCase = (category || "").toLowerCase();
                data = filteredIndexedData[lowerCase];
            }
            return data || [];
        },
        getObjectId: function (objects, item)
        {
            var objectId = item.get("id");
            return (objects.getIdToFilter && objects.getIdToFilter(item)) || objectId;
        },
        filterItems: function (element, id, item, diagramAsSystemsFlow)
        {
            if (diagramAsSystemsFlow) {
                if (element.items && element.items[p.documentCategory.DIAGRAMS]) {
                    return element.items[p.documentCategory.DIAGRAMS].some(function (ele)
                    {
                        return ele.id === item.get("idAttribute") ||
                            ele.id === item.get('id');
                    });
                }
            }
            return element.id === id;
        },
        filterUsingIndexedData: function (item, objects)
        {
            var dataCategory = (this.sectionToFilter || "");
            var itemsOfType = this.getFilteredDataFor(dataCategory);
            var that = this;
            if (itemsOfType && itemsOfType.length > 0) {
                var id = this.getObjectId(objects, item);
                if (id) {
                    return itemsOfType.some(
                            function (element)
                            {
                                return that.filterItems
                                (
                                        element,
                                        id,
                                        item,
                                        window.diagramAsSystemsFlow
                                );
                            });
                }
            }
            return false;
        }
    };
});
