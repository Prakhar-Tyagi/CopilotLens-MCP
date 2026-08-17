/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define(["jquery", "underscore", "backbone", "Project"],
    function ($, _, Backbone, Project) {
        "use strict";

		return Backbone.Collection.extend({
			model: Project,
			url: "unzipped/data/projects.xml",

			fetch: function (options)
			{
				_.extend(options, {
					dataType: "text"
				});

				return Backbone.Collection.prototype.fetch.call(this, options);
			},

			parse: function (data)
			{
				var doc = $.parseXML(data);
				var projectElements = $('project', doc);

				return _.map(projectElements, function (element)
				{
					return {
						id: $(element).attr('id'),
						mainText: $(element).attr('name'),
						subText: $(element).attr('description')
					};
				});
			},

			getProjectAtIndex: function (index)
			{
				if (index < 0 || index >= this.length) {
					return null;
				}

				return this.at(index);
			}
		});
    }
);