({
    baseUrl: './s',
    paths: {
        "Utils": "utilities/Utils",
        "TextStyler": "utilities/TextStyler",
        "OptionExpression": "OptionExpression",
        "jt3DModel": "empty:",
        "ra3DModel": "empty:",
        "routers/multipleDocumentRouter": "empty:"
    },
    include: [
        "Utils",
        "TextStyler",
        "OptionExpression",
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