/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caf.cafmain.actions.capture.sysml;

import com.mentor.capital.xml.SAXParserService;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;

/**
 * Validates XML file against the given XSD schema file.
 */
public class XMLConfigSchemaValidator
{

	@NotNull private final File xsdFile;
	@NotNull private final File configFile;
	@NotNull private final IXMLValidationErrorHandler validationErrorHandler;
	@NotNull private final ErrorHandler parserErrorHandler;

	public XMLConfigSchemaValidator(@NotNull File xsdFile, @NotNull File configFile,
			@NotNull IXMLValidationErrorHandler validationErrorHandler, @NotNull ErrorHandler parserErrorHandler)
	{
		this.xsdFile = xsdFile;
		this.configFile = configFile;
		this.validationErrorHandler = validationErrorHandler;
		this.parserErrorHandler = parserErrorHandler;
	}

	public boolean validate()
	{
		if (!xsdFile.exists()) {
			validationErrorHandler.handleMissingXSD();
			return false;
		}
		try {
			SAXParserFactory factory = SAXParserService.INSTANCE.newSAXParserFactoryXXEAndExternalTDDisabled();
			factory.setNamespaceAware(true);
			factory.setValidating(false);
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			try {
				factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
				factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
				factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			}
			catch (SAXNotRecognizedException e) {
				// Properties not supported by this parser implementation - silently ignore
			}

			try {
				schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
				schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			}
			catch (IllegalArgumentException | SAXNotRecognizedException | SAXNotSupportedException e) {
				// Not supported by this implementation-fail safe
			}
			factory.setSchema(schemaFactory.newSchema(xsdFile));
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			reader.setErrorHandler(parserErrorHandler);
			reader.parse(new InputSource(configFile.getPath()));
		}
		catch (SAXNotRecognizedException | SAXNotSupportedException | ParserConfigurationException exception) {
			validationErrorHandler.handleValidationFailure(exception);
			return false;
		}
		catch (IOException | SAXException exception) {
			validationErrorHandler.handleValidationFailure(exception);
			return false;
		}
		return true;
	}
}
