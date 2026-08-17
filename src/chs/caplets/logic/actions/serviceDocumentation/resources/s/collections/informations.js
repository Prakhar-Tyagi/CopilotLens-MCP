/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, Backbone, xmlDataLoader, objectFactoryImpl, mentor*/
define(["SectionCollection", "currentPackage"],
        function (BaseCollection, selectedPackage)
        {
            "use strict";
            if(Utils.isPopoutWindow()) {
                mentor.publisher.informationData = getWindowObj().mentor.publisher.informationData;
            } else {
                var p = mentor.publisher;
                var IntroductionSection = BaseCollection.extend({
                    category: p.documentCategory.INFORMATION,
                    initialize: function ()
                    {
                        selectedPackage.on("change:id", this.fetch, this);
                        selectedPackage.on("change:language", this.fetch, this);
                    },
                    encodePath: function (informationData)
                    {
                        return informationData;
                    },
                    getData: function (selectedProject)
                    {
                        return (selectedProject && mentor.publisher.LanguageFilteredProject.filterInformationPages(
                                this.encodePath(selectedProject.getInformation()))) || [];
                    }
                });
                mentor.publisher.informationData = new IntroductionSection();
            }
            return mentor.publisher.informationData;
        });