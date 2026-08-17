/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(["fileDisplayHandler", "componentRouter"],
        function (fileDisplayHandler, componentRouter)
        {
            return extend(componentRouter, {

                showComponent: function (component)
                {
                    mentor.publisher.router.openFaceView(component.objectId, component.systemId, "", function (object)
                    {
                        return ((object && object.view) || "").toLowerCase();
                    });
                    // componentRouter.showComponent(component);

                }
            });
        });
