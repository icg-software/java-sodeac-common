/*******************************************************************************
 * Copyright (c) 2019, 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.typedtree;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.sodeac.common.annotation.Version;
import org.sodeac.common.misc.StringConverter;
import org.sodeac.common.typedtree.annotation.Domain;
import org.sodeac.common.typedtree.annotation.IgnoreIfEmpty;
import org.sodeac.common.typedtree.annotation.IgnoreIfFalse;
import org.sodeac.common.typedtree.annotation.IgnoreIfNull;
import org.sodeac.common.typedtree.annotation.IgnoreIfTrue;
import org.sodeac.common.typedtree.annotation.Transient;
import org.sodeac.common.typedtree.annotation.XMLNodeList;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

// quick and dirty - poc
public class XMLMarshaller
{
    private final String mainNamespace;
    
    private final Map<Class<? extends BranchNodeMetaModel>, XMLNodeMarshaller> nodeMarshallerIndex;
    private final Map<String, Function<Object, String>> toStringIndex = StringConverter.toStringIndex();
    private final Map<String, Function<String, Object>> fromStringIndex = StringConverter.fromStringIndex();
    
    protected XMLMarshaller(final String namespace)
    {
        super();
        this.mainNamespace = namespace;
        this.nodeMarshallerIndex = new HashMap<Class<? extends BranchNodeMetaModel>, XMLMarshaller.XMLNodeMarshaller>();
    }
    
    protected void publish(final BranchNodeMetaModel model)
    {
        Class<? extends BranchNodeMetaModel> nodeModelClass = model.getClass();
        if (this.nodeMarshallerIndex.containsKey(nodeModelClass))
        {
            throw new IllegalStateException("marshallerIndex.containsKey(nodeModelClass)"); // TODO simply return
        }
        
        XMLNodeMarshaller nodeMarshaller = new XMLNodeMarshaller(nodeModelClass);
        this.nodeMarshallerIndex.put(nodeModelClass, nodeMarshaller);
        
    }
    
    protected void build()
    {
        
        for (final Entry<Class<? extends BranchNodeMetaModel>, XMLNodeMarshaller> entry : this.nodeMarshallerIndex.entrySet())
        {
            BranchNodeMetaModel metaModel = ModelRegistry.getBranchNodeMetaModel(entry.getValue().nodeModelClass);
            for (final INodeType<?, ?> nodeType : metaModel.getNodeTypeList())
            {
                if (nodeType instanceof LeafNodeType)
                {
                    XmlAttribute xmlAttribute = nodeType.referencedByField().getAnnotation(XmlAttribute.class);
                    XmlElement xmlElement = nodeType.referencedByField().getAnnotation(XmlElement.class);
                    Transient transientFlag = nodeType.referencedByField().getAnnotation(Transient.class);
                    
                    if (transientFlag != null)
                    {
                        continue;
                    }
                    
                    SubUnmarshallerContainer unmarshalContainer = new SubUnmarshallerContainer();
                    unmarshalContainer.nodeType = nodeType;
                    unmarshalContainer.parseTextOnly = true;
                    unmarshalContainer.nodeName = nodeType.getNodeName();
                    unmarshalContainer.stringToValue = this.fromStringIndex.get(nodeType.getTypeClass().getCanonicalName());
                    unmarshalContainer.marshaller = this.nodeMarshallerIndex.get(nodeType.getTypeClass());
                    
                    if (unmarshalContainer.stringToValue == null)
                    {
                        throw new RuntimeException("Deserializer for class " + nodeType.getTypeClass().getCanonicalName() + " not found");
                    }
                    
                    SubMarshallerContainer marshalContainer = new SubMarshallerContainer();
                    marshalContainer.nodeType = nodeType;
                    marshalContainer.valueToString = this.toStringIndex.get(nodeType.getTypeClass().getCanonicalName());
                    
                    marshalContainer.ignoreIfNull = nodeType.referencedByField().getAnnotation(IgnoreIfNull.class) != null;
                    
                    if (nodeType.getTypeClass() == Boolean.class)
                    {
                        marshalContainer.ignoreIfTrue = nodeType.referencedByField().getAnnotation(IgnoreIfTrue.class) != null;
                        marshalContainer.ignoreIfFalse = nodeType.referencedByField().getAnnotation(IgnoreIfFalse.class) != null;
                    }
                    
                    marshalContainer.nodeName = nodeType.getNodeName();
                    if (xmlAttribute != null)
                    {
                        if ((xmlAttribute.name() != null) && (!xmlAttribute.name().isEmpty()) && (!"##default".equals(xmlAttribute.name())))
                        {
                            marshalContainer.nodeName = xmlAttribute.name();
                            unmarshalContainer.nodeName = xmlAttribute.name();
                        }
                    }
                    else if (xmlElement != null)
                    {
                        if ((xmlElement.name() != null) && (!xmlElement.name().isEmpty()) && (!"##default".equals(xmlElement.name())))
                        {
                            marshalContainer.nodeName = xmlElement.name();
                            unmarshalContainer.nodeName = xmlElement.name();
                        }
                    }
                    
                    if (marshalContainer.valueToString == null)
                    {
                        throw new RuntimeException("Serializer for class " + nodeType.getTypeClass().getCanonicalName() + " not found");
                    }
                    if (xmlAttribute != null)
                    {
                        marshalContainer.runner = marshalContainer::runLeafNodeAsAttribute;
                        entry.getValue().attributeSubMarshallerList.add(marshalContainer);
                        
                        unmarshalContainer.runner = unmarshalContainer::runLeafNodeAsAttribute;
                        entry.getValue().attributeSubUnmarshallerIndex.put(unmarshalContainer.nodeName, unmarshalContainer);
                    }
                    else
                    {
                        marshalContainer.runner = marshalContainer::runLeafNodeAsElement;
                        entry.getValue().elementMarshallerList.add(marshalContainer);
                        
                        unmarshalContainer.runner = unmarshalContainer::runLeafNodeAsElement;
                        entry.getValue().elementSubUnmarshallerIndex.put(unmarshalContainer.nodeName, unmarshalContainer);
                    }
                    
                    if (marshalContainer.ignoreIfTrue)
                    {
                        entry.getValue().defaultSetterUnmarshalling.add(b -> b.setValue((LeafNodeType) nodeType, true));
                    }
                    else if (marshalContainer.ignoreIfFalse)
                    {
                        entry.getValue().defaultSetterUnmarshalling.add(b -> b.setValue((LeafNodeType) nodeType, false));
                    }
                    if ((!marshalContainer.ignoreIfNull) && marshalContainer.ignoreIfEmpty)
                    {
                        entry.getValue().defaultSetterUnmarshalling.add(b -> b.setValue((LeafNodeType) nodeType, ""));
                    }
                    
                }
                
                if (nodeType instanceof BranchNodeType)
                {
                    XmlElement xmlElement = nodeType.referencedByField().getAnnotation(XmlElement.class);
                    
                    SubUnmarshallerContainer unmarshalContainer = new SubUnmarshallerContainer();
                    unmarshalContainer.nodeType = nodeType;
                    unmarshalContainer.nodeName = nodeType.getNodeName();
                    unmarshalContainer.marshaller = this.nodeMarshallerIndex.get(nodeType.getTypeClass());
                    
                    SubMarshallerContainer container = new SubMarshallerContainer();
                    container.ignoreIfNull = nodeType.referencedByField().getAnnotation(IgnoreIfNull.class) != null;
                    
                    container.nodeType = nodeType;
                    container.marshaller = this.nodeMarshallerIndex.get(nodeType.getTypeClass());
                    container.nodeName = nodeType.getNodeName();
                    
                    if (xmlElement != null)
                    {
                        if ((xmlElement.name() != null) && (!xmlElement.name().isEmpty()) && (!"##default".equals(xmlElement.name())))
                        {
                            container.nodeName = xmlElement.name();
                            unmarshalContainer.nodeName = xmlElement.name();
                        }
                    }
                    
                    if (container.marshaller == null)
                    {
                        throw new RuntimeException("Marshaller for class " + nodeType.getTypeClass().getCanonicalName() + " not found");
                    }
                    container.runner = container::runBranchNode;
                    entry.getValue().elementMarshallerList.add(container);
                    
                    unmarshalContainer.runner = unmarshalContainer::runBranchNode;
                    entry.getValue().elementSubUnmarshallerIndex.put(unmarshalContainer.nodeName, unmarshalContainer);
                }
                
                if (nodeType instanceof BranchNodeListType)
                {
                    XmlElement xmlElement = nodeType.referencedByField().getAnnotation(XmlElement.class);
                    XMLNodeList xmlNodeList = nodeType.referencedByField().getAnnotation(XMLNodeList.class);
                    
                    SubUnmarshallerContainer unmarshalContainer = new SubUnmarshallerContainer();
                    unmarshalContainer.nodeType = nodeType;
                    unmarshalContainer.nodeName = nodeType.getNodeName();
                    unmarshalContainer.marshaller = this.nodeMarshallerIndex.get(nodeType.getTypeClass());
                    
                    SubMarshallerContainer container = new SubMarshallerContainer();
                    container.ignoreIfEmpty = nodeType.referencedByField().getAnnotation(IgnoreIfEmpty.class) != null;
                    
                    container.nodeType = nodeType;
                    container.marshaller = this.nodeMarshallerIndex.get(nodeType.getTypeClass());
                    container.nodeName = nodeType.getNodeName();
                    if (xmlElement != null)
                    {
                        if ((xmlElement.name() != null) && (!xmlElement.name().isEmpty()) && (!"##default".equals(xmlElement.name())))
                        {
                            container.nodeName = xmlElement.name();
                            unmarshalContainer.nodeName = nodeType.getNodeName();
                        }
                    }
                    
                    container.singleName = nodeType.getTypeClass().getSimpleName();
                    
                    if ((xmlNodeList != null) && (xmlNodeList.childElementName() != null) && (!xmlNodeList.childElementName().isEmpty()))
                    {
                        container.singleName = xmlNodeList.childElementName();
                        unmarshalContainer.singleName = xmlNodeList.childElementName();
                    }
                    
                    if ((xmlNodeList != null) && (!xmlNodeList.listElement()))
                    {
                        container.listElement = false;
                        unmarshalContainer.listElement = false;
                    }
                    
                    if (container.marshaller == null)
                    {
                        throw new RuntimeException("Marshaller for class " + nodeType.getTypeClass().getCanonicalName() + " not found");
                    }
                    container.runner = container::runBranchNodeList;
                    
                    entry.getValue().elementMarshallerList.add(container);
                    
                    if (unmarshalContainer.listElement)
                    {
                        unmarshalContainer.runner = unmarshalContainer::runBranchNodeListWithListElement;
                        entry.getValue().elementSubUnmarshallerIndex.put(unmarshalContainer.nodeName, unmarshalContainer);
                    }
                    else
                    {
                        unmarshalContainer.runner = unmarshalContainer::runBranchNodeListWithoutListElement;
                        entry.getValue().elementSubUnmarshallerIndex.put(unmarshalContainer.singleName, unmarshalContainer);
                    }
                }
            }
            
        }
    }
    
    public void marshal(final BranchNode<?, ?> node, final OutputStream os, final boolean closeStream) throws IOException, XMLStreamException, FactoryConfigurationError
    {
        try
        {
            XMLNodeMarshaller rootMarshaller = this.nodeMarshallerIndex.get(node.getNodeType().getTypeClass());
            if (rootMarshaller == null)
            {
                throw new IllegalStateException("Marshaller not found for " + node.getNodeType().getTypeClass());
            }
            XMLStreamWriter out = XMLOutputFactory.newInstance().createXMLStreamWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
            try
            {
                String rootName = node.getNodeType().getNodeName();
                XmlElement xmlElement = node.getNodeType().referencedByField().getAnnotation(XmlElement.class);
                if ((xmlElement != null) && (xmlElement.name() != null) && (!xmlElement.name().isEmpty()) && (!"##default".equals(xmlElement.name())))
                {
                    rootName = xmlElement.name();
                }
                out.setDefaultNamespace(this.mainNamespace);
                out.writeStartDocument("UTF-8", "1.0");
                out.writeStartElement(rootName);
                out.writeDefaultNamespace(this.mainNamespace);
                
                rootMarshaller.marshal(out, node);
                out.writeEndElement();
                out.writeEndDocument();
            }
            finally
            {
                out.flush();
                out.close(); // does not close the underlying output stream
            }
        }
        finally
        {
            if (closeStream)
            {
                os.close();
            }
        }
    }
    
    public void unmarshal(final BranchNode<?, ?> node, final InputStream is, final boolean closeStream) throws IOException, XMLStreamException, FactoryConfigurationError
    {
        try
        {
            XMLNodeMarshaller rootMarshaller = this.nodeMarshallerIndex.get(node.getNodeType().getTypeClass());
            if (rootMarshaller == null)
            {
                throw new IllegalStateException("Marshaller not found for " + node.getNodeType().getTypeClass());
            }
            
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(is);
            ReaderInput readerInput = new ReaderInput();
            readerInput.setReader(reader);
            try
            {
                
                while (reader.hasNext())
                {
                    switch (reader.next())
                    {
                    case XMLStreamConstants.START_ELEMENT:
                        
                        rootMarshaller.defaultSetterUnmarshalling.forEach(d -> d.accept(node));
                        int attributeCount = readerInput.getReader().getAttributeCount();
                        for (int i = 0; i < attributeCount; i++)
                        {
                            String attributeName = readerInput.getReader().getAttributeLocalName(i);
                            String attributeValue = readerInput.getReader().getAttributeValue(i);
                            
                            SubUnmarshallerContainer unmarshallerContainer = rootMarshaller.attributeSubUnmarshallerIndex.get(attributeName);
                            if (unmarshallerContainer != null)
                            {
                                readerInput.setValue(attributeValue);
                                unmarshallerContainer.runner.accept(readerInput, node);
                            }
                        }
                        readerInput.setValue(null);
                        rootMarshaller.unmarshal(readerInput, node);
                        return;
                    }
                }
            }
            finally
            {
                reader.close();
            }
            
        }
        finally
        {
            if (closeStream)
            {
                is.close();
            }
        }
    }
    
    private class XMLNodeMarshaller
    {
        protected XMLNodeMarshaller(final Class<? extends BranchNodeMetaModel> nodeModelClass)
        {
            super();
            this.nodeModelClass = nodeModelClass;
        }
        
        protected Class<? extends BranchNodeMetaModel> nodeModelClass = null;
        protected List<SubMarshallerContainer> attributeSubMarshallerList = new ArrayList<>();
        protected List<SubMarshallerContainer> elementMarshallerList = new ArrayList<>();
        protected List<Consumer<BranchNode<?, ?>>> defaultSetterUnmarshalling = new ArrayList<>();
        protected Map<String, SubUnmarshallerContainer> attributeSubUnmarshallerIndex = new HashMap<>();
        protected Map<String, SubUnmarshallerContainer> elementSubUnmarshallerIndex = new HashMap<>();
        
        protected void marshal(final XMLStreamWriter out, final BranchNode<? extends BranchNodeMetaModel, ? extends BranchNodeMetaModel> node) throws XMLStreamException
        {
            for (final SubMarshallerContainer container : this.attributeSubMarshallerList)
            {
                container.runner.accept(out, node);
            }
            for (final SubMarshallerContainer container : this.elementMarshallerList)
            {
                container.runner.accept(out, node);
            }
        }
        
        protected void unmarshal(final ReaderInput readerInput, final BranchNode<?, ?> node) throws XMLStreamException
        {
            SubUnmarshallerContainer unmarshallerContainerForText = null;
            boolean isNull = false;
            int openedElement = 0;
            StringBuilder characterBuilder = null;
            String singleCharacter = null;
            while (readerInput.getReader().hasNext())
            {
                switch (readerInput.getReader().next())
                {
                case XMLStreamConstants.START_ELEMENT:
                    
                    if (characterBuilder != null)
                    {
                        if (isNull)
                        {
                            readerInput.setValue(null);
                        }
                        else
                        {
                            readerInput.setValue(characterBuilder.toString());
                        }
                        unmarshallerContainerForText.runner.accept(readerInput, node);
                    }
                    else if (singleCharacter != null)
                    {
                        if (isNull)
                        {
                            readerInput.setValue(null);
                        }
                        else
                        {
                            readerInput.setValue(singleCharacter);
                        }
                        unmarshallerContainerForText.runner.accept(readerInput, node);
                    }
                    characterBuilder = null;
                    singleCharacter = null;
                    unmarshallerContainerForText = null;
                    String name = readerInput.getReader().getLocalName();
                    
                    SubUnmarshallerContainer unmarshallerContainer = this.elementSubUnmarshallerIndex.get(name);
                    if (unmarshallerContainer != null)
                    {
                        if (unmarshallerContainer.parseTextOnly)
                        {
                            openedElement++;
                            isNull = false;
                            int attributeCount = readerInput.getReader().getAttributeCount();
                            for (int i = 0; i < attributeCount; i++)
                            {
                                String attributeName = readerInput.getReader().getAttributeLocalName(i);
                                String attributeValue = readerInput.getReader().getAttributeValue(i);
                                
                                if ("null".equals(attributeName) && Boolean.TRUE.toString().equals(attributeValue))
                                {
                                    isNull = true;
                                }
                            }
                            unmarshallerContainerForText = unmarshallerContainer;
                        }
                        else
                        {
                            unmarshallerContainer.runner.accept(readerInput, node);
                        }
                    }
                    else
                    {
                        openedElement++;
                    }
                    
                    break;
                
                case XMLStreamConstants.END_ELEMENT:
                    
                    if (characterBuilder != null)
                    {
                        if (isNull)
                        {
                            readerInput.setValue(null);
                        }
                        else
                        {
                            readerInput.setValue(characterBuilder.toString());
                        }
                        unmarshallerContainerForText.runner.accept(readerInput, node);
                    }
                    else if (singleCharacter != null)
                    {
                        if (isNull)
                        {
                            readerInput.setValue(null);
                        }
                        else
                        {
                            readerInput.setValue(singleCharacter);
                        }
                        unmarshallerContainerForText.runner.accept(readerInput, node);
                    }
                    characterBuilder = null;
                    singleCharacter = null;
                    unmarshallerContainerForText = null;
                    openedElement--;
                    if (openedElement < 0)
                    {
                        return;
                    }
                    
                    break;
                
                case XMLStreamConstants.CHARACTERS:
                    
                    if (unmarshallerContainerForText != null)
                    {
                        if (singleCharacter == null)
                        {
                            singleCharacter = readerInput.getReader().getText();
                        }
                        else if (characterBuilder != null)
                        {
                            characterBuilder.append(readerInput.getReader().getText());
                        }
                        else
                        {
                            characterBuilder = new StringBuilder();
                            characterBuilder.append(singleCharacter);
                            characterBuilder.append(readerInput.getReader().getText());
                        }
                    }
                    
                    break;
                }
                
            }
        }
    }
    
    private class SubUnmarshallerContainer
    {
        protected INodeType<?, ?> nodeType;
        protected boolean parseTextOnly = false;
        protected Function<String, Object> stringToValue = null;
        protected BiConsumer<ReaderInput, BranchNode<?, ?>> runner = null;
        protected XMLNodeMarshaller marshaller = null;
        protected String nodeName = null;
        protected String singleName = null;
        protected boolean listElement = true;
        
        protected void runLeafNodeAsAttribute(final ReaderInput readerInput, final BranchNode<?, ?> node)
        {
            node.setValue((LeafNodeType) this.nodeType, this.stringToValue.apply(readerInput.getValue()));
        }
        
        protected void runLeafNodeAsElement(final ReaderInput readerInput, final BranchNode node)
        {
            node.setValue((LeafNodeType) this.nodeType, this.stringToValue.apply(readerInput.getValue()));
        }
        
        protected void runBranchNode(final ReaderInput readerInput, final BranchNode node)
        {
            try
            {
                int attributeCount = readerInput.getReader().getAttributeCount();
                
                for (int i = 0; i < attributeCount; i++)
                {
                    if (readerInput.getReader().getAttributeLocalName(i).equals("null") && "true".equals(readerInput.getReader().getAttributeValue(i)))
                    {
                        readerInput.setValue(null);
                        int openedElement = 1;
                        while (readerInput.getReader().hasNext())
                        {
                            switch (readerInput.getReader().next())
                            {
                            case XMLStreamConstants.START_ELEMENT:
                                
                                openedElement++;
                                
                                break;
                            
                            case XMLStreamConstants.END_ELEMENT:
                                
                                openedElement--;
                                if (openedElement == 0)
                                {
                                    return;
                                }
                                
                                break;
                            }
                        }
                        
                        return;
                    }
                }
                BranchNode child = node.create((BranchNodeType) this.nodeType);
                this.marshaller.defaultSetterUnmarshalling.forEach(d -> d.accept(child));
                
                for (int i = 0; i < attributeCount; i++)
                {
                    String attributeName = readerInput.getReader().getAttributeLocalName(i);
                    String attributeValue = readerInput.getReader().getAttributeValue(i);
                    
                    SubUnmarshallerContainer unmarshallerContainer = this.marshaller.attributeSubUnmarshallerIndex.get(attributeName);
                    if (unmarshallerContainer != null)
                    {
                        readerInput.setValue(attributeValue);
                        unmarshallerContainer.runner.accept(readerInput, child);
                    }
                }
                readerInput.setValue(null);
                this.marshaller.unmarshal(readerInput, child);
            }
            catch (final Exception e)
            {
                throw new RuntimeException(this.nodeType + " " + e.getMessage(), e);
            }
        }
        
        protected void runBranchNodeListWithListElement(final ReaderInput readerInput, final BranchNode node)
        {
            try
            {
                while (readerInput.getReader().hasNext())
                {
                    switch (readerInput.getReader().next())
                    {
                    case XMLStreamConstants.START_ELEMENT:
                        BranchNode child = node.create((BranchNodeListType) this.nodeType);
                        this.marshaller.defaultSetterUnmarshalling.forEach(d -> d.accept(child));
                        int attributeCount = readerInput.getReader().getAttributeCount();
                        for (int i = 0; i < attributeCount; i++)
                        {
                            String attributeName = readerInput.getReader().getAttributeLocalName(i);
                            String attributeValue = readerInput.getReader().getAttributeValue(i);
                            
                            SubUnmarshallerContainer unmarshallerContainer = this.marshaller.attributeSubUnmarshallerIndex.get(attributeName);
                            if (unmarshallerContainer != null)
                            {
                                readerInput.setValue(attributeValue);
                                unmarshallerContainer.runner.accept(readerInput, child);
                            }
                        }
                        readerInput.setValue(null);
                        this.marshaller.unmarshal(readerInput, child);
                        break;
                    
                    case XMLStreamConstants.END_ELEMENT:
                        return;
                        
                    }
                }
            }
            catch (final Exception e)
            {
                throw new RuntimeException(this.nodeType + " " + e.getMessage(), e);
            }
        }
        
        protected void runBranchNodeListWithoutListElement(final ReaderInput readerInput, final BranchNode<?, ?> node)
        {
            try
            {
                BranchNode child = node.create((BranchNodeListType) this.nodeType);
                this.marshaller.defaultSetterUnmarshalling.forEach(d -> d.accept(child));
                int attributeCount = readerInput.getReader().getAttributeCount();
                for (int i = 0; i < attributeCount; i++)
                {
                    String attributeName = readerInput.getReader().getAttributeLocalName(i);
                    String attributeValue = readerInput.getReader().getAttributeValue(i);
                    
                    SubUnmarshallerContainer unmarshallerContainer = this.marshaller.attributeSubUnmarshallerIndex.get(attributeName);
                    if (unmarshallerContainer != null)
                    {
                        readerInput.setValue(attributeValue);
                        unmarshallerContainer.runner.accept(readerInput, child);
                    }
                }
                readerInput.setValue(null);
                this.marshaller.unmarshal(readerInput, child);
            }
            catch (final Exception e)
            {
                throw new RuntimeException(this.nodeType + " " + e.getMessage(), e);
            }
        }
    }
    
    private class SubMarshallerContainer
    {
        protected INodeType<?, ?> nodeType;
        protected BiConsumer<XMLStreamWriter, BranchNode<?, ?>> runner = null;
        protected Function<Object, String> valueToString = null;
        protected XMLNodeMarshaller marshaller = null;
        protected String nodeName = null;
        protected String singleName = null;
        protected boolean listElement = true;
        boolean ignoreIfNull = false;
        boolean ignoreIfTrue = false;
        boolean ignoreIfFalse = false;
        boolean ignoreIfEmpty = false;
        
        protected void runLeafNodeAsElement(final XMLStreamWriter out, final BranchNode<?, ?> node)
        {
            try
            {
                
                LeafNode<?, ?> leafNode = node.get((LeafNodeType) this.nodeType);
                if (leafNode.getValue() == null)
                {
                    if (this.ignoreIfNull)
                    {
                        return;
                    }
                    out.writeStartElement(this.nodeName);
                    out.writeAttribute("null", Boolean.TRUE.toString());
                    out.writeEndElement();
                }
                else
                {
                    if ((this.ignoreIfFalse) && (!((Boolean) leafNode.getValue()).booleanValue()))
                    {
                        return;
                    }
                    if ((this.ignoreIfTrue) && ((Boolean) leafNode.getValue()).booleanValue())
                    {
                        return;
                    }
                    out.writeStartElement(this.nodeName);
                    out.writeCharacters(this.valueToString.apply(leafNode.getValue()));
                    out.writeEndElement();
                }
                
            }
            catch (final Exception e)
            {
                throw new RuntimeException(this.nodeType + " " + e.getMessage(), e);
            }
        }
        
        protected void runLeafNodeAsAttribute(final XMLStreamWriter out, final BranchNode<?, ?> node)
        {
            try
            {
                LeafNode<?, ?> leafNode = node.get((LeafNodeType) this.nodeType);
                if (leafNode.getValue() == null)
                {
                    if (this.ignoreIfNull)
                    {
                        return;
                    }
                    out.writeAttribute(this.nodeName, "");
                }
                else
                {
                    if ((this.ignoreIfFalse) && (!((Boolean) leafNode.getValue()).booleanValue()))
                    {
                        return;
                    }
                    if ((this.ignoreIfTrue) && ((Boolean) leafNode.getValue()).booleanValue())
                    {
                        return;
                    }
                    out.writeAttribute(this.nodeName, this.valueToString.apply(leafNode.getValue()));
                }
            }
            catch (final Exception e)
            {
                throw new RuntimeException(this.nodeType + " " + e.getMessage(), e);
            }
        }
        
        protected void runBranchNode(final XMLStreamWriter out, final BranchNode<?, ?> node)
        {
            try
            {
                BranchNode<?, ?> branchNode = node.get((BranchNodeType) this.nodeType);
                if (branchNode == null)
                {
                    if (this.ignoreIfNull)
                    {
                        return;
                    }
                    out.writeStartElement(this.nodeName);
                    out.writeAttribute("null", Boolean.TRUE.toString());
                    out.writeEndElement();
                }
                else
                {
                    out.writeStartElement(this.nodeName);
                    this.marshaller.marshal(out, branchNode);
                    out.writeEndElement();
                }
                
            }
            catch (final Exception e)
            {
                throw new RuntimeException(this.nodeType + " " + e.getMessage(), e);
            }
        }
        
        protected void runBranchNodeList(final XMLStreamWriter out, final BranchNode<?, ?> node)
        {
            try
            {
                List<BranchNode<?, ?>> branchNodeList = node.getUnmodifiableNodeList((BranchNodeListType) this.nodeType);
                
                if (this.ignoreIfEmpty && branchNodeList.isEmpty())
                {
                    return;
                }
                
                if (this.listElement)
                {
                    
                    out.writeStartElement(this.nodeName);
                }
                
                for (final BranchNode<?, ?> branchNode : branchNodeList)
                {
                    out.writeStartElement(this.singleName);
                    if (branchNode == null)
                    {
                        out.writeAttribute("null", Boolean.TRUE.toString());
                    }
                    else
                    {
                        this.marshaller.marshal(out, branchNode);
                    }
                    out.writeEndElement();
                }
                if (this.listElement)
                {
                    out.writeEndElement();
                }
            }
            catch (final Exception e)
            {
                throw new RuntimeException(this.nodeType + " " + e.getMessage(), e);
            }
        }
        
    }
    
    public static XMLMarshaller getForTreeModel(final Class<? extends TypedTreeMetaModel<?>> modelClass)
    {
        ParseXMLMarshallerHandler xmlMarsallerHandler = new ParseXMLMarshallerHandler(modelClass);
        
        ModelRegistry.parse(modelClass, xmlMarsallerHandler);
        
        return xmlMarsallerHandler.getXMLMarshaller();
    }
    
    private static class ParseXMLMarshallerHandler implements ITypedTreeModelParserHandler
    {
        private String namespace = null;
        private XMLMarshaller marshaller = null;
        private volatile boolean buildDone = false;
        
        public ParseXMLMarshallerHandler(final Class<? extends TypedTreeMetaModel<?>> modelClass)
        {
            super();
            
            Version version = modelClass.getDeclaredAnnotation(Version.class);
            String versionString = version == null ? "1.0.0" : version.major() + "." + version.minor() + "." + version.service();
            Domain domain = modelClass.getDeclaredAnnotation(Domain.class);
            if (domain != null)
            {
                this.namespace = "http://" + domain.name() + "/xmlns/" + domain.module() + "/v" + versionString;
            }
            else
            {
                String packageName = modelClass.getPackage().getName();
                String[] packageSplit = packageName.split("\\.");
                String domainName = "";
                for (int i = packageSplit.length; i > 0; i--)
                {
                    if (!domainName.isEmpty())
                    {
                        domainName = domainName + ".";
                    }
                    domainName = domainName + packageSplit[i - 1];
                }
                this.namespace = "http://" + domainName + "/xmlns/" + modelClass.getSimpleName().toLowerCase() + "/v" + versionString;
            }
            
            this.marshaller = new XMLMarshaller(this.namespace);
        }
        
        @Override
        public void startModel(final BranchNodeMetaModel model, final Set<INodeType<BranchNodeMetaModel, ?>> references)
        {
            ITypedTreeModelParserHandler.super.startModel(model, references);
            this.marshaller.publish(model);
        }
        
        @Override
        public void endModel(final BranchNodeMetaModel model, final Set<INodeType<BranchNodeMetaModel, ?>> references)
        {
            ITypedTreeModelParserHandler.super.endModel(model, references);
        }
        
        @Override
        public void onNodeType(final BranchNodeMetaModel model, final INodeType<BranchNodeMetaModel, ?> nodeType) { }
        
        public XMLMarshaller getXMLMarshaller()
        {
            if (!this.buildDone)
            {
                this.buildDone = true;
                this.marshaller.build();
            }
            return this.marshaller;
        }
        
    }
    
    private class ReaderInput
    {
        private XMLStreamReader reader = null;
        private String value = null;
        
        protected XMLStreamReader getReader()
        {
            return this.reader;
        }
        
        protected void setReader(final XMLStreamReader reader)
        {
            this.reader = reader;
        }
        
        protected String getValue()
        {
            return this.value;
        }
        
        protected void setValue(final String value)
        {
            this.value = value;
        }
        
    }
    
}
