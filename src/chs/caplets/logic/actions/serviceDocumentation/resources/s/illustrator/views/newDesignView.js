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
            var view = HarnessDesignView(p.contentType.NEW_DESIGN_REVISION, "newHarnessLayoutId");
            view.getDocumentTitle = function (documentSet)
            {
                return mentor.publisher.languageTranslator.localize('New') + " " +
                        documentSet.getNameWithPartNumberAndRevision();
            };
            return view;
        }
)