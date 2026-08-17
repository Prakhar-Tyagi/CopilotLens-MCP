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
        "backbone","PackagesInSession"
    ],
    function (Backbone, packagesInSession) {
        "use strict";

        var activeSession,
            kLastViewedProjectIDCookie = "lastViewedProjectID",
            kProjectsProperty = 'projects',
            kSelectedPackageProperty = 'selectedPackage',
            kSelectedProjectProperty = 'selectedProject',
            UserSession;

        UserSession = Backbone.Model.extend({
            initialize: function () {
                var projects = packagesInSession.get("projects");
                var packages = packagesInSession.get("packages");
                var emptyProjects = _.filter(projects.models, function(model){
                    return packages.findWhere({projectId: model.id}) === undefined;
                }, this)
                .map(function(model){
                    return model.get("id");
                });

                projects.remove(emptyProjects, {silent: true});

                this.set(kProjectsProperty, projects);

                var lastViewedProjectID,
                    lastViewedProjectIDCookie,
                    lastViewedProject;

                lastViewedProjectIDCookie = Utils.getLocationSpecificCookieName(kLastViewedProjectIDCookie);
                lastViewedProjectID = Utils.readCookie(lastViewedProjectIDCookie);
                if (lastViewedProjectID && lastViewedProjectID !== "undefined") {
                    lastViewedProject = projects.get(lastViewedProjectID);
                }
                if (!lastViewedProject) {
                    lastViewedProject = projects.at(0);
                }
                this.set(kSelectedProjectProperty, lastViewedProject);

                this.on("change:" + kSelectedProjectProperty, function (session) {
                    this.unset(kSelectedPackageProperty);
                    Utils.createCookie(lastViewedProjectIDCookie, session.get(kSelectedProjectProperty).id, Utils.getCookiesDuration());
                }, this);
            }

        }, {
            kProjectsProperty: kProjectsProperty,
            kSelectedPackageProperty: kSelectedPackageProperty,
            kSelectedProjectProperty: kSelectedProjectProperty,

            getActiveSession: function () {
                if (!activeSession) {
                    activeSession = new UserSession();
                }

                return activeSession;
            }

        });

        return UserSession;
    }
)