package org.decision_deck.utils.persist;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.xmlbeans.XmlException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.io.Closeables;

public class XmlUtils {

	static private class PedanticErrorHandler implements ErrorHandler {
		public PedanticErrorHandler() {
			/** Empty. */
		}

		@Override
		public void error(SAXParseException exception) throws SAXException {
			throw exception;
		}

		@Override
		public void fatalError(SAXParseException exception) throws SAXException {
			throw exception;
		}

		@Override
		public void warning(SAXParseException exception) throws SAXException {
			throw exception;
		}
	}

	private static final XmlUtils s_instance = new XmlUtils();
	private static final PedanticErrorHandler s_pedanticErrorHandler = new PedanticErrorHandler();

	static public DocumentBuilder getBuilder() throws XmlException {
		final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true);

		final DocumentBuilder builder;
		try {
			builder = domFactory.newDocumentBuilder();
		} catch (ParserConfigurationException exc) {
			throw new XmlException(exc);
		}
		builder.setErrorHandler(XmlUtils.s_pedanticErrorHandler);

		return builder;
	}

	static public Transformer getTransformer() {
		final Transformer newTransformer;
		try {
			newTransformer = TransformerFactory.newInstance().newTransformer();
		} catch (TransformerConfigurationException exc) {
			throw new IllegalStateException(exc);
		} catch (TransformerFactoryConfigurationError exc) {
			throw new IllegalStateException(exc);
		}
		return newTransformer;
	}

	static public XmlUtils instance() {
		return s_instance;
	}

	private final List<ByteSource> m_schemaSuppliers = Lists.newLinkedList();

	public void addSchema(final ByteSource source) {
		m_schemaSuppliers.add(source);
	}

	public DocumentBuilder getBuilderWithSchema() throws XmlException, IOException {
		final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true);

		domFactory.setSchema(getSchema());

		final DocumentBuilder builder;
		try {
			builder = domFactory.newDocumentBuilder();
		} catch (ParserConfigurationException exc) {
			throw new XmlException(exc);
		}
		builder.setErrorHandler(s_pedanticErrorHandler);
		return builder;
	}

	public Schema getSchema() throws XmlException, IOException {
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setErrorHandler(s_pedanticErrorHandler);
		final Schema schema;
		try {
			final List<InputStream> inputStreams = Lists.newLinkedList();
			try {
				for (ByteSource supplier : m_schemaSuppliers) {
					inputStreams.add(supplier.openBufferedStream());
				}
				final List<Source> sources = Lists.newLinkedList();
				for (InputStream inputStream : inputStreams) {
					sources.add(new StreamSource(inputStream));
				}
				schema = schemaFactory.newSchema(sources.toArray(new Source[sources.size()]));
				for (InputStream inputStream : inputStreams) {
					inputStream.close();
				}
			} finally {
				for (InputStream inputStream : inputStreams) {
					Closeables.closeQuietly(inputStream);
				}
			}
		} catch (SAXException exc) {
			throw new XmlException(exc);
		}
		return schema;
	}
}
