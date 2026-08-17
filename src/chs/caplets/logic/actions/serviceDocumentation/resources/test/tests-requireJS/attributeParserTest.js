/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

describe("attributeParserTest", function ()
{

    it("should be able to parse attributes correctly", function ()
    {
        var actualAttribute, attrParser, testdata = {name: 'attrName', value: 'attrVal'};
        attrParser = mentor.publisher.attributeParser(
                function (attr, node)
                {
                    return {
                        text: function ()
                        {
                            return testdata[attr];
                        }
                    }
                }
        );

        attrParser({}, {
            translator: {
                translateQuickCode: function (text)
                {
                    return text + "_translated";
                }
            },
            callback: function (attr)
            {
                actualAttribute = attr;
            }
        });
        expect(actualAttribute.name).toBe("attrName_translated");
        expect(actualAttribute.value).toBe("attrVal_translated");
    });
});