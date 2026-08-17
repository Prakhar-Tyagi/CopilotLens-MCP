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
            'jquery',
            'underscore',
            'backbone',
            'UserSession'
        ],
        function ($, _, Backbone, UserSession) {
            var kCurrentIndexProperty = "currentIndex";

            return Backbone.Model.extend({
                projects: null,

                initialize: function () {
                    var activeSession = UserSession.getActiveSession();
                    var projects = activeSession.get(UserSession.kProjectsProperty);
                    var selectedProject = activeSession.get(UserSession.kSelectedProjectProperty);

                    this.projects = projects;
                    this.set(kCurrentIndexProperty, projects.indexOf(selectedProject));

                    activeSession.on("change:" + UserSession.kSelectedProjectProperty, function () {
                        var selectedProject = activeSession.get(UserSession.kSelectedProjectProperty);

                        this.set(kCurrentIndexProperty, projects.indexOf(selectedProject));
                    }, this);
                },

                getCurrentProjectIndex: function () {
                    return this.get(kCurrentIndexProperty);
                },

                setCurrentIndex: function (index) {
                    var activeSession = UserSession.getActiveSession();
                    activeSession.set(UserSession.kSelectedProjectProperty, this.projects.at(index));
                },

                nextProjectButtonClicked: function () {
                    this.setCurrentIndex(this.getCurrentProjectIndex() + 1);
                },

                previousProjectButtonClicked: function () {
                    this.setCurrentIndex(this.getCurrentProjectIndex() - 1);
                },

                projectSelected: function (id) {
                    var project = this.projects.get(id);
                    var activeSession = UserSession.getActiveSession();
                    activeSession.set(UserSession.kSelectedProjectProperty, project);
                },

                localizedSort: function () {
                    this.projects.comparator = function (project) {
                        return Utils.translate(project.get('mainText'));
                    };
                    this.projects.sort();

                    var activeSession = UserSession.getActiveSession();
                    var selectedProject = activeSession.get(UserSession.kSelectedProjectProperty);

                    this.set(kCurrentIndexProperty, this.projects.indexOf(selectedProject));
                },

                toJSON: function () {
                    return {
                        project: this.projects.getProjectAtIndex(this.getCurrentProjectIndex()),
                        nextProject: this.projects.getProjectAtIndex(this.getCurrentProjectIndex() + 1),
                        previousProject: this.projects.getProjectAtIndex(this.getCurrentProjectIndex() - 1)
                    }
                }
            }, {
                kCurrentIndexProperty: kCurrentIndexProperty
            });
        }
);