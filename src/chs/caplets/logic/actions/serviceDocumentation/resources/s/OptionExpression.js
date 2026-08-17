/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/**
 *  Filtering Module.
 *
 *  This class is used to filter viewer's data based on current options set
 *  for a VIN
 *
 * @param optionExpressions option expression for an object
 */
var OptionExpressionFilter = function (optionExpressions) {

    this.optionExpressions = optionExpressions;

    /**
     * apply option expression filter for element
     *
     * @param objectUID  schemeUID
     * @param selectedOpExpression option list
     * @param element object in SVG
     */
    this.applyOptionExpression = function (objectUID, selectedOpExpression, element) {
        /*var objectModelMap = packageModel.get('objectMap');*/
        // var isObjectActive = objectModelMap[objectUID] == null ? true : objectModelMap[objectUID];
        var isActive = mentor.publisher.dataLoader.objectMap[objectUID] == null ? true :
            (isObjectActive(mentor.publisher.dataLoader.objectMap[objectUID], selectedOpExpression));
        if (!isActive) {
            //$(element).attr("style", "display:none");
            //switch off the element
            $(element).remove();
        }

    };

    this.evaluteOptionsAgainstOptionExpressions = function (objectOpExpressions, selectedOptions) {
        var i, isValid = false;
        if (objectOpExpressions instanceof Array) {
            for (i = 0; i < objectOpExpressions.length; i = i + 1) {
                isValid = this.optionExpressionEvaluation(objectOpExpressions[i], selectedOptions);
                if (isValid) {
                    break;
                }
            }

        } else {
            isValid = this.optionExpressionEvaluation(objectOpExpressions, selectedOptions);
        }
        return isValid;

    };

    /**
     * object options expression gets evaluated against selected options
     *
     * @param objectOpExpression
     * @param selectedOptions
     */
    this.optionExpressionEvaluation = function (objectOpExpression, selectedOptions) {

        if (objectOpExpression === null || "" === objectOpExpression.trim() || !selectedOptions ||
            "" === selectedOptions.trim()) {
            /**
             *object has no option expression, therefor it should be visible
             */
            return true;

        }
        /**
         * replcaeall &amp; to &
         */
        objectOpExpression = objectOpExpression.replace(/amp;/g, "");

        /**
         * parse option expression
         */
        var objectOptions = this.parse(objectOpExpression.trim());

        /**
         * iterates through all the options,
         * and if option is present in optionExpression then it is replaced by true value other wise false is written
         * for it in the expression
         */
        objectOptions = this.getOptionsWithTrueSubstituted(objectOptions, selectedOptions);
        /*for (var count = 0; count < vinOptions.length; count++) {
         //START ---> Its a costly operation-->if VIn has 50 options and (In general options.xml has 30k objects and if every opject has
         //10 param then the iterations will be 50x20 (for each object in options.xml) We can use normal String replace
         //Again in next for loop from line 142 u will be parsing again

         var option = vinOptions[count].trim();
         if (option.trim() == "") {
         continue;
         }
         var patternStr = new RegExp(option,'gi');
         objectOptions = objectOptions.replace(patternStr, "true");//this.matchOptionAgainstOpExp(objectOptions, option);
         }*/
        /**
         * now again parse it and for elemnts which are not found in selected options,
         * write false for them
         */
        var objectExpression = objectOptions.split("#");
        var evalExpression = '';
        for (var i = 0; i < objectExpression.length; i++) {
            var subExpress = objectExpression[i].trim();
            if (subExpress != "&&" && subExpress != "||" &&
                subExpress != "(" && subExpress != ")" && subExpress != 'true' && subExpress != '!' &&
                subExpress != "") {
                evalExpression = evalExpression + false;
            }
            else {
                evalExpression = evalExpression + objectExpression[i];
            }

        }

        /**
         * here we have a expression with operators as || , && or ! and operands are true and false only.
         * now we can evaluate it
         */
        if (!validateEvalExpression(evalExpression)) {
            return false;

        }
        return eval(evalExpression);
    };

    function validateEvalExpression(evalExpression){
        
        // Checks if the expression contains only valid characters (true, false, &&, ||, !, (,), " " ) and is not empty.
        var validExpressionRegex=/^(\s*(true|false|&&|\|\||!|\(|\))\s*)+$/;
        if(evalExpression==null || evalExpression.trim()=="" || !validExpressionRegex.test(evalExpression)){
            return false;
        }
        
        //Checks for balanced parentheses in the expression
        if(!hasBalancedParentheses(evalExpression)){
            return false;
        }

        return true;
    }

    function hasBalancedParentheses(expression) {
        let count = 0;
    
        for (let char of expression) {
            if (char === '(') {
                count++;
            } else if (char === ')') {
                count--;
                if (count < 0) {
                    return false;
                }
            }
        }
    
    return count === 0;
}

    this.getOptionsWithTrueSubstituted = function (objOpExpression, selectedOptions) {
        var vinOptions = selectedOptions.split(',');

        if (vinOptions.length > (objOpExpression.split('#')).length) {
            var leadingAndTrailingCommAdded = ',' + selectedOptions + ',';
            leadingAndTrailingCommAdded = leadingAndTrailingCommAdded.replace(/\s*[,]\s*/gi, ",");//All the space leading and trailing comma will be trimmed
            //\s for space and * for 0 or more occurence
            var objectExpArray = objOpExpression.split('#');

            var resultedExpression = '';

            for (var i = 0; i < objectExpArray.length; i++) {
                if (resultedExpression != '') {
                    resultedExpression += '#';
                }
                //if this option is found in selected option set then replace it with true value
                var found = leadingAndTrailingCommAdded.indexOf("," + objectExpArray[i].trim() + ",");
                if (found > -1) {
                    resultedExpression = resultedExpression + 'true';
                }
                else {
                    resultedExpression = resultedExpression + objectExpArray[i];
                }
            }
            objOpExpression = resultedExpression;
        }
        else {
            objOpExpression = '#' + objOpExpression + '#';//Adding leading and and trailing #, thsi will be removed after this else executed
            for (var count = 0; count < vinOptions.length; count++) {
                //START ---> Its a costly operation-->if VIn has 50 options and (In general options.xml has 30k objects and if every opject has
                //10 param then the iterations will be 50x20 (for each object in options.xml) We can use normal String replace
                //Again in next for loop from line 142 u will be parsing again

                var option = vinOptions[count].trim();
                if (option.trim() == "") {
                    continue;
                }
                var patternStr = new RegExp('#' + option + '#', 'gi');
                objOpExpression = objOpExpression.replace(patternStr, "#true#");//this.matchOptionAgainstOpExp(objectOptions, option);
            }
            objOpExpression = objOpExpression.substring(1, objOpExpression.length - 1);//removed the leading and trailing # added at else starting
        }
        return objOpExpression;
    };
    /**
     * method parse option expression to add demarcation between operands and operators, "#" is used as a separator here.
     * so the option expression  op1 && (!op2 && op3) is convereted into op1#&&#(#&!#op2#&&op3#)#
     *
     * @param optionexpression option expression
     */
    this.parse = function (optionexpression) {
        if (optionexpression.match(/.*&&.*/)) {
            var logicalAndexp = optionexpression.split('&&');
            if (logicalAndexp.length > 1) {
                return this.parseSubExpressions(logicalAndexp, '&&');
            }
        }
        if (optionexpression.match(/.*\|\|.*/)) {
            var logicalORExpressions = optionexpression.split('||');
            if (logicalORExpressions.length > 1) {
                return this.parseSubExpressions(logicalORExpressions, '||');
            }
        }

        if (optionexpression.match(/.*!.*/)) {
            var logicalNOTExpressions = optionexpression.split('!');

            if (logicalNOTExpressions.length > 1) {
                return this.parseSubExpressions(logicalNOTExpressions, '!');
            }
        }

        if (optionexpression.match(/.*\(.*/)) {
            var leftBracketExpressions = optionexpression.split('(');

            if (leftBracketExpressions.length >= 1) {
                return  this.parseSubExpressions(leftBracketExpressions, '(');
            }
        }

        if (optionexpression.match(/.*\).*/)) {
            var rightBracketExpressions = optionexpression.split(')');
            if (rightBracketExpressions.length > 1) {
                return this.parseSubExpressions(rightBracketExpressions, ')');
            }
        }

        return   optionexpression;
    };

    /**
     * breaks the option expression into two parts and
     * then parse each part separately
     *
     * @param logicalAndexp sub option expression
     *
     * @param operator operator
     */
    this.parseSubExpressions = function (logicalAndexp, operator) {
        var processedExpression = null;
        if (logicalAndexp.length == 1 && operator == "!") {
            return (operator + "#" + logicalAndexp[0]).trim();
        }
        for (var i = 0; i < logicalAndexp.length; i++) {
            if (processedExpression == null) {
                processedExpression = this.parse(logicalAndexp[i].trim());
            }
            else {
                processedExpression = processedExpression + '#' + operator + '#' + this.parse(logicalAndexp[i].trim());
            }
        }
        return processedExpression.trim();
    };

    this.createOptionExpressionMap = function () {
        if (!mentor.publisher.dataLoader.objectMap) {
            mentor.publisher.dataLoader.createOptionExpressionMap();
        }
    };

    this.filterSVG = function (svgRootElement, options) {
        var optionFilterInstance = this;
        this.createOptionExpressionMap();
        $("g", svgRootElement).each(function () {
            //get its desc
            $("desc", this).each(function () {
                var descElement = $(this).text();
                if (descElement.match(/.*UID.*/)) {
                    //parse the desc information to get option expression
                    var descValue = descElement.split(' ');
                    //descValue[1] is schemUID
                    optionFilterInstance.applyOptionExpression(descValue[1], options, $(this).parent());
                }
            });
        });



    }
};

/**
 * SVG filtering starts here
 *
 * @param svgRootElement container ID where SVG is loaded
 */
function doFilter(svgRootElement, selectedOptions) {
    var optionExpressionFilter = new OptionExpressionFilter();
    optionExpressionFilter.filterSVG(svgRootElement, selectedOptions);
}

/**
 * This function will read options.xml file.
 * It will filter all the UID's with passed options set and
 * build a MAP: wherne key=object UID and value= true/false,
 * This map can be said as ObjectUID activeness map
 *
 * the map will be added to packageModel
 * and will be used to filter objects based of option expressions
 */
function createObjectUIDToOptionExpressionMap(id) {
    return populateActiveObjectMap("options", id);
}

function populateActiveObjectMap(fileName, id) {
    var objectMap = {};
    //console.log("load opbject UID to op exp map ");
    var optionExpressionFile = Utils.prepareFilePath(id + '/' + fileName.trim() +
        ".xml");
    $.ajax({ url : optionExpressionFile,
        success : function (data, textStatus, XMLHttpRequest) {
            $("object", data).each(function () {
                var diagramId = $(this).attr("diagramId");
                objectMap[$(this).attr('id')] = $(this).text() + "";
            });

        }, error : function (XMLHttpRequest, textStatus, errorThrown) {
        }, dataType : (Utils.is_msie()) ? "text" : "xml", async : false});
    return objectMap;

}

function isObjectActive(objectOptionExpression, selectedOptions) {

    if (typeof(objectOptionExpression) == "undefined" || objectOptionExpression == null ||
        objectOptionExpression.trim() == "" || objectOptionExpression.trim() == "#true#") {
        return true;
    }

    if (selectedOptions == '') {
        return true;
    }
    var optionFilter = new OptionExpressionFilter();
    return  optionFilter.optionExpressionEvaluation(objectOptionExpression, selectedOptions);
}

function getActiveConfigurationsForSystemWithOptions(systemOptionExpression, activeconfigs) {
    var obj = new Array();
    for (var index = 0; index < activeconfigs.length; index++) {
        var configuration = activeconfigs[index];
        var selectedOptions = configuration.value;
        var isActive = isObjectActive(systemOptionExpression, selectedOptions);
        if (isActive) {
            obj.push(configuration);
        }
    }
    return obj;
}

/*
 This function is used basically for Harness and System level reports, The fault code reports are not handled through this system. we ahve separate FaultCodeSearch.js
 for fault reports.
 */
function isReportElementActive(optionExpression) {
    var isPopout = window.opener && window.opener.mentor, options = mentor.publisher.filter.vinOptions;
    if (isPopout) {
        options = window.opener.mentor.publisher.filter.vinOptions
    }

    return isObjectActive(optionExpression, options);
}




