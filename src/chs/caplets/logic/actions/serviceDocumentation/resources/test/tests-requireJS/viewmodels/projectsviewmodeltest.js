/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/** globals createContext, describe, it, $, _, Backbone */
(function ()
{
    'use strict';

    var activeSession = new Backbone.Model(),
        context,
        kProjectsProperty = "kProjectsProperty",
        kSelectedProjectProperty = "kSelectedProjectProperty",
        projects = new Backbone.Collection(),
        stubs,
        UserSession;

    projects.reset([{name: "Alpha"}, {name: "Beta"}, {name: "Gamma"}]);
    activeSession.set(kProjectsProperty, projects);

    UserSession = {
        getActiveSession: function ()
        {
            return activeSession;
        },
        kProjectsProperty: kProjectsProperty,
        kSelectedProjectProperty: kSelectedProjectProperty
    }
    stubs = {
        jquery: $,
        underscore: _,
        backbone: Backbone,
        UserSession: UserSession,
        Projects: Backbone.Collection
    };
    context = createContext(stubs);

    context(
        ['viewModels/ProjectsViewModel'],
        function (ProjectsViewModel)
        {
            describe('ProjectsViewModel', function ()
            {
                beforeEach(function ()
                {
                    activeSession.set(kSelectedProjectProperty, projects.at(0));
                });

                it('should load when initialized', function ()
                {
                    var model = new ProjectsViewModel();

                    expect(JSON.stringify(model.projects.toJSON())).toBe('[{"name":"Alpha"},{"name":"Beta"},{"name":"Gamma"}]');
                });

                it('should update the current index when the selected project changes', function ()
                {
                    var model = new ProjectsViewModel();
                    activeSession.set(kSelectedProjectProperty, projects.at(1));

                    var index = model.getCurrentProjectIndex();
                    expect(index).toBe(1);
                });

                it('should update the current project when next button is clicked', function ()
                {
                    var model = new ProjectsViewModel();
                    model.nextProjectButtonClicked();

                    var index = model.getCurrentProjectIndex();
                    expect(index).toBe(1);
                });

                it('should update the current project when previous button is clicked', function ()
                {
                    activeSession.set(kSelectedProjectProperty, projects.at(2));

                    var model = new ProjectsViewModel();
                    model.previousProjectButtonClicked();

                    var index = model.getCurrentProjectIndex();
                    expect(index).toBe(1);
                });
            });
        },
        function (err)
        {
            describe("ProjectsViewModel", function ()
            {
                it("should load", function ()
                {
                    expect(err).toBeUndefined();
                });
            });
        }
    )
})();