/*global define, mentor, Backbone*/
define(["backbone", "Projects", "Packages"], function (backbone, Projects, Packages) {
    "use strict";
    var PackagesInSession = Backbone.Model.extend({
        defaults: {
            packages: {},
            projects: {}
        },
        initialize: function () {
            var projects = new Projects();
            projects.fetch({
                async: false
            });
            var packages = new Packages();
            packages.fetch({
                async: false
            });
            this.set('packages', packages.clone());
            this.set('projects', projects.clone());
        }
    });
    return new PackagesInSession();
});