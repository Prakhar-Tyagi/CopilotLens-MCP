/**
 * Created with IntelliJ IDEA.
 * User: kayyagar
 * Date: 06/11/12
 * Time: 8:01 PM
 * To change this template use File | Settings | File Templates.
 */
describe("textSplitterTest", function() {
    beforeEach(function() {
        var svg = $('<svg data-translation="marker-based" height="100%" width="100%" style="cursor: default;"> <g id="viewport" transform="matrix(0.018257280811667442,0,0,0.018257280811667442,66.34995380473265,215.20794717879107)">' +
                '<text xmlns="http://www.w3.org/2000/svg" class="B1 translatable" x="51112.4453125" y="1806" widthfactor="1.0976673181676246" heightfactor="1.68" rot="0" hJustification="1" vJustification="1" flip="true" data-original="3C Y55X ENG. MANAGAMENT DIESEL VED5 SHEET2 {AD}" text-anchor="middle"><tspan x="51112.4453125" dy="-356.03125" style="cursor: default;">{testCode}</tspan><tspan x="51112.4453125" dy="712.0625">Design</tspan></text>' +
                '<g id="desc" widthfactor="1" heightfactor="1" flip="true"><desc>chs.cof.logical.schem.CAFShieldBody UID25b55b-1316adac60b-9e788023cd93580da11edf9eaf55278e UID25b55b-1316adac60a-9e788023cd93580da11edf9eaf55278e UID25b55b-1316adac609-9e788023cd93580da11edf9eaf55278e</desc><path class="C" d="M10534,11354v-1579"/><path class="C" d="M10534,7262 C10362,7262 10223,7401 10223,7573 "/><path class="C" d="M10845,7573 C10845,7401 10706,7262 10534,7262 "/><path class="C" d="M10223,9464 C10223,9635 10362,9775 10534,9775 "/><path class="C" d="M10534,9775 C10706,9775 10845,9635 10845,9464 "/><path class="C" d="M10223,7573v1891"/><path class="C" d="M10845,7573v1891"/></g></g>');
        $('body').html(svg);
    });

    afterEach(function () {
        $('body>svg').remove();
    });

    it("test Text splitter for text with no spaces and insufficient width.", function() {
        var val = Splitter.start('abcd', 3);
        // assertEquals("Text splitter for text with no spaces and insufficient width failed.", true,
        //         (val[0] === 'abc' && val[1] === 'd' && val.length === 2));
        expect(val.length).toBe(2);
        expect(val[0].trim()).toBe('abc');
        expect(val[1].trim()).toBe('d');
    });

    it("test Text splitter for text with spaces and insufficient width.", function() {
        var val = Splitter.start('abcd e', 4);
        // assertEquals("test Text splitter for text with spaces and insufficient width failed.", true,
        //         (val[0] === 'abcd' && val[1] === 'e' && val.length === 2));
        expect(val.length).toBe(2);
        expect(val[0].trim()).toBe('abcd');
        expect(val[1].trim()).toBe('e');
    });

    it("test Text splitter for text with spaces and sufficient width.", function() {
        var val = Splitter.start('abcd e', 6);
        // assertEquals("test Text splitter for text with spaces and sufficient width failed.", true,
        //         (val[0] === 'abcd e' && val.length === 1));
        expect(val.length).toBe(1);
        expect(val[0].trim()).toBe('abcd e');
    });

    it("test Text splitter for text with spaces and insufficient width and intermediate split and combo.", function() {
        var val = Splitter.start('abcd e', 3);
        // assertEquals("test Text splitter for text with spaces and insufficient width and intermediate split and combo failed.",
        //         true, (val[0] === 'abc' && val[1] === 'd e' && val.length === 2));
        expect(val.length).toBe(2);
        expect(val[0].trim()).toBe('abc');
        expect(val[1].trim()).toBe('d e');
    });

    it("test Text splitter for text with spaces and insufficient width and intermediate split and separate.", function() {
        var val = Splitter.start('aaaa bb c', 3);
        // assertEquals("test Text splitter for text with spaces and insufficient width and intermediate split and separate failed.",
        //         true,
        //         (val[0] === 'aaa' && val[1] === 'a' && val[2].trim() === 'bb' && val[3] === 'c' && val.length === 4));
        expect(val.length).toBe(4);
        expect(val[0].trim()).toBe('aaa');
        expect(val[1].trim()).toBe('a');
        expect(val[2].trim()).toBe('bb');
        expect(val[3].trim()).toBe('c');
    });

    it("test Text splitter for text with spaces and insufficient width and no intermediate split and combo.", function() {
        var val = Splitter.start('a bb ccc d', 4);
        // assertEquals("test Text splitter for text with spaces and insufficient width and no intermediate split and combo failed.",
        //         true,
        //         (val[0] === 'a bb' && val[1].trim() === 'ccc' && val[2].trim().trim() === 'd' && val.length === 3));
        expect(val.length).toBe(3);
        expect(val[0].trim()).toBe('a bb');
        expect(val[1].trim()).toBe('ccc');
        expect(val[2].trim()).toBe('d');
    });

    it("test Text splitter for text with spaces and insufficient width and intermediate split and no combo.", function() {
        var val = Splitter.start('puntoo di conn', 4);
        // assertEquals("test Text splitter for text with spaces and insufficient width and intermediate split and no combo failed.",
        //         true, (val[0] === 'punt' && val[1].trim() === 'oo' && val[2].trim() === 'di' && val[3] === 'conn' &&
        //                 val.length === 4));
        // expect(val[0] === 'punt' && val[1].trim() === 'oo' && val[2].trim() === 'di' && val[3] === 'conn' &&
        //         val.length === 4).toBe(true);
        expect(val.length).toBe(4);
        expect(val[0].trim()).toBe('punt');
        expect(val[1].trim()).toBe('oo');
        expect(val[2].trim()).toBe('di');
        expect(val[3].trim()).toBe('conn');
    });

    it("test TranslationUtils is able to translate text on SVG document", function() {
        var translationUtils = new TranslationUtils($), preTranslate = Utils.translate;
        Utils.translate = function ()
        {
            return "translated Value";
        }
        translationUtils.translateSVGContent($('body>svg'), true);
        waitsFor(function () {
            return $('.translatable').text()!=='';
        }, "wait for translatable text to update", 10);
        expect($('.translatable').text()).toBe("translated ValueDesign");
        Utils.translate = preTranslate;
    });

});