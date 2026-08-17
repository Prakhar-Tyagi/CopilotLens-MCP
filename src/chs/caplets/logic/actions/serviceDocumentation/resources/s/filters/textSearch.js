/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define("textSearch",
        [
            "filters/documentContentBasedFilter",
            "filters/attributeBasedFilter"
        ],
        function (attrAndDocContentFilter,
                attributeBasedFilter) {
            "use strict";
            var p = mentor.publisher;
            return function (collections, useAttrBasedSearch) {
                return {
                    //for UT
                    indexSearchEnabled: false,
                    //for UT
                    setIndexBasedSearch: function (indexBasedSearch) {
                        this.indexBasedSearch = indexBasedSearch;
                    },
                    //for UT
                    setAttrBasedSearch: function (attrBasedSearch) {
                        this.attrBasedSearch = attrBasedSearch;
                    },
                    areIndexesGenerated: function () {
                        return attrAndDocContentFilter.areIndexesGenerated(p.project.getId());
                    },
                    getFilter: function () {
                        var indexBasedSearch = this.indexBasedSearch || attrAndDocContentFilter;
                        var attrBasedSearch = this.attrBasedSearch || attributeBasedFilter;
                        var isEnabled = !useAttrBasedSearch && (this.areIndexesGenerated() || this.indexSearchEnabled);
                        if (isEnabled && typeof collections.category != "undefined") {
                            return indexBasedSearch;
                        }
                        else {
                            return attrBasedSearch;
                        }
                    },
                    filterByText: function (searchText) {
                        if (searchText) {
                            return this.getFilter().filter(collections, searchText);
                        }
                        else {
                            return collections.filter(function () {
                                return true;
                            });
                        }

                    }
                };

            };
        });
