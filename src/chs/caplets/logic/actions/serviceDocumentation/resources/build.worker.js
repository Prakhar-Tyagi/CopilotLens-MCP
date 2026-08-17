({
    baseUrl: './s',
    paths: {
        "filters/Search": "filters/Search",
        "filters/SearchUsingIndex": "filters/SearchUsingIndex",
        "filters/PlainSearch": "filters/PlainSearch",
        "OptionExpression": "OptionExpression",
        "utilities/ajax": "utilities/ajax",
        "views/designobjects/designObjectsDataLoader": "views/designobjects/designObjectsDataLoader",
        "views/designobjects/filteredDataLoaders.js": "views/designobjects/filteredDataLoaders.js",
    },
    include: [
        "filters/Search",
        "filters/SearchUsingIndex",
        "filters/PlainSearch",
        "OptionExpression",
        "utilities/ajax",
        "views/designobjects/designObjectsDataLoader",
        "views/designobjects/filteredDataLoaders.js",
    ],
    skipModuleInsertion: true,
    optimize: "uglify2",
    inlineText: false,
    useStrict: true,
    uglify2: {
        output: {
            beautify: false
        },
        mangle: false
    }
});