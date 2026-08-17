function SVGElementVisibilityToggler(config, undercore, $)
{
    var svgRoot = config.root;
    var xmlLoader = config.xmlLoader;
    var designLevelElementSelector = "HarnessDiagram";
    var svgFragmentRootStart = "<svg xmlns=\"http://www.w3.org/2000/svg\">";
    var svgFragmentRootEnd = "</svg>";
    var svgFragmentFileExtension = ".xml";
    var svgFragmentFileType = "xml";
    return {
        hideDesignLevelElements: function () {
            if (this.designLevelElementsVisible) {
                this.designLevelElementsVisible = false;
                $("." + designLevelElementSelector, svgRoot).hide();
            }
        },
        showDesignLevelElements: function () {
            this.designLevelElementsVisible = true;
            this.appendSVGFragmentOnHighlight(designLevelElementSelector);
        },
        appendSVGFragmentOnHighlight: function (cssClassName) {
            var svgElement = svgRoot;
            var domParser = new DOMParser();
            var that = this;
            var elesToadd = svgElement.parentNode.getElementsByClassName(cssClassName);
            for(var i=0;i<elesToadd.length;i++) {
                var ele =  elesToadd.item(i);
                var elementId = ele.id;
                var svgFragmentToAdd = this.getSVGFragmentFor(elementId);
                var svgFragmentDOM = this.createDOMFromSVGFragment(domParser, svgFragmentToAdd)
                var childNodes = svgFragmentDOM.documentElement.childNodes;
                this.appendChildrenTo(ele, childNodes)
                $(ele).show();
            }
        },
        appendChildrenTo: function (element, children) {
            for (var index = 0; index < children.length; index++) {
                var childNode = children.item(index);
                var newChild = svgRoot.ownerDocument.importNode(childNode, true);
                element.appendChild(newChild);
            }
        },
        createDOMFromSVGFragment: function (domParser, svgFragmentToAdd) {
            return domParser.parseFromString(svgFragmentToAdd, 'application/xml');
        },
        getSVGFragmentFor: function (elementId) {
            var fileName = elementId + svgFragmentFileExtension;
            var baseURI = svgRoot.baseURI || svgRoot.parentNode.URL;
            var dirEndIndex = baseURI.lastIndexOf("/");
            var svgFragmentPath = baseURI.substr(0, dirEndIndex) + "/" + fileName;
            var svgFragment = xmlLoader.loadFile(svgFragmentPath, false, false, svgFragmentFileType);
            var responseText = svgFragment.xmlHttpObject.responseText;
            return responseText;
        },
        toggleObjectLevelElementVisibility: function (elementIds) {

            var differentElementSelected = !undercore.isEqual(this.previousIds, elementIds);
            if (elementIds && elementIds.length > 0 && differentElementSelected) {
                if (this.svgHasElements(elementIds)) {
                    this.hidePreviouslyShownElements(elementIds);
                    elementIds.forEach(function (elementCSS) {
                        this.appendSVGFragmentOnHighlight(elementCSS)
                    }.bind(this));
                }
            }
        },
        svgHasElements: function (elementIds) {
            for (var i = 0; i < elementIds.length; i++) {
                if ($("." + elementIds[i], svgRoot).length > 0) {
                    return true;
                }
            }
            return false;
        },
        hidePreviouslyShownElements: function (elementIds) {
            this.previousIds = this.previousIds || [];
            this.previousIds.forEach(function (objectIdAsCSSClass) {
                var elesToRemove = svgRoot.parentNode.getElementsByClassName(objectIdAsCSSClass);
                for(var eRemove=0;eRemove<elesToRemove.length;eRemove++) {
                    $(elesToRemove.item(eRemove)).hide();
                }
            }.bind(this));
            this.previousIds = elementIds;
        }
    }
}
