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
        "preferences",
        "UserSession",
        "ProjectsViewModel"
    ],
    function ($, _, Backbone, preferences, UserSession, ProjectsViewModel) {
        "use strict";

        var ProjectsView = Backbone.View.extend({
            events: {
                'click #next-project-btn' : function () {
                    this.model.nextProjectButtonClicked();
                },
                'click #previous-project-btn' : function () {
                    this.model.previousProjectButtonClicked();
                }
            },

            initialize: function () {
                this.model = new ProjectsViewModel();
                this.model.on("change:" + ProjectsViewModel.kCurrentIndexProperty, this.render, this);

                preferences.on("change:language", this.render, this);
            },

            render: function () {
                this.model.localizedSort();

                this.setElement(this.container);
                this.$el.html(_.template(this.templateHTML)(
                    this.getAdaptedJSON(this.model.toJSON())
                ));

                $("#project-thumbnail").css("opacity", 1);

                return this;
            },

            convertToHtmlEntities: function (input) {
                var tempElement = document.createElement('div');
                tempElement.textContent = input;
                return tempElement.innerHTML;
            },

            getAdaptedJSON: function (data) {
                var nextTitle,
                    previousTitle,
                    projectDescription,
                    projectTitle;

                nextTitle = (data.nextProject) ? Utils.translate(data.nextProject.get("mainText")) : null;
                previousTitle = (data.previousProject) ? Utils.translate(data.previousProject.get('mainText')) : null;
                projectDescription = this.convertToHtmlEntities(
                        Utils.translate(data.project.get('subText')).replace(/\\n/g, "<br>"));
                projectTitle = this.convertToHtmlEntities(Utils.translate(data.project.get('mainText')));

                return {
                    nextTitle: nextTitle,
                    previousTitle: previousTitle,
                    projectDescription: projectDescription,
                    project: data.project,
                    projectTitle: projectTitle
                }
            }
        });

        return new ProjectsView();
    }
);