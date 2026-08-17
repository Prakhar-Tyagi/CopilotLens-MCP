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
            "illustrator/views/HarnessDesignView"
        ],
        function (HarnessDesignView,
                selectedSystem,
                fileDisplayHandler,
                harnessLayouts)
        {
            "use strict";
            var p = mentor.publisher;
            var view = HarnessDesignView(p.contentType.OLD_DESIGN_REVISION, "oldHarnessLayoutId");
            view.getDocumentTitle = function (documentSet)
            {
                return mentor.publisher.languageTranslator.localize('Old') + " " +
                        documentSet.getNameWithPartNumberAndRevision();
            };
            return view;
        }
)