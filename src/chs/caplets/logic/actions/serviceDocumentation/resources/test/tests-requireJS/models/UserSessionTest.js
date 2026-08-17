/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, ?SISW?), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer?s 
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */
require([
    "UserSession",
], function (UserSession) {
    "use strict";

    describe("UserSession", function () {
        var session;

        beforeEach(function () {
            spyOn(Utils, "createCookie");
            session = new UserSession();
        });

        it("should use default cookie duration when window.mentor is undefined", function () {
            var originalMentor = window.mentor;
            window.mentor = undefined;
            const newProject = new Backbone.Model({ id: "1", name: "Project_1" });
            session.get(UserSession.kProjectsProperty).add(newProject);
            session.set(UserSession.kSelectedProjectProperty, newProject);
            const lastViewedProjectIDCookie = Utils.getLocationSpecificCookieName("lastViewedProjectID");
            expect(Utils.createCookie).toHaveBeenCalledWith(lastViewedProjectIDCookie, "1", 365);
            window.mentor = originalMentor;
        });

        it("should use default cookie duration when serverConfig is missing", function () {
            var originalMentor = window.mentor;
            window.mentor = { publisher: {} };
            const newProject = new Backbone.Model({ id: "1", name: "Project_1" });
            session.get(UserSession.kProjectsProperty).add(newProject);
            session.set(UserSession.kSelectedProjectProperty, newProject);
            const lastViewedProjectIDCookie = Utils.getLocationSpecificCookieName("lastViewedProjectID");
            expect(Utils.createCookie).toHaveBeenCalledWith(lastViewedProjectIDCookie, "1", 365);
            window.mentor = originalMentor;
        });
    });
});

