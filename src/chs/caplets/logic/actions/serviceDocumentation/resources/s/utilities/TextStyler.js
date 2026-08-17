/**
 * Created with IntelliJ IDEA.
 * User: kayyagar
 * Date: 11/17/12
 * Time: 12:38 AM
 * To change this template use File | Settings | File Templates.
 */
var Styler = function (node, str, port, svgDoc) {
    "use strict";
    var anchorAttr = 'start', boundingBoxX, boundingBoxY, process, textX , hJ , vJ , viewport, addTemporaryTextNode, flip, rot, addTSpanElements,
        textY , textWidth , textHeight , dy = textHeight, newTextX, drawBoundingBox, initialize, boundedWidth, boundedHeight, doFlip, addText,
        placeText, placeBoundedBox, doPostProcessing, rotation, toFlip, splittedTexts= [], willTextFit, isNewTextOk, currentNode, stylable = false;
    var _horizontalJustification = {
        left:0,
        middle:1,
        right:2
    };
    var _verticalJustification = {
        top:0,
        center:1,
        bottom:2
    };

    initialize = function () {
        //this is the styling algorithm
        //there are 3 cases
        //1.if the text is not a bounded text, apply the style on temporary node
        //temporary node is the node which is temporarily created with the translated text instead of the quick code
        //2 if the text is a bounded text
        //first stylize the node
        //then split the texts
        //then add the texts into multiple tspans/ one text element depending on the number of splits
        //do post processing on the node
        currentNode = node;
        viewport = port;
        textX = node.getBBox().x;
        textY = node.getBBox().y;
        textWidth = node.getBBox().width;
        textHeight = node.getBBox().height;
        if (!$(node).attr('widthfactor')) {
            //todo means what?we need a proper exit condition for non-stylable nodes
            if (str.indexOf("\\n") == -1) {
            splittedTexts.push(str);
            } else {
                splittedTexts = str.split("\\n");
            }
            return;
        }
        stylable = true;
        var widthFactor, heightfactor;
        if ($(node).attr('widthfactor')) {
            widthFactor = parseFloat($(node).attr('widthfactor'));
        }
        if ($(node).attr('heightfactor')) {
            heightfactor = parseFloat($(node).attr('heightfactor'));
        }
        if ($(node).attr('hJustification')) {
            hJ = parseInt($(node).attr('hJustification'), 10);
        }
        if ($(node).attr('vJustification')) {
            vJ = parseInt($(node).attr('vJustification'), 10);
        }
        if ($(node).attr('flip')) {
            flip = $(node).attr('flip');
        }
        if ($(node).attr('rot')) {
            rotation = $(node).attr('rot');
        }
        if (widthFactor === 1 && heightfactor === 1) {
            //when the text is not a bounded text, the bounding width and height will that be of the translated text
            var tempNode = addTemporaryTextNode(str, node);
            boundedWidth = tempNode.getBBox().width;
            boundedHeight = tempNode.getBBox().height;
            if (str.indexOf("\\n") == -1) {
            splittedTexts.push(str);
            } else {
                splittedTexts = str.split("\\n");
            }
        }
        else {
            boundedWidth = node.getComputedTextLength() * widthFactor;
            boundedHeight = node.getBBox().height * heightfactor;
            splittedTexts = Splitter.process(str,willTextFit);
        }
        process();
    };
    addTemporaryTextNode = function (str, node) {
        var tempNode = svgDoc.ownerDocument.createElementNS("http://www.w3.org/2000/svg", 'text');
        var child_text_node = svgDoc.ownerDocument.createTextNode(str);
        var originalClass = node.getAttribute("class");
        tempNode.setAttributeNS(null, 'class', originalClass);
        tempNode.setAttributeNS(null, 'style', 'visibility:hidden');
        tempNode.appendChild(child_text_node);
        //todo can we do without adding this to doc
        //todo check compatibility across browsers
        viewport[0].appendChild(tempNode);
        return tempNode;
    };
    doFlip = function () {
        if (hJ === _horizontalJustification.left) {
            hJ = _horizontalJustification.right;
        } else if (hJ === _horizontalJustification.right) {
            hJ = _horizontalJustification.left;
        }
        if (vJ === _verticalJustification.top) {
            vJ = _verticalJustification.bottom;
        } else if (vJ === _verticalJustification.bottom) {
            vJ = _verticalJustification.top;
        }
    };

    process = function () {
        rotation = parseInt(rotation, 10);
        if (flip === 'true') {
            if (rotation > 90 && rotation < 270) {
                toFlip = true;
                doFlip();
            }
        }
        placeText();
        placeBoundedBox();
    };

    placeText = function () {
        if (hJ === _horizontalJustification.left) {
            newTextX = textX;
            anchorAttr = 'start';
        } else if (hJ === _horizontalJustification.middle) {
            newTextX = textX + (textWidth / 2);
            anchorAttr = 'middle';
        } else if (hJ === _horizontalJustification.right) {
            newTextX = textX + textWidth;
            anchorAttr = 'end';
        }
        dy = textHeight;
    };

    placeBoundedBox = function () {
        if (hJ === _horizontalJustification.left) {
            boundingBoxX = textX;
        } else if (hJ === _horizontalJustification.right) {
            boundingBoxX = textX + textWidth - boundedWidth;
        }
        if (vJ === _verticalJustification.top) {
            boundingBoxY = textY;
        } else if (vJ === _verticalJustification.bottom) {
            boundingBoxY = textY + textHeight - boundedHeight;
        }

        if (hJ === _horizontalJustification.middle) {
            boundingBoxX = textX + (textWidth / 2) - (boundedWidth / 2);
        }
        if (vJ === _verticalJustification.center) {
            boundingBoxY = textY + (textHeight / 2) - (boundedHeight / 2);
        }
    };

    doPostProcessing = function (node) {
        if (!toFlip) {
            return;
        }
        var textWidth = node.getBBox().width, newTextX, x = $(node).attr('x');
        if (hJ === _horizontalJustification.left) {
            newTextX = x + textWidth;
            anchorAttr = 'end';
        } else if (hJ === _horizontalJustification.right) {
            newTextX = x - textWidth;
            anchorAttr = 'start';
        }
        $(node).attr('x', newTextX);
        $('tspan', node).attr('x', newTextX);
        $(node).attr('text-anchor', anchorAttr);
    };

    drawBoundingBox = function (node, viewport) {
        var rect = svgDoc.ownerDocument.createElementNS("http://www.w3.org/2000/svg", 'rect');
        rect.setAttributeNS(null, 'id', 'temprect');
        rect.setAttributeNS(null, 'x', boundingBoxX);
        rect.setAttributeNS(null, 'y', boundingBoxY);
        rect.setAttributeNS(null, 'height', boundedHeight);
        rect.setAttributeNS(null, 'width', boundedWidth);
        rect.setAttributeNS(null, 'stroke', 'red');
        rect.setAttributeNS(null, 'fill', 'none');
        rect.setAttributeNS(null, 'stroke-width', '20px');
        if ($(node).attr('transform')) {
            rect.setAttributeNS(null, 'transform', $(node).attr('transform'));
        }
        $(viewport)[0].appendChild(rect);
    };

    isNewTextOk = function(newText, oldText){
        //todo need to do this only when wrapping is enabled
        var length = newText.getComputedTextLength(), oldWidth = oldText.getComputedTextLength(), boundingWidth;
        if($(oldText).attr('widthfactor')){
            boundingWidth = oldWidth*$(oldText).attr('widthfactor');
            return length <= boundingWidth;
        }
        return true;
    };

    willTextFit=function (str) {
        var tempNode = addTemporaryTextNode(str, currentNode);
        var ret = isNewTextOk(tempNode, currentNode);
        viewport[0].removeChild(tempNode);
        return ret;
    };

    addTSpanElements = function (translatedTextStr, anExistingTextNode, valign) {
        var words = $.isArray(translatedTextStr) ? translatedTextStr : translatedTextStr.split("\\n"),
            text_node = svgDoc.ownerDocument.createTextNode("");
        var existingContent = anExistingTextNode.firstChild;
        anExistingTextNode.appendChild(text_node);
        anExistingTextNode.replaceChild(text_node, existingContent);
        try {
            var noOfWords = words.length;
            var nonEmptyWords = noOfWords;
            var firstText = null;
            for (var i = 0; i < noOfWords; i++) {
                if ("" == words[i]) {
                    nonEmptyWords--;
                    continue;
                }
                var tspan_element = svgDoc.ownerDocument.createElementNS("http://www.w3.org/2000/svg", "tspan");
                var xCoor = anExistingTextNode.getAttributeNS(null, "x");
                tspan_element.setAttributeNS(null, "x", xCoor);
                if (i == 0) {
                    firstText = tspan_element;
                }
                else {
                    tspan_element.setAttributeNS(null, "dy", dy);
                }

                var child_text_node = svgDoc.ownerDocument.createTextNode(words[i]);
                tspan_element.appendChild(child_text_node);
                anExistingTextNode.appendChild(tspan_element);

            }
            if (firstText != null) {
                if (valign === 2) {
                    firstText.setAttributeNS(null, "dy", (-1) * (dy) * (nonEmptyWords - 1));
                }
                else if (valign === 1) {
                    var even = ((nonEmptyWords%2) === 0);
                    if (even) {
                        firstText.setAttributeNS(null, "dy", (-1) * ((dy) * ((nonEmptyWords / 2)-1) +
                            (dy) / 2));
                    }
                    else {
                        firstText.setAttributeNS(null, "dy", (-1) * (dy) *
                            ((nonEmptyWords - 1) / 2));
                    }

                }
                else {
                    firstText.setAttributeNS(null, "dy", 0);
                }
            }

        }
        catch (e) {
            //write the text without replacing new line charecters.
            return addText(translatedTextStr, anExistingTextNode);
        }
        return text_node;
    };
    addText = function (translatedTextStr, anExistingTextNode) {
        var newContent = svgDoc.ownerDocument.createTextNode(translatedTextStr);
        var existingContent = anExistingTextNode.firstChild;
        anExistingTextNode.appendChild(newContent);
        anExistingTextNode.replaceChild(newContent, existingContent);
        return  newContent;
    };

    initialize();

    return {
        applyStyle:function () {
            if(!stylable){
                addText(splittedTexts[0], currentNode);
                return;
            }
            $(node).attr('x', newTextX);
            $(node).attr('text-anchor', anchorAttr);
            //drawBoundingBox(node, viewport);
            if (splittedTexts && splittedTexts.length > 1) {
                addTSpanElements(splittedTexts, currentNode,vJ);
                doPostProcessing(currentNode);
            }else if(splittedTexts && splittedTexts.length === 1){
                addText(splittedTexts[0], currentNode);
            }
        }
    };
};
var Splitter = {
    wrappedTexts:[],
    spare:'',
    lengthToTest:0,
    willTextFit:function (str) {
        return str.length <= this.lengthToTest;
    },
    //todo optimize the algorithm
    splitter:function (text) {
        var pos = 0, newText, len = text.length;
        newText = this.splitText(text, pos);
        while (!this.checker(newText)) {
            if (pos === len) {
                break;
            }
            ++pos;
            newText = this.splitText(text, pos);
        }
        if (pos === 0) {
            this.spare = newText;
        }
        else {
            this.wrappedTexts.push(newText);
            this.splitter(text.substr(len - pos, len));
        }
    },
    splitText:function (text, pos) {
        var len = text.length;
        return text.substr(0, len - pos);
    },
    splitArray:function (array, pos) {
        var len = array.length;
        return this.convertToString(array.slice(0, len - pos));
    },
    convertToString:function (arr) {
        var str = '';
        for (var k = 0; k < arr.length; k = k + 1) {
            if (str === '') {
                str = str + arr[k];
            }
            else {
                str = str + ' ' + arr[k];
            }
        }
        return str;
    },
    preComputeIfThereAreSpaces:function (str) {
        return str.split(' ');
    },
    startSplitting:function (splits) {
        var pos = 0, newText, len = splits.length, found = true;
        newText = this.splitArray(splits, pos);
        if (len === 0) {
            return;
        }
        this.spare = '';
        while (!this.checker(newText)) {
            if (pos === (len - 1)) {
                found = false;
                break;
            }
            ++pos;
            newText = this.splitArray(splits, pos);
        }
        if (found) {
            this.wrappedTexts.push(newText);
        }
        if (!found) {
            this.splitter(this.convertToString(splits.slice(0, 1)));
            var tempArray = [], c = [];
            tempArray.push(this.spare);
            if (this.spare) {
                this.startSplitting(c.concat(tempArray, splits.slice(1, len)));
            }
        }
        else {
            this.startSplitting(splits.slice(len - pos, len));
        }
    },
    checker:'',
    process:function (str, checker) {
        this.checker = checker;
        this.wrappedTexts = [];
        var splits = this.preComputeIfThereAreSpaces(str);
        this.startSplitting(splits);
        return this.wrappedTexts.filter(function (e) {
            return e
        });
    },
    start:function (str, l) {
        this.lengthToTest = l;
        var arr = this.process(str, this.willTextFit);
        for (var p = 0; p < arr.length; p = p + 1) {
            //console.log(arr[p]);
        }
        return arr;
    }
};