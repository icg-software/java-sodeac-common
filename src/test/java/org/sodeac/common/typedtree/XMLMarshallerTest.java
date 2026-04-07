package org.sodeac.common.typedtree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.sodeac.common.model.CoreTreeModel;
import org.sodeac.common.model.ThrowableNodeType;
import org.sodeac.common.typedtree.ModelPath.ModelPathBuilder;
import org.sodeac.common.typedtree.TypedTreeMetaModel.RootBranchNode;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class XMLMarshallerTest
{
	@Test
	public void test0001() throws IOException, XMLStreamException, FactoryConfigurationError
	{
		Exception exc = new Exception("outerexception", new Exception("innerexception"));
		RootBranchNode<CoreTreeModel, ThrowableNodeType> exceptionNode = ThrowableNodeType.nodeFromThrowable(exc);
		XMLMarshaller marshaller = ModelRegistry.getTypedTreeMetaModel(CoreTreeModel.class).getXMLMarshaller();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		marshaller.marshal(exceptionNode, baos, true);
		
		String xml1 = baos.toString();
		
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		RootBranchNode<CoreTreeModel, ThrowableNodeType> exceptionNode2 = ModelRegistry.getTypedTreeMetaModel(CoreTreeModel.class).createRootNode(CoreTreeModel.throwable);
		marshaller.unmarshal(exceptionNode2, bais, true);
		
		baos = new ByteArrayOutputStream();
		marshaller.marshal(exceptionNode2, baos, true);
		
		String xml2 = baos.toString();
		
		assertEquals("value should be correct",xml1, xml2);
	}
}
