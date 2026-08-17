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
        "jquery",
        "underscore",
        "backbone",
        "UserSession"
    ],
    function ($, _, Backbone, UserSession) {
        "use strict";

        var ProjectsPanel;

        ProjectsPanel = Backbone.View.extend({

            events: {
                "click .titlebar": "onTitlebarClick",
                "click .listItem": "onItemClick"
            },

            onTitlebarClick: function (event) {
                $(event.currentTarget).parent().find(".listItem").each(function () { $(this).toggle(); });

                event.stopPropagation();
            },

            onItemClick: function (event) {
                var id = $(event.currentTarget).attr('data-id');
                var activeSession = UserSession.getActiveSession();
                var projects = activeSession.get(UserSession.kProjectsProperty);
                var clickedProject = projects.get(id);

                activeSession.set(UserSession.kSelectedProjectProperty, clickedProject);

                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER, {});

                event.stopPropagation();
            },

            render: function () {
                var currentProject,
                    options,
                    panel,
                    projects,
                    renderedPanel;

                panel = this;

                currentProject = UserSession.getActiveSession().get(UserSession.kSelectedProjectProperty);
                projects = UserSession.getActiveSession().get(UserSession.kProjectsProperty);

                options = {};
                options.className = "projects-panel";
                options.expand = true;
                options.items = projects.map(function (project) {
                    var clone;

                    clone = project.clone();
                    clone.isActive = "";

                    return clone;
                }).filter(function (project) {
                        return project.id != currentProject.id;
                    });
                options.showPopup = false;
                options.showTitle = false;
                options.title = "";
                options.totalItems = projects;

                renderedPanel = _.template(ProjectsPanel.templateHTML)(options);
                panel.$el.append(renderedPanel);

                return this;
            }

        });

        return ProjectsPanel;
    }
)