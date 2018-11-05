package org.decision_deck.utils.persist;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSink;

public class XmlWriteUtils {

	static public Document newDocument() throws XmlException {
		return XmlUtils.getBuilder().newDocument();
	}

	static public Document newDocument(Document source) {
		final Document doc2;
		try {
			doc2 = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException exc) {
			throw new IllegalStateException(exc);
		}
		Node newRoot = doc2.importNode(source.getDocumentElement(), true);
		doc2.appendChild(newRoot);
		return doc2;
	}

	static public Document putInDocument(Node node) {
		Document doc2;
		try {
			doc2 = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException exc) {
			throw new IllegalStateException(exc);
		}
		final Node sourceNode;

		if (node instanceof Document) {
			Document doc1 = (Document) node;
			sourceNode = doc1.getDocumentElement();
		} else {
			sourceNode = node;
		}
		Node newRoot = doc2.importNode(sourceNode, true);
		doc2.appendChild(newRoot);
		return doc2;
	}

	static public void write(Node contents, ByteSink destination) throws IOException {
		try (OutputStream output = destination.openBufferedStream()) {
			final StreamResult result = new StreamResult(output);
			write(contents, result);
		}
	}

	static public void write(Node contents, File outFile) throws IOException {
		final StreamResult result = new StreamResult(outFile);
		write(contents, result);
	}

	private static void write(Node contents, final StreamResult result) throws IOException {
		try {
			XmlUtils.getTransformer().transform(new DOMSource(contents), result);
		} catch (TransformerException exc) {
			throw new IOException(exc);
		}
	}

	private final XmlOptions m_saveOptions = new XmlOptions();

	private boolean m_validate;

	public XmlWriteUtils() {
		m_validate = true;
		m_saveOptions.setSavePrettyPrint();
		m_saveOptions.setCharacterEncoding(Charsets.UTF_8.name());
	}

	/**
	 * Retrieves the information whether this object only accepts to write valid
	 * documents. The default is <code>true</code>.
	 *
	 * @return <code>true</code> if this object validates documents before
	 *         writing them.
	 */
	public boolean doesValidate() {
		return m_validate;
	}

	/**
	 * Retrieves a writable view of the options used to save XML streams.
	 * Default options are to use pretty print and to use the UTF-8 encoding.
	 *
	 * @return not <code>null</code>.
	 */
	public XmlOptions getSaveOptions() {
		return m_saveOptions;
	}

	/**
	 * Enables or disables the check for validation before writing any document.
	 * The default is <code>true</code>, thus this object validates each
	 * document before returning or writing them. It is not recommanded to
	 * disable validation but it can be useful for debug.
	 *
	 * @param validate
	 *            <code>false</code> to allow invalid documents.
	 */
	public void setValidate(boolean validate) {
		m_validate = validate;
	}

	/**
	 * Writes the given XML document to the given destination. The document must
	 * be valid, except if this object is specifically set to not validate
	 * documents.
	 *
	 * @param doc
	 *            not <code>null</code>, must conform to the schema bound to the
	 *            document.
	 * @param destination
	 *            not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             writer, or while writing to the destination.
	 */
	public void write(XmlObject doc, ByteSink destination) throws IOException {
		checkNotNull(destination);
		checkNotNull(doc);
		if (m_validate) {
			checkArgument(doc.validate(), "Given document does not validate.");
		}

		try (OutputStream outputStream = destination.openBufferedStream()) {
			doc.save(outputStream, m_saveOptions);
		}
	}

}
