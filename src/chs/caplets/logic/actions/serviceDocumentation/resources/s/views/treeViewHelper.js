/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

define("treeViewHelper", [
    'jquery',
    'underscore',
    'backbone'
], function ($, _, Backbone) {
    "use strict";

    function getUniqueId(text)
    {
        var randomNumber = Math.floor(Math.random() * 10000);
        return text + "::" + randomNumber;
    }

    function countFiles(folder)
    {
        return _.keys(folder.files || []).length +
                _.values(folder.folders || []).map(countFiles).reduce(function (x, y) {
                    return x + y;
                }, 0);
    }

    var treeViewHelper = _.extend({}, {
        setupOptions: function (options) {
            this.idString = options.idString;
            this.fileLabelSupplierFn = options.fileLabelSupplierFn.bind(this);
            this.folderLabelSupplierFn = options.folderLabelSupplierFn.bind(this);
            this.desginFolderProviderFn = options.desginFolderProviderFn.bind(this);
            this.folderSeparator = (options.folderSeparatorFn && options.folderSeparatorFn.call(this));
            this.showFolderCount = options.showFolderCount;
            this.showFoldersFirst = options.showFoldersFirst;
        },

        createTreeView: function (designs, options) {
            if (!options) {
                return "";
            }
            this.setupOptions(options);

            var folders = this.createTreeDataFromDesigns(designs);
            var treeHtml = '<ol class="tree auto-tree-folder" style="display: none">';
            var that = this;
            var sortedArray = this.sortKeys(folders.folders);
            for (var folder in sortedArray) {
                if (sortedArray.hasOwnProperty(folder) && sortedArray[folder]) {
                    treeHtml += this.renderFolder(folders.folders[sortedArray[folder]], true);
                }
            }
            treeHtml += "</ol>";
            return treeHtml;
        },

        createTreeDataFromDesigns: function (designs) {
            var folderSeparator, folders, parent;
            folderSeparator = this.folderSeparator;
            designs = designs || [], folders = {
                mainText: "",
                folders: {}
            };
            parent = folders;
            for (var design in designs) {
                if (!designs.hasOwnProperty(design)) {
                    continue;
                }

                var thisDesign = designs[design];
                var systemfolders = this.desginFolderProviderFn.call(this, thisDesign);

                if (!(systemfolders && systemfolders instanceof Array)) {
                    return folders;
                }

                systemfolders.forEach(function (systemfolder) {
                    var foldersInfo;
                    if (this.folderSeparator) {
                        foldersInfo = systemfolder.split(folderSeparator);;
                    } else {
                        foldersInfo = systemfolder.replace(/\\/g, "/").split("/")
                    }

                    for (var f in foldersInfo) {
                        let decodedString;
                        try {
                            decodedString = decodeURIComponent(foldersInfo[f]);
                        }catch (e) {
                            // console.log(e);
                            decodedString = foldersInfo[f];
                        }
                        let foldersInfoElement = this.escape(decodedString);
                        var fol = {
                            mainText: foldersInfoElement,
                            folders: {},
                            files: {}
                        };
                        var existingFolder = parent.folders[foldersInfoElement];
                        if (!existingFolder) {
                            parent.folders[foldersInfoElement] = fol;
                        }
                        parent = parent.folders[foldersInfoElement];
                    }
                    var idKey = this.idString;
                    parent.files[thisDesign.get(idKey)] = thisDesign;
                    parent = folders;
                }.bind(this));
            }
            return folders;
        },

        escape: function (htmlStr) {
            return htmlStr.replace(/&/g, "&amp;")
                    .replace(/</g, "&lt;")
                    .replace(/>/g, "&gt;")
                    .replace(/"/g, "&quot;")
                    .replace(/'/g, "&#39;");

        },

        renderFolder: function (folder, showExpandCollapse) {
            var treeHTML = "";
            treeHTML += "<li>";
            var folders = folder.folders, files = folder.files;
            var totalItems = Object.keys(folders).length + Object.keys(files).length;
            if (folder.mainText) {
                var uniqueId = getUniqueId(folder.mainText);
                treeHTML +=
                        '<label style="white-space: pre;" class="auto-tree-folder-label" ' +
                        'title="' + Utils.translate(this.folderLabelSupplierFn.call(this, folder.mainText)) + '"' +
                        'for="' + uniqueId + '">'
                        + Utils.translate(this.folderLabelSupplierFn.call(this, folder.mainText)) + '</label>';

                if (this.showFolderCount) {
                    treeHTML += '<span class="folder-count">' + countFiles(folder) + '</span>';
                }
                if (showExpandCollapse) {
                    treeHTML += '<span class="expand-tree auto-tree-expand-all"></span>';
                    treeHTML += '<span class="collapse-tree auto-tree-collapse-all"></span>';
                }
                treeHTML += '<input class="auto-tree-folder-checkbox" type="checkbox"  id="' + uniqueId + '">';
            }

            if (totalItems > 0) {
                treeHTML += "<ol>";
            }

            if (Object.keys(files).length > 0 && !this.showFoldersFirst) {
                var filesHtml = this.handleChildFiles(files);
                treeHTML += filesHtml;
            }

            if (Object.keys(folders).length > 0) {
                var foldersHtml = this.handleChildFolders(folders);
                treeHTML += foldersHtml;
            }

            if (Object.keys(files).length > 0 && this.showFoldersFirst) {
                var filesHtml = this.handleChildFiles(files);
                treeHTML += filesHtml;
            }

            if (totalItems > 0) {
                treeHTML += "</ol>";
            }

            treeHTML += "</li>";
            return treeHTML;
        },

        handleChildFiles: function (files) {
            var first = true, treeHTML = "";

            var filesArr = [];
            for (var key in files) {
                filesArr.push(files[key]);
            }

            var sortedFiles = filesArr.sort(function (a, b) {
                return Utils.alphaNumericCompareFn(a.get("mainText"), b.get("mainText"));
            }.bind(this));

            for (var i in sortedFiles) {
                var thisDesign = sortedFiles[i];
                treeHTML +=
                        '<li data-id="' + thisDesign.get(this.idString) +
                        '" base-id="' + thisDesign.get("systemId") +
                        '" class="file listItem auto-tree-file">' +
                        '<span style="white-space: pre;" class="auto-tree-file-label">' + this.fileLabelSupplierFn.call(this, thisDesign) +
                        '</span>' +
                        '<span class="popUp auto-tree-file-popup"></span>' +
                        '</li>';
            }
            return treeHTML;
        },

        handleChildFolders: function (folders) {
            var first = true, treeHTML = "";
            var sortedFolders = this.sortKeys(folders);
            for (var sub in sortedFolders) {

                treeHTML += "<li><ol class='folder auto-tree-folder'>";
                if (sortedFolders.hasOwnProperty(sub) && sortedFolders[sub]) {
                    treeHTML += this.renderFolder(folders[sortedFolders[sub]], false);
                }
                treeHTML += "</ol></li>";
            }

            return treeHTML;
        },

        sortKeys: function (thisObj) {
            var objectKeysSorted = [];
            for (var key in thisObj) {
                if (thisObj.hasOwnProperty(key)) {
                    objectKeysSorted.push(key);
                }
            }
            objectKeysSorted = objectKeysSorted.sort(Utils.alphaNumericCompareFn)
            return objectKeysSorted;
        },


    });

    return {
        createTreeView: treeViewHelper.createTreeView.bind(treeViewHelper)
    };
});
