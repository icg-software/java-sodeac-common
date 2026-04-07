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
package org.sodeac.common.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.sodeac.common.function.ConplierBean;
import org.sodeac.common.jdbc.ResultSetParseHelper.ResultSetParseHelperBuilder.NodeConfiguration;
import org.sodeac.common.jdbc.ResultSetParseHelper.ResultSetParseHelperBuilder.ParsePhase;
import org.sodeac.common.misc.RuntimeWrappedException;

/**
 *
 * A helper class to convert  data from more or less complex linked {@link ResultSet} to a tree like / object based data structure.
 * <br><br>
 * The rows are divided into clusters. After all parse phases process a cluster, the next cluster is processed by parse phases.
 * <br><br>
 * The requirements:<br>
 * 1. The processed {@link ResultSet} must be scrollable<br>
 * 2. The data for a main object must be delivered in consecutively rows<br>
 * 3. Each object requires an unique-key-column<br>
 * 4. The main configurations of all project phases must specify the same key column<br>
 * <br>
 *
 * @author Sebastian Palarus
 *
 */
public class ResultSetParseHelper implements AutoCloseable
{
    public static final String DEFAULT_PHASE = "DEFAULT";
    
    private ResultSetParseHelper()
    {
        super();
    }
    
    private List<ParsePhase> parsePhaseList = null;
    private volatile boolean closable = true;
    
    public ResultSetParseHelper setUnclosable()
    {
        this.closable = false;
        return this;
    }
    
    // TODO Controller: stopper, skipper, progress
    public void parse(final PreparedStatement preparedStatement, final Object root, final int mainPhaseSize) throws SQLException
    {
        if (mainPhaseSize < 1)
        {
            throw new IllegalStateException("mainPhaseSize < 1");
        }
        
        ResultSet resultSet = preparedStatement.executeQuery();
        try
        {
            resultSet.last();
            int lastRow = resultSet.getRow();
            resultSet.beforeFirst();
            
            if (lastRow < 1)
            {
                return;
            }
            
            List<ParsePhaseInstance> parsePhaseInstanceList = new ArrayList<ParsePhaseInstance>();
            
            NodeConfiguration mainConfiguration = null;
            
            for (final ParsePhase parsePhase : this.parsePhaseList)
            {
                if (parsePhase.nodeConfiguration == null)
                {
                    continue;
                }
                ParsePhaseInstance parsePhaseInstance = new ParsePhaseInstance();
                parsePhaseInstance.parsePhase = parsePhase;
                parsePhaseInstance.mainNode = new Node();
                parsePhaseInstance.mainNode.configuration = parsePhase.nodeConfiguration;
                parsePhaseInstance.mainNode.objects = new HashMap<>();
                parsePhaseInstance.mainNode.childNodes = new HashMap<>();
                parsePhaseInstanceList.add(parsePhaseInstance);
                if (mainConfiguration == null)
                {
                    mainConfiguration = parsePhase.getNodeConfiguration();
                }
            }
            
            if (parsePhaseInstanceList.isEmpty())
            {
                return;
            }
            
            Cursor cursor = new Cursor<>();
            cursor.setResultSet(resultSet);
            
            int beforeFirstInCluster = -1;
            boolean parse = true;
            while (parse)
            {
                if (beforeFirstInCluster > -1)
                {
                    resultSet.absolute(beforeFirstInCluster);
                }
                
                int firstInCluster = -1;
                int lastInCluster = -1;
                
                Set<Object> ids = new HashSet<>();
                
                while (resultSet.next())
                {
                    if (firstInCluster == -1)
                    {
                        firstInCluster = resultSet.getRow();
                    }
                    
                    ids.add(fetchId(mainConfiguration.idType, mainConfiguration.idColumnName, resultSet));
                    if (ids.size() > mainPhaseSize)
                    {
                        break;
                    }
                    
                    lastInCluster = resultSet.getRow();
                }
                
                beforeFirstInCluster = lastInCluster;
                
                if (lastInCluster == lastRow)
                {
                    parse = false;
                }
                
                List<Object> mainNodeList = new ArrayList<>();
                Set<Object> mainNodeSet = new HashSet<>();
                Object lastMainNode = null;
                
                for (final ParsePhaseInstance parsePhaseInstance : parsePhaseInstanceList)
                {
                    lastMainNode = null;
                    
                    if (firstInCluster == 1)
                    {
                        resultSet.beforeFirst();
                    }
                    else
                    {
                        resultSet.absolute(firstInCluster - 1);
                    }
                    
                    while (resultSet.next())
                    {
                        cursor.setMainObject(null);
                        cursor.setParentObject(root);
                        
                        Object mainNode = parsePhaseInstance.mainNode.fetch(resultSet, cursor, true);
                        Objects.requireNonNull(mainNode);
                        
                        if (!mainNode.equals(mainNode))
                        {
                            throw new IllegalStateException("main nodes must equals to self");
                        }
                        
                        if (!mainNodeSet.contains(mainNode))
                        {
                            mainNodeSet.add(mainNode);
                            mainNodeList.add(mainNode);
                            
                            if ((lastMainNode != null) && (parsePhaseInstance.parsePhase.consumerMainNodeComplete != null))
                            {
                                parsePhaseInstance.parsePhase.consumerMainNodeComplete.accept(lastMainNode);
                            }
                            lastMainNode = mainNode;
                        }
                        
                        if (lastInCluster == resultSet.getRow())
                        {
                            break;
                        }
                    }
                    
                    if ((lastMainNode != null) && (parsePhaseInstance.parsePhase.consumerMainNodeComplete != null))
                    {
                        parsePhaseInstance.parsePhase.consumerMainNodeComplete.accept(lastMainNode);
                    }
                    
                    if ((!mainNodeList.isEmpty()) && (parsePhaseInstance.parsePhase.consumerClusterComplete != null))
                    {
                        parsePhaseInstance.parsePhase.consumerClusterComplete.accept(mainNodeList);
                    }
                    
                    parsePhaseInstance.mainNode.reset();
                    mainNodeList.clear();
                    mainNodeSet.clear();
                    lastMainNode = null;
                }
                
                mainNodeList = null;
                mainNodeSet = null;
                lastMainNode = null;
                
                if (resultSet.getRow() == lastRow)
                {
                    parse = false;
                }
            }
            
            parsePhaseInstanceList.forEach(i -> i.clear());
            parsePhaseInstanceList.clear();
            cursor.clear();
            
            parsePhaseInstanceList = null;
            cursor = null;
        }
        finally
        {
            resultSet.close();
        }
        
    }
    
    private class ParsePhaseInstance
    {
        ParsePhase parsePhase;
        Node mainNode;
        
        private void clear()
        {
            this.parsePhase = null;
            if (this.mainNode != null)
            {
                this.mainNode.clear();
                this.mainNode = null;
            }
        }
    }
    
    protected class Node
    {
        private Node parent = null;
        private Object id = null;
        private NodeConfiguration configuration = null;
        private Map<Object, Object> objects = null;                // PK / Object
        private Map<Object, List<Node>> childNodes = null;        // PK / ConfigurationName,ChildNode
        private Object lastId = UUID.randomUUID().toString();
        private Object lastObject = null;
        
        private void reset()
        {
            NodeConfiguration configurationBackup = this.configuration;
            this.clear();
            this.configuration = configurationBackup;
            this.objects = new HashMap<>();
            this.childNodes = new HashMap<>();
        }
        
        private void clear()
        {
            if (this.objects != null)
            {
                this.objects.clear();
            }
            if (this.childNodes != null)
            {
                for (final List<Node> child : this.childNodes.values())
                {
                    if (child != null)
                    {
                        child.forEach(n -> n.clear());
                    }
                    child.clear();
                }
                this.childNodes.clear();
            }
            this.parent = null;
            this.configuration = null;
            this.objects = null;
            this.childNodes = null;
            this.lastId = null;
            this.id = null;
            this.lastObject = null;
        }
        
        protected Object fetch(final ResultSet resultSet, final Cursor cursor, final boolean isMain) throws SQLException
        {
            Object currentId = fetchId(this.configuration.idType, this.configuration.idColumnName, resultSet);
            cursor.setId(currentId);
            List<Node> childs = this.childNodes.get(currentId);
            
            if
            (
                (!((currentId == null) && (this.lastId == null))) &&
                ((currentId == null) || (!currentId.equals(this.lastId)))
            )
            {
                this.lastId = currentId;
                
                if (this.objects.containsKey(currentId))
                {
                    this.lastObject = this.objects.get(currentId);
                }
                else
                {
                    if (isMain)
                    {
                        this.reset();
                    }
                    Object object = null;
                    if (isMain)
                    {
                        cursor.mainObject = null;
                    }
                    try
                    {
                        if ((currentId == null) && (this.configuration.recordParserIfNull != null))
                        {
                            object = this.configuration.recordParserIfNull.parse(cursor);
                        }
                        else if ((currentId != null) && (this.configuration.recordParser != null))
                        {
                            object = this.configuration.recordParser.parse(cursor);
                        }
                    }
                    catch (final Exception e)
                    {
                        if (e instanceof RuntimeException)
                        {
                            throw (RuntimeException) e;
                        }
                        throw new RuntimeWrappedException(e);
                    }
                    catch (final Error e)
                    {
                        throw new RuntimeWrappedException(e);
                    }
                    this.objects.put(currentId, object);
                    this.lastObject = object;
                    
                    if ((this.configuration.childList != null) && (!this.configuration.childList.isEmpty()))
                    {
                        childs = new ArrayList<>();
                        this.childNodes.put(currentId, childs);
                        
                        for (final NodeConfiguration childNodeConfiguration : (List<NodeConfiguration>) this.configuration.childList)
                        {
                            Node node = new Node();
                            node.parent = this;
                            node.configuration = childNodeConfiguration;
                            node.objects = new HashMap<>();
                            node.childNodes = new HashMap<>();
                            node.id = currentId;
                            
                            childs.add(node);
                        }
                    }
                }
            }
            
            if (isMain)
            {
                cursor.mainObject = this.lastObject;
            }
            
            cursor.setParentObject(this.lastObject);
            
            if (childs != null)
            {
                Object backupParent = cursor.parentObject;
                Object backupId = cursor.id;
                for (final Node childNode : childs)
                {
                    cursor.parentObject = backupParent;
                    cursor.id = backupId;
                    childNode.fetch(resultSet, cursor, false);
                }
            }
            
            return this.lastObject;
        }
        
    }
    
    private Object fetchId(final Class idType, final String idColumnName, final ResultSet resultSet) throws SQLException
    {
        Object id = null;
        
        if (idType == String.class)
        {
            id = resultSet.getString(idColumnName);
        }
        else if (idType == Integer.class)
        {
            int x = resultSet.getInt(idColumnName);
            if (resultSet.wasNull())
            {
                id = null;
            }
            else
            {
                id = x;
            }
        }
        else if (idType == Long.class)
        {
            long x = resultSet.getLong(idColumnName);
            if (resultSet.wasNull())
            {
                id = null;
            }
            else
            {
                id = x;
            }
        }
        else if (idType == UUID.class)
        {
            String x = resultSet.getString(idColumnName);
            if (x == null)
            {
                id = null;
            }
            else
            {
                id = UUID.fromString(x);
            }
        }
        
        return id;
    }
    
    /**
     * Builder to build a {@link ResultSetParseHelper}
     *
     * @param <B> data type of current configuration
     * @param <I> key / id type of current configuration
     * @param <M> data type of main object
     * @param <P> data type of parent object
     *
     * @author Sebastian Palarus
     */
    public static class ResultSetParseHelperBuilder<B, I, M, P>
    {
        private List<ParsePhase> parsePhaseList = null;
        private ParsePhase currentParsePhase = null;
        
        /**
         * Creates a new builder to build a {@link ResultSetParseHelper}. A default parser phase is automatically created.
         *
         * @param mainIdColumnName  column name for key / id of main node object
         * @param mainIdType        type of main node objects key / id
         * @param mainNodeOjectType type main node object
         * @param rootType          type of super main object
         * @param recordParser      parser logic
         *
         * @return builder new builder to build a {@link ResultSetParseHelper}
         */
        public static <B, I, S> ResultSetParseHelperBuilder<B, I, B, S> newBuilder
        (
            final String mainIdColumnName,
            final Class<I> mainIdType,
            final Class<B> mainNodeOjectType,
            final Class<S> rootType,
            final IRecordParser<I, B, S, B> recordParser
        )
        {
            return newBuilder(mainIdColumnName, mainIdType, mainNodeOjectType, rootType, recordParser, null, null);
        }
        
        /**
         * Creates a new builder to build a {@link ResultSetParseHelper}. A default parser phase is automatically created.
         *
         * @param mainIdColumnName           column name for key / id of main node object
         * @param mainIdType                 type of main node objects key / id
         * @param mainNodeOjectType          type main node object
         * @param rootType                   type of super main object
         * @param recordParser               parser logic
         * @param consumerOnMainNodeComplete listener to notify if main node is completely parsed with all sub parsers
         * @param consumerClusterComplete    listener to notify if a cluster is completely parsed by parser phase
         *
         * @return new builder to build a {@link ResultSetParseHelper}
         */
        public static <B, I, S> ResultSetParseHelperBuilder<B, I, B, S> newBuilder
        (
            final String mainIdColumnName,
            final Class<I> mainIdType,
            final Class<B> mainNodeOjectType,
            final Class<S> rootType,
            final IRecordParser<I, B, S, B> recordParser,
            final Consumer<B> consumerOnMainNodeComplete,
            final Consumer<List<B>> consumerClusterComplete
        )
        {
            ResultSetParseHelperBuilder<B, I, B, S> builder = new ResultSetParseHelperBuilder<B, I, B, S>();
            builder.parsePhaseList = new ArrayList<>();
            builder.newParsePhase(DEFAULT_PHASE, mainIdColumnName, mainIdType, mainNodeOjectType, rootType, recordParser, consumerOnMainNodeComplete, consumerClusterComplete);
            
            return builder;
        }
        
        /**
         * Creates a sub parser configuration as child of current parser configuration.
         *
         * @param idColumnName  column name for key / id of node object
         * @param idType        type of node objects key / id
         * @param nodeOjectType type node object
         * @param recordParser  parser logic
         *
         * @return builder
         */
        public <C, J> ResultSetParseHelperBuilder<C, J, M, B> subParser(final String idColumnName, final Class<J> idType, final Class<C> nodeOjectType, final IRecordParser<J, M, B, C> recordParser)
        {
            NodeConfiguration parentConfiguration = this.currentParsePhase.currentNodeConfiguration;
            this.currentParsePhase.currentNodeConfiguration = new NodeConfiguration(UUID.randomUUID().toString(), new ConplierBean(), idColumnName, (Class) idType, parentConfiguration);
            parentConfiguration.childList.add(this.currentParsePhase.currentNodeConfiguration);
            
            this.currentParsePhase.currentNodeConfiguration.recordParser = (IRecordParser) recordParser;
            
            return (ResultSetParseHelperBuilder) this;
        }
        
        /**
         * Defines parser logic of current configuration in case of id null values
         *
         * @param recordParser parser logic
         *
         * @return builder
         */
        public ResultSetParseHelperBuilder<B, I, M, P> onNullRecord(final IRecordParser<I, M, P, B> recordParser)
        {
            this.currentParsePhase.currentNodeConfiguration.recordParserIfNull = recordParser;
            return this;
        }
        
        /**
         * close current configuration object and set parent configuration object as current configuration object.
         *
         * @return builder
         */
        public ResultSetParseHelperBuilder<P, ?, M, ?> build()
        {
            if (this.currentParsePhase.currentNodeConfiguration.parent != null)
            {
                this.currentParsePhase.currentNodeConfiguration = this.currentParsePhase.currentNodeConfiguration.parent;
            }
            return (ResultSetParseHelperBuilder) this;
        }
        
        /**
         * close current configuration object and set parent configuration object as current configuration object.
         *
         * @param parentKeyType    type of id of parent configuration
         * @param parentParentType type of object of parent configuration
         * @param parentIdType     type of id of parent configuration
         *
         * @return
         */
        public <A, Z, Y> ResultSetParseHelperBuilder<A, Y, M, Z> build(final Class<A> parentDataType, final Class<Z> parentParentType, final Class<Y> parentIdType)
        {
            if (this.currentParsePhase.currentNodeConfiguration.parent != null)
            {
                this.currentParsePhase.currentNodeConfiguration = this.currentParsePhase.currentNodeConfiguration.parent;
            }
            return (ResultSetParseHelperBuilder) this;
        }
        
        /**
         * Opens a new parser phase
         *
         * @param name              name of parser phase
         * @param mainIdColumnName  column name for key / id of main node object
         * @param mainIdType        type of main node objects key / id
         * @param mainNodeOjectType type main node object
         * @param rootType          type of super main object
         * @param recordParser      parser logic
         *
         * @return builder
         */
        public <B, I, S> ResultSetParseHelperBuilder<B, I, B, S> newParsePhase
        (
            final String name,
            final String mainIdColumnName,
            final Class<I> mainIdType,
            final Class<B> mainNodeOjectType,
            final Class<S> rootType,
            final IRecordParser<I, B, S, B> recordParser
        )
        {
            return newParsePhase(name, mainIdColumnName, mainIdType, mainNodeOjectType, rootType, recordParser, null, null);
        }
        
        /**
         * Opens a new parser phase
         *
         * @param name                       name of parser phase
         * @param mainIdColumnName           column name for key / id of main node object
         * @param mainIdType                 type of main node objects key / id
         * @param mainNodeOjectType          type main node object
         * @param rootType                   type of super main object
         * @param recordParser               parser logic
         * @param consumerOnMainNodeComplete listener to notify if main node is completely parsed with all sub parsers
         * @param consumerClusterComplete    listener to notify if a cluster is completely parsed by parser phase
         *
         * @return builder
         */
        public <B, I, S> ResultSetParseHelperBuilder<B, I, B, S> newParsePhase
        (
            final String name,
            final String mainIdColumnName,
            final Class<I> mainIdType,
            final Class<B> mainNodeOjectType,
            final Class<S> rootType,
            final IRecordParser<I, B, S, B> recordParser,
            final Consumer<B> consumerOnMainNodeComplete,
            final Consumer<List<B>> consumerClusterComplete
        )
        {
            this.currentParsePhase = this.new ParsePhase(name);
            this.parsePhaseList.add(this.currentParsePhase);
            
            this.currentParsePhase.currentNodeConfiguration = this.new NodeConfiguration(UUID.randomUUID().toString(), new ConplierBean(), mainIdColumnName, (Class) mainIdType, null);
            this.currentParsePhase.nodeConfiguration = this.currentParsePhase.currentNodeConfiguration;
            
            this.currentParsePhase.currentNodeConfiguration.recordParser = (IRecordParser) recordParser;
            this.currentParsePhase.consumerClusterComplete = (Consumer) consumerClusterComplete;
            this.currentParsePhase.consumerMainNodeComplete = (Consumer) consumerOnMainNodeComplete;
            
            return (ResultSetParseHelperBuilder) this;
        }
        
        /**
         * Build {@link ResultSetParseHelper} from builder. After this the builder is not usable anymore.
         *
         * @return ResultSetParseHelper
         */
        public ResultSetParseHelper buildParser()
        {
            return buildParser(true);
        }
        
        /**
         * Build {@link ResultSetParseHelper} from builder. If {@code disposeBuilder} is defined as true, after this the builder is not usable anymore.
         *
         * @param disposeBuilder indicates whether the object is disposed after creates the helper object
         *
         * @return ResultSetParseHelper
         */
        public ResultSetParseHelper buildParser(final boolean disposeBuilder)
        {
            ResultSetParseHelper parser = new ResultSetParseHelper();
            parser.parsePhaseList = new ArrayList<>();
            for (final ParsePhase parsePhase : this.parsePhaseList)
            {
                parser.parsePhaseList.add(parsePhase.copy());
            }
            if (disposeBuilder)
            {
                this.parsePhaseList.forEach(p -> p.clear());
                this.parsePhaseList.clear();
                this.parsePhaseList = null;
            }
            return parser;
        }
        
        protected class ParsePhase
        {
            private String name = null;
            private NodeConfiguration nodeConfiguration = null;
            private NodeConfiguration currentNodeConfiguration = null;
            private Consumer<Object> consumerMainNodeComplete = null;
            private Consumer<Object> consumerClusterComplete = null;
            
            protected ParsePhase(final String name)
            {
                super();
                this.name = name;
            }
            
            protected ParsePhase copy()
            {
                ParsePhase copy = new ParsePhase(this.name);
                copy.nodeConfiguration = this.nodeConfiguration.copy();
                copy.consumerClusterComplete = this.consumerClusterComplete;
                copy.consumerMainNodeComplete = this.consumerMainNodeComplete;
                return copy;
            }
            
            protected String getName()
            {
                return this.name;
            }
            
            protected NodeConfiguration getNodeConfiguration()
            {
                return this.nodeConfiguration;
            }
            
            protected void clear()
            {
                this.name = null;
                if (this.nodeConfiguration != null)
                {
                    this.nodeConfiguration.clear();
                    this.nodeConfiguration = null;
                }
                if (this.currentNodeConfiguration != null)
                {
                    this.currentNodeConfiguration.clear();
                    this.currentNodeConfiguration = null;
                }
                this.consumerMainNodeComplete = null;
                this.consumerClusterComplete = null;
            }
        }
        
        protected class NodeConfiguration
        {
            private String nodeName = null;
            private NodeConfiguration parent = null;
            private ConplierBean<B> objectReference = null;
            private Class<I> idType = null;
            private String idColumnName = null;
            private IRecordParser<I, M, P, B> recordParser = null;
            private IRecordParser<I, M, P, B> recordParserIfNull = null;
            private List<NodeConfiguration> childList = null;
            
            protected NodeConfiguration(final String nodeName, final ConplierBean<B> objectReference, final String idColumnName, final Class<I> idType, final NodeConfiguration parent)
            {
                super();
                this.nodeName = nodeName;
                this.objectReference = objectReference;
                this.parent = parent;
                this.idType = idType;
                this.idColumnName = idColumnName;
                this.childList = new ArrayList<>();
            }
            
            protected NodeConfiguration copy()
            {
                NodeConfiguration copy = new NodeConfiguration(this.nodeName, this.objectReference, this.idColumnName, this.idType, this.parent);
                copy.parent = null;
                copy.recordParser = this.recordParser;
                copy.recordParserIfNull = this.recordParserIfNull;
                for (final NodeConfiguration child : this.childList)
                {
                    copy.childList.add(child.copy());
                }
                return copy;
            }
            
            protected String getNodeName()
            {
                return this.nodeName;
            }
            
            protected NodeConfiguration getParent()
            {
                return this.parent;
            }
            
            protected ConplierBean<B> getObjectReference()
            {
                return this.objectReference;
            }
            
            protected Class<I> getIdType()
            {
                return this.idType;
            }
            
            protected String getIdColumnName()
            {
                return this.idColumnName;
            }
            
            protected IRecordParser<I, M, P, B> getRecordParser()
            {
                return this.recordParser;
            }
            
            protected IRecordParser<I, M, P, B> getRecordParserIfNull()
            {
                return this.recordParserIfNull;
            }
            
            protected void clear()
            {
                this.nodeName = null;
                this.parent = null;
                if (this.objectReference != null)
                {
                    this.objectReference.dispose();
                    this.objectReference = null;
                }
                this.idType = null;
                this.idColumnName = null;
                this.recordParser = null;
                this.recordParserIfNull = null;
                if (this.childList != null)
                {
                    this.childList.forEach(n -> n.clear());
                    this.childList.clear();
                    this.childList = null;
                }
            }
        }
        
    }
    
    /**
     * Close the parser helper. After this the helper is not usable anymore.
     */
    @Override
    public void close()
    {
        if (!this.closable)
        {
            return;
        }
        
        if (this.parsePhaseList != null)
        {
            this.parsePhaseList.forEach(p -> p.clear());
            this.parsePhaseList.clear();
            this.parsePhaseList = null;
        }
    }
    
    /**
     * The cursor is the access point to database and create java objects
     *
     * @param <I> type of key
     * @param <M> type of main object
     * @param <P> type of parent object
     *
     * @author Sebastian Palarus
     */
    public static class Cursor<I, M, P>
    {
        private ResultSet resultSet;
        private I id;
        private M mainObject;
        private P parentObject;
        
        /**
         * Getter for {@link ResultSet}
         *
         * @return ResultSet
         */
        public ResultSet getResultSet()
        {
            return this.resultSet;
        }
        
        protected void setResultSet(final ResultSet resultSet)
        {
            this.resultSet = resultSet;
        }
        
        /**
         * Getter for id of current data object
         *
         * @return id of current data object
         */
        public I getId()
        {
            return this.id;
        }
        
        protected void setId(final I id)
        {
            this.id = id;
        }
        
        /**
         * Getter for current main object
         *
         * @return current main object
         */
        public M getMainObject()
        {
            return this.mainObject;
        }
        
        public void setMainObject(final M mainObject)
        {
            this.mainObject = mainObject;
        }
        
        /**
         * Getter for current parent object
         *
         * @return current parent object
         */
        public P getParentObject()
        {
            return this.parentObject;
        }
        
        public void setParentObject(final P parentObject)
        {
            this.parentObject = parentObject;
        }
        
        public String getString(final String columnName) throws SQLException
        {
            return this.resultSet.getString(columnName);
        }
        
        public Short getShort(final String columnName) throws SQLException
        {
            Short value = this.resultSet.getShort(columnName);
            return this.resultSet.wasNull() ? null : value;
        }
        
        public Integer getInteger(final String columnName) throws SQLException
        {
            Integer value = this.resultSet.getInt(columnName);
            return this.resultSet.wasNull() ? null : value;
        }
        
        public Long getLong(final String columnName) throws SQLException
        {
            Long value = this.resultSet.getLong(columnName);
            return this.resultSet.wasNull() ? null : value;
        }
        
        public Float getFloat(final String columnName) throws SQLException
        {
            Float value = this.resultSet.getFloat(columnName);
            return this.resultSet.wasNull() ? null : value;
        }
        
        public Double getDouble(final String columnName) throws SQLException
        {
            Double value = this.resultSet.getDouble(columnName);
            return this.resultSet.wasNull() ? null : value;
        }
        
        public Boolean getBoolean(final String columnName) throws SQLException
        {
            Boolean value = this.resultSet.getBoolean(columnName);
            return this.resultSet.wasNull() ? null : value;
        }
        
        public Date getTimestamp(final String columnName) throws SQLException
        {
            Timestamp value = this.resultSet.getTimestamp(columnName);
            return value == null ? null : new Date(value.getTime());
        }
        
        public Date getDate(final String columnName) throws SQLException
        {
            Date value = this.resultSet.getDate(columnName);
            return value == null ? null : new Date(value.getTime());
        }
        
        public Date getTime(final String columnName) throws SQLException
        {
            Time value = this.resultSet.getTime(columnName);
            return value == null ? null : new Date(value.getTime());
        }
        
        public UUID getUUID(final String columnName) throws SQLException
        {
            return (UUID) this.resultSet.getObject(columnName);
        }
        
        public UUID getUUIDFromString(final String columnName) throws SQLException
        {
            String value = this.resultSet.getString(columnName);
            if (value == null)
            {
                return null;
            }
            return UUID.fromString(value);
        }
        
        public byte[] getBytes(final String columnName) throws SQLException
        {
            return this.resultSet.getBytes(columnName);
        }
        
        protected void clear()
        {
            this.resultSet = null;
            this.id = null;
            this.mainObject = null;
            this.parentObject = null;
        }
    }
    
    /**
     * A record parser extract data from {@link ResultSet} and provide the data object with parsed values.
     *
     * @param <I> type of key / id
     * @param <M> type of main object
     * @param <P> type of parent object
     * @param <B> type of object to provide
     *
     * @author Sebastian Palarus
     */
    @FunctionalInterface
    public interface IRecordParser<I, M, P, B> extends Function<Cursor<I, M, P>, B>
    {
        
        @Override
        default B apply(final Cursor<I, M, P> cursor)
        {
            try
            {
                return this.parse(cursor);
            }
            catch (final Exception e)
            {
                if (e instanceof RuntimeException)
                {
                    throw (RuntimeException) e;
                }
                else
                {
                    throw new RuntimeException(e);
                }
            }
        }
        
        /**
         * extract data of record and provide data object with parsed values
         *
         * @param cursor access point to related objects
         *
         * @return provided data object
         *
         * @throws Exception
         */
        B parse(Cursor<I, M, P> cursor) throws Exception;
        
    }
}
