/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global require, describe, it, expect, beforeEach, Backbone, afterEach, createContext*/
describe('SubPackageCollectionTest', function(){
    var CollectionUnderTest,
    subPackages;

    var subpackage1 = {
        name: 'packet1',
        prefix: 'z',
        start: '1',
        description: ''
    },
    subPackage2 = {
        name: 'another',
        prefix: '',
        start: '2',
        description: ''
    },
    subPackage3 = {
        name: 'beta',
        prefix: 'a',
        start: '1',
        description: ''
    },
    subPackage4 = {
        name: 'beta',
        prefix: 'f',
        start: '200',
        description: ''
    },
    subPackage5 = {
        name: 'beta',
        prefix: 'f',
        start: '20',
        description: ''
    };

    subPackages = [subpackage1, subPackage2, subPackage3, subPackage4, subPackage5];

    require(['collections/SubPackages'], function (SubPackages) {
        CollectionUnderTest = SubPackages;
    });

    beforeEach(function(){

    });

    afterEach(function(){

    });

    it('should load the collection',  function(){
        var collection = new CollectionUnderTest(subPackages);
        expect(collection).toBeDefined();
    });

    it('should sort the collection(subPackages) as defined by Comparator',  function(){
        var collection = new CollectionUnderTest(subPackages);
        expect(collection.models.length).toBe(5);
        // Sort by Name, then prefix, then start value
        var expectedSortedOrder = [subPackage2, subPackage3, subPackage5, subPackage4,subpackage1];
        expect(JSON.stringify(collection.models)).toBe(JSON.stringify(expectedSortedOrder));
    });

    it('should parse the xml data to return related subpackages', function(){
        var xmlData = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n' +
                '<packages>\n' +
                '    <package id="data\\9328" name="packet1" description="eff1 description1" projectId="test-project">        \n' +
                '        <subpackage id="data\\9328" type="effectivity" description="" range="f1-f10" prefix="f" start="1" end="10"/>\n' +
                '        <subpackage id="data\\a0fe" type="effectivity" description="" range="f11-f15" prefix="f" start="11" end="15"/>' +
                '        <subpackage id="data\\0a3f" type="effectivity" description="" range="f16-f20" prefix="f" start="16" end="20"/>\n' +
                '        <subpackage id="data\\0369" type="effectivity" description="" range="f21-f40" prefix="f" start="21" end="40"/>' +
                '        <subpackage id="data\\f4a9" type="effectivity" description="" range="f41-f60" prefix="f" start="41" end="60"/>\n' +
                '        <subpackage id="data\\20f5" type="effectivity" description="" range="f61-f75" prefix="f" start="61" end="75"/>\n' +
                '        <subpackage id="data\\8c75" type="effectivity" description="" range="f76-f90" prefix="f" start="76" end="90"/>\n' +
                '        <subpackage id="data\\d131" type="effectivity" description="" range="f91-f135" prefix="f" start="91" end="135"/>\n' +
                '        <subpackage id="data\\9328" type="effectivity" description="" range="f1-f100" prefix="f" start="1" end="100"/>\n' +
                '        <subpackage id="data\\d131" type="effectivity" description="" range="f136-f200" prefix="f" start="136" end="200"/>\n' +
                '    </package>\n' +
                '   <package id="data\\9328" name="test-packet" description="eff1 description2" projectId="UID248155-16242bdbe5e-1c7b7572353af8ade7bbb7e98a4eb4f1">' +
                '        <subpackage id="data\\9328" type="effectivity" description="" range="z1-z10" prefix="z" start="1" end="10"/>\n' +
                '        <subpackage id="data\\a0fe" type="effectivity" description="" range="z11-z15" prefix="z" start="11" end="15"/>' +
                '        <subpackage id="data\\0a3f" type="effectivity" description="" range="z16-z20" prefix="z" start="16" end="20"/>' +
                '        <subpackage id="data\\0369" type="effectivity" description="" range="z21-z40" prefix="z" start="21" end="40"/>' +
                '        <subpackage id="data\\f4a9" type="effectivity" description="" range="z41-z60" prefix="z" start="41" end="60"/>\n' +
                '        <subpackage id="data\\20f5" type="effectivity" description="" range="z61-z75" prefix="z" start="61" end="75"/>\n' +
                '        <subpackage id="data\\8c75" type="effectivity" description="" range="z76-z90" prefix="z" start="76" end="90"/>\n' +
                '        <subpackage id="data\\d131" type="effectivity" description="" range="z91-z135" prefix="z" start="91" end="135"/>\n' +
                '        <subpackage id="data\\9328" type="effectivity" description="" range="z1-z100" prefix="z" start="1" end="100"/>\n' +
                '        <subpackage id="data\\d131" type="effectivity" description="" range="z136-z200" prefix="z" start="136" end="200"/>\n' +
                '    </package>\n' +
                '\t<package id="data\\9328" name="test_packet" description="eff1 descriptionx" projectId="UID248155-16242bdbe5e-1c7b7572353af8ade7bbb7e98a4eb4f1">' +
                '        <subpackage id="data\\9328" type="effectivity" description="" range="BL1-BL10" prefix="BL" start="1" end="10"/>\n' +
                '        <subpackage id="data\\a0fe" type="effectivity" description="" range="BL11-BL15" prefix="BL" start="11" end="15"/>' +
                '        <subpackage id="data\\0a3f" type="effectivity" description="" range="BL16-BL20" prefix="BL" start="16" end="20"/>\n' +
                '        <subpackage id="data\\0369" type="effectivity" description="" range="BL21-BL40" prefix="BL" start="21" end="40"/>' +
                '        <subpackage id="data\\f4a9" type="effectivity" description="" range="BL41-BL60" prefix="BL" start="41" end="60"/>\n' +
                '        <subpackage id="data\\20f5" type="effectivity" description="" range="BL61-BL75" prefix="BL" start="61" end="75"/>\n' +
                '        <subpackage id="data\\8c75" type="effectivity" description="" range="BL76-BL90" prefix="BL" start="76" end="90"/>\n' +
                '        <subpackage id="data\\d131" type="effectivity" description="" range="BL91-BL135" prefix="BL" start="91" end="135"/>\n' +
                '        <subpackage id="data\\9328" type="effectivity" description="" range="BL1-BL100" prefix="BL" start="1" end="100"/>\n' +
                '        <subpackage id="data\\d131" type="effectivity" description="" range="BL136-BL200" prefix="BL" start="136" end="200"/>\n' +
                '    </package>\n' +
                '</packages>';

        var collection = new CollectionUnderTest(subPackages);
        collection.projectId = 'test-project';
        var resultSubPackages = collection.parse(xmlData, {projectId: "test-project"});

        expect(resultSubPackages.length).toBe(10);
        expect(_.every(resultSubPackages, function(subPackage){
            return subPackage.prefix === 'f';
        })).toBeTruthy();
    });

    it('should parse the &nbsp and \\n', function() {
        var xmlData = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n' +
                '<packages>\n' +
                '    <package id="data\\9328" name="packet1" description="Description with &amp;nbsp; and \\n" projectId="test-project">        \n' +
                '        <subpackage id="data\\9328" type="effectivity" description="" range="f1-f10" prefix="f" start="1" end="10"/>\n' +
                '    </package>\n' +
                '</packages>';

        var collection = new CollectionUnderTest(subPackages);
        collection.projectId = 'test-project';
        var result = collection.parse(xmlData, {projectId: "test-project"});
        expect(result[0].description).toBe('Description with      and \n');
    });

});