package org.decision_deck.utils.persist;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import com.google.common.io.Closeables;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;

public class XmlReadUtils {

	private static final Logger s_logger = LoggerFactory.getLogger(XmlReadUtils.class);

	/**
	 * Retrieves the "DOM" document corresponding to the given xml source
	 * document. The document is namespace aware.
	 *
	 * @param source
	 *            not <code>null</code>, must correspond to an xml document.
	 * @return not <code>null</code>.
	 * @throws IOException
	 *             if an IO error occurs.
	 * @throws XmlException
	 *             if a parse error occurs.
	 */
	static public Document getAsDom(ByteSource source) throws IOException, XmlException {
		checkNotNull(source);

		final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true);
		final Document domSource;
		try (InputStream input = source.openBufferedStream()) {
			final InputSource is = new InputSource(input);
			final DocumentBuilder newDocumentBuilder = domFactory.newDocumentBuilder();
			domSource = newDocumentBuilder.parse(is);
		} catch (SAXException exc) {
			throw new XmlException(exc);
		} catch (ParserConfigurationException exc) {
			throw new XmlException(exc);
		}
		return domSource;
	}

	/**
	 * <p>
	 * Retrieves the node which is a direct child of the given document root
	 * element; and has the given qName. Puts the contents of the said node into
	 * a document fragment. The node itself is not put into the document
	 * fragment, only its contents. Namespace and local part of the qname must
	 * be set. The prefix part may be used for debugging only, as it plays no
	 * role; any prefix, including the default prefix, may be used. Using a
	 * prefix which exists in the given document and is bound to a different
	 * namespace is not a problem.
	 * </p>
	 * <p>
	 * The node found is <em>removed</em> from the given source document.
	 * </p>
	 * <p>
	 * Use this method when you know that the document contains a corresponding
	 * node, typically when the document is validated against a schema known to
	 * contain this node.
	 * </p>
	 *
	 * @param sourceDocument
	 *            not <code>null</code>.
	 * @param qName
	 *            not <code>null</code>, not a <code>null</code> namespace or
	 *            local part.
	 * @return not <code>null</code>.
	 * @throws XmlException
	 *             if the given node can't be found.
	 */
	static public DocumentFragment getChildNode(Document sourceDocument, final QName qName) throws XmlException {
		checkArgument(qName.getNamespaceURI() != null);
		checkArgument(qName.getLocalPart() != null);

		final Map<String, String> namespaces = Maps.newHashMap();
		/** This works even with a default (empty) prefix. */
		final String prefix = qName.getPrefix();
		namespaces.put(prefix, qName.getNamespaceURI());
		/** Search only in nodes that are direct childs of the root node. */
		// final XPathExpression search =
		// XPathExpressionFactory.createXPathExpression(
		// "/node()/" + prefix + ":" + qName.getLocalPart(), namespaces);
		/** Search from root in the whole tree. */
		final XPathExpression search = XPathExpressionFactory
				.createXPathExpression("//" + prefix + ":" + qName.getLocalPart(), namespaces);
		final List<Node> foundNodes = search.evaluateAsNodeList(sourceDocument);
		if (foundNodes.size() == 0) {
			throw new XmlException("Couldn't find node: " + qName + ".");
		}
		if (foundNodes.size() > 1) {
			throw new XmlException("Found more than one matching node: " + qName + ".");
		}
		final Node childNode = Iterables.getOnlyElement(foundNodes);
		s_logger.debug("Found child node contents {}.", XmlReadUtils.toString(childNode));

		DocumentFragment fragment = sourceDocument.createDocumentFragment();

		while (childNode.hasChildNodes()) {
			fragment.appendChild(childNode.getFirstChild());
		}
		s_logger.debug("Returning fragment {}.", XmlReadUtils.toString(fragment));
		return fragment;
	}

	static public Map<String, String> getChildsTextContents(final Node parent) {
		final Map<String, String> results = Maps.newLinkedHashMap();
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			results.put(child.getLocalName(), child.getTextContent());
		}
		return results;
	}

	/**
	 * Retrieves the namespace associated to the document root of the given xml
	 * source document.
	 *
	 * @param source
	 *            not <code>null</code>, must correspond to an xml document.
	 * @return the namespace read, possibly <code>null</code> if none has been
	 *         found.
	 * @throws IOException
	 *             if an IO error occurs.
	 * @throws XmlException
	 *             if a parse error occurs.
	 */
	static public String getNamespace(ByteSource source) throws IOException, XmlException {
		checkNotNull(source);

		final Document domSource = getAsDom(source);
		final String sourceNamespace = domSource.getDocumentElement().getNamespaceURI();
		return sourceNamespace;
	}

	/**
	 * Retrieves the namespace associated to the document root of the given xml
	 * source document.
	 *
	 * @param source
	 *            not <code>null</code>, must correspond to an xml document.
	 * @return the namespace read, possibly <code>null</code> if none has been
	 *         found.
	 * @throws IOException
	 *             if an IO error occurs.
	 * @throws XmlException
	 *             if a parse error occurs.
	 */
	static public String getNamespace_Reader(InputSupplier<? extends Reader> source) throws IOException, XmlException {
		checkNotNull(source);

		final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true);
		final Document domSource;
		final Reader input = source.getInput();
		try {
			domSource = domFactory.newDocumentBuilder().parse(new InputSource(input));
			input.close();
		} catch (SAXException exc) {
			throw new XmlException(exc);
		} catch (ParserConfigurationException exc) {
			throw new XmlException(exc);
		} finally {
			Closeables.closeQuietly(input);
		}
		final String sourceNamespace = domSource.getDocumentElement().getNamespaceURI();
		return sourceNamespace;
	}

	static public Node readNode(final ByteSource source) throws IOException, XmlException {
		try (InputStream input = source.openBufferedStream()) {
			final DOMResult result = new DOMResult();
			try {
				TransformerFactory.newInstance().newTransformer().transform(new StreamSource(input), result);
			} catch (TransformerConfigurationException exc) {
				throw new XmlException(exc);
			} catch (TransformerException exc) {
				throw new XmlException(exc);
			} catch (TransformerFactoryConfigurationError exc) {
				throw new XmlException(exc);
			}
			return result.getNode();
		}
	}

	static public Node readNode(URL url) throws XmlException, IOException {
		return readNode(Resources.asByteSource(url));
	}

	static public String toString(Node node) {
		final DOMSource source;
		source = new DOMSource(node);
		final StringWriter writer = new StringWriter();
		final StreamResult result = new StreamResult(writer);
		try {
			XmlUtils.getTransformer().transform(source, result);
		} catch (TransformerException exc) {
			/**
			 * Might be because of lack of DOM level 3 support (e.g. XmlBeans
			 * nodes).
			 */
			s_logger.warn("First transformation attempt failed, re-trying with a new implementation.");
			final DOMSource sourceCopied = new DOMSource(XmlWriteUtils.putInDocument(node));
			try {
				XmlUtils.getTransformer().transform(sourceCopied, result);
			} catch (TransformerException exc2) {
				s_logger.error(
						"Exception again while trying transform with a new implementation. Throwing original exception. Second exception logged.",
						exc2);
				throw new IllegalStateException(exc);
			}
		}
		return writer.toString();
	}

}
