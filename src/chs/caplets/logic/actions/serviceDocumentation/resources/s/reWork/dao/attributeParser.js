/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

(function (p)
{
    "use strict"
    p.attributeParser = function (DomUtil)
    {
        DomUtil = DomUtil || $;
        return function (attributeNode, config)
        {
            var attr = {}, name = DomUtil("name", attributeNode).text().trim() || "",
                    value = DomUtil("value", attributeNode).text().trim() || "";
            config = config || {};
            config.translator = config.translator || mentor.publisher.LanguageFilteredProject;

            attr.name = config.translator.translateQuickCode(name);
            attr.value = config.translator.translateQuickCode(value);
            attr.getId = function ()
            {
                return name;
            };
            config.callback && config.callback(attr);
        }

    }

}(mentor.publisher));
