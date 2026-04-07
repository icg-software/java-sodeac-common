/*******************************************************************************
 * Copyright (c) 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.synchronization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.sql.DataSource;

import org.sodeac.common.jdbc.TypedTreeJDBCCruder;
import org.sodeac.common.jdbc.TypedTreeJDBCCruder.Session;
import org.sodeac.common.typedtree.BranchNode;
import org.sodeac.common.typedtree.BranchNodeMetaModel;

public class TypedTreeJDBCSynchronization<T extends BranchNodeMetaModel> implements AutoCloseable
{
    private TypedTreeJDBCSynchronization()
    {
        super();
    }
    
    public static <T extends BranchNodeMetaModel> TTSBuilder<T> newSynchronisationForType(Class<T> clazz)
    {
        return new TTSBuilder<T>();
    }
    
    public static class FindLocalNodesContext<T extends BranchNodeMetaModel>
    {
        private Collection<BranchNode<? extends BranchNodeMetaModel, T>> chunk = null;
        private Session session = null;
        private Map<BranchNode<? extends BranchNodeMetaModel, T>, BranchNode<? extends BranchNodeMetaModel, T>> pairs = null;
        
        private void close()
        {
            this.chunk = null;
            this.session = null;
            
            if (pairs != null)
            {
                pairs.clear();
            }
            pairs = null;
        }
        
        public Collection<BranchNode<? extends BranchNodeMetaModel, T>> getChunk()
        {
            return chunk;
        }
        
        public Session getSession()
        {
            return session;
        }
        
        public void definePair(BranchNode<? extends BranchNodeMetaModel, T> remote, BranchNode<? extends BranchNodeMetaModel, T> local)
        {
            if (pairs == null)
            {
                pairs = new HashMap<>();
            }
            this.pairs.put(remote, local);
        }
        
    }
    
    public static class CheckUpToDateContext<T extends BranchNodeMetaModel>
    {
        private BranchNode<? extends BranchNodeMetaModel, T> remote = null;
        private BranchNode<? extends BranchNodeMetaModel, T> local = null;
        
        private void close()
        {
            this.remote = null;
            this.local = null;
        }
        
        public BranchNode<? extends BranchNodeMetaModel, T> getRemote()
        {
            return remote;
        }
        
        public BranchNode<? extends BranchNodeMetaModel, T> getLocal()
        {
            return local;
        }
        
    }
    
    public static class LocalNodeFactoryContext<T extends BranchNodeMetaModel>
    {
        private BranchNode<? extends BranchNodeMetaModel, T> remote = null;
        
        private void close()
        {
            this.remote = null;
        }
        
        public BranchNode<? extends BranchNodeMetaModel, T> getRemote()
        {
            return remote;
        }
    }
    
    public static class UpdateLocalNodeContext<T extends BranchNodeMetaModel>
    {
        private BranchNode<? extends BranchNodeMetaModel, T> remote = null;
        private BranchNode<? extends BranchNodeMetaModel, T> local = null;
        
        private void close()
        {
            this.remote = null;
            this.local = null;
        }
        
        public BranchNode<? extends BranchNodeMetaModel, T> getRemote()
        {
            return remote;
        }
        
        public BranchNode<? extends BranchNodeMetaModel, T> getLocal()
        {
            return local;
        }
        
    }
    
    public static class PersistLocalNodesContext<T extends BranchNodeMetaModel>
    {
        private List<BranchNode<? extends BranchNodeMetaModel, T>> updateList = null;
        private Session session = null;
        
        private void close()
        {
            if (this.updateList != null)
            {
                this.updateList.clear();
                this.updateList = null;
            }
            this.session = null;
        }
        
        public List<BranchNode<? extends BranchNodeMetaModel, T>> getUpdateList()
        {
            return updateList;
        }
        
        public Session getSession()
        {
            return session;
        }
    }
    
    public static class DisposeChunkPhaseContext<T extends BranchNodeMetaModel>
    {
        private Collection<BranchNode<? extends BranchNodeMetaModel, T>> chunk = null;
        private Map<BranchNode<? extends BranchNodeMetaModel, T>, BranchNode<? extends BranchNodeMetaModel, T>> pairs = null;
        private List<BranchNode<? extends BranchNodeMetaModel, T>> updateList = null;
        private List<BranchNode<? extends BranchNodeMetaModel, T>> createdList = null;
        
        private void close()
        {
            this.chunk = null;
            this.pairs = null;
            this.updateList = null;
            this.createdList = null;
        }
        
        public Collection<BranchNode<? extends BranchNodeMetaModel, T>> getChunk()
        {
            return chunk;
        }
        
        public Map<BranchNode<? extends BranchNodeMetaModel, T>, BranchNode<? extends BranchNodeMetaModel, T>> getPairs()
        {
            return pairs;
        }
        
        public List<BranchNode<? extends BranchNodeMetaModel, T>> getUpdateList()
        {
            return updateList;
        }
        
        public List<BranchNode<? extends BranchNodeMetaModel, T>> getCreatedList()
        {
            return createdList;
        }
        
    }
    
    private FindLocalNodesContext<T> finderContext = new FindLocalNodesContext<>();
    private CheckUpToDateContext<T> checkUpToDateContext = new CheckUpToDateContext<>();
    private LocalNodeFactoryContext<T> locaNodeFactoryContext = new LocalNodeFactoryContext<>();
    private UpdateLocalNodeContext<T> updateLocalNodeContext = new UpdateLocalNodeContext<>();
    private PersistLocalNodesContext<T> persistLocalNodesContext = new PersistLocalNodesContext<>();
    private DisposeChunkPhaseContext<T> disposeChunkPhaseContext = new DisposeChunkPhaseContext<>();
    
    private DataSource dataSource = null;
    private TypedTreeJDBCCruder cruder = null;
    private volatile Session session = null;
    private Consumer<FindLocalNodesContext<T>> finder = null;
    private Function<CheckUpToDateContext<T>, Boolean> checker = null;
    private Function<LocalNodeFactoryContext<T>, BranchNode<? extends BranchNodeMetaModel, T>> factory = null;
    private Consumer<UpdateLocalNodeContext<T>> update = null;
    private Consumer<PersistLocalNodesContext<T>> persist = null;
    private Consumer<DisposeChunkPhaseContext<T>> dispose = null;
    
    private void checkSession()
    {
        if (this.session == null)
        {
            synchronized (this)
            {
                if (this.session == null)
                {
                    this.session = this.cruder.openSession(dataSource);
                }
            }
        }
    }
    
    public TypedTreeJDBCSynchronization<T> pushChunk(Collection<BranchNode<? extends BranchNodeMetaModel, T>> chunk)
    {
        if (chunk == null)
        {
            return this;
        }
        if (chunk.isEmpty())
        {
            return this;
        }
        
        this.checkSession();
        
        List<BranchNode<? extends BranchNodeMetaModel, T>> updateList = new ArrayList<>();
        List<BranchNode<? extends BranchNodeMetaModel, T>> createdList = new ArrayList<>();
        Map<BranchNode<? extends BranchNodeMetaModel, T>, BranchNode<? extends BranchNodeMetaModel, T>> pairs = null;
        
        this.finderContext.close();
        this.finderContext.chunk = chunk;
        this.finderContext.session = this.session;
        try
        {
            this.finder.accept(this.finderContext);
            pairs = this.finderContext.pairs == null ? new HashMap<>() : new HashMap<>(this.finderContext.pairs);
            
            for (BranchNode<? extends BranchNodeMetaModel, T> remote : chunk)
            {
                try
                {
                    BranchNode<? extends BranchNodeMetaModel, T> local = pairs.get(remote);
                    if (local != null)
                    {
                        this.checkUpToDateContext.close();
                        try
                        {
                            this.checkUpToDateContext.local = local;
                            this.checkUpToDateContext.remote = remote;
                            if (checker.apply(this.checkUpToDateContext).booleanValue())
                            {
                                continue;
                            }
                        }
                        finally
                        {
                            this.checkUpToDateContext.close();
                        }
                    }
                    else
                    {
                        this.locaNodeFactoryContext.close();
                        this.locaNodeFactoryContext.remote = remote;
                        try
                        {
                            local = this.factory.apply(this.locaNodeFactoryContext);
                        }
                        finally
                        {
                            this.locaNodeFactoryContext.close();
                        }
                        if (local != null)
                        {
                            createdList.add(local);
                        }
                    }
                    this.updateLocalNodeContext.close();
                    this.updateLocalNodeContext.local = local;
                    this.updateLocalNodeContext.remote = remote;
                    try
                    {
                        this.update.accept(this.updateLocalNodeContext);
                        if (this.updateLocalNodeContext.local != null)
                        {
                            updateList.add(this.updateLocalNodeContext.local);
                        }
                    }
                    finally
                    {
                        this.updateLocalNodeContext.close();
                    }
                }
                catch (Exception e)
                {
                    // TODO: handle exception
                    e.printStackTrace();
                }
            }
            
            this.checkSession();
            this.persistLocalNodesContext.close();
            this.persistLocalNodesContext.updateList = updateList;
            this.persistLocalNodesContext.session = this.session;
            try
            {
                this.persist.accept(this.persistLocalNodesContext);
            }
            finally
            {
                this.persistLocalNodesContext.close();
            }
        }
        finally
        {
            this.finderContext.close();
        }
        
        this.disposeChunkPhaseContext.close();
        try
        {
            this.disposeChunkPhaseContext.pairs = pairs;
            this.disposeChunkPhaseContext.chunk = chunk;
            this.disposeChunkPhaseContext.updateList = updateList;
            this.disposeChunkPhaseContext.createdList = createdList;
            
            this.dispose.accept(this.disposeChunkPhaseContext);
        }
        finally
        {
            this.disposeChunkPhaseContext.close();
            
            try
            {
                updateList.clear();
            }
            catch (Exception | Error e) { }
            
            try
            {
                createdList.clear();
            }
            catch (Exception | Error e) { }
            
            try
            {
                if (pairs != null)
                {
                    pairs.clear();
                }
            }
            catch (Exception | Error e) { }
        }
        return this;
    }
    
    @Override
    public void close() throws Exception
    {
        try
        {
            Session session = this.session;
            
            if (session != null)
            {
                this.session = null;
                session.close();
            }
        }
        catch (Exception | Error e) { }
        
        try
        {
            this.finderContext.close();
        }
        catch (Exception | Error e) { }
        
        try
        {
            this.checkUpToDateContext.close();
        }
        catch (Exception | Error e) { }
        
        try
        {
            this.locaNodeFactoryContext.close();
        }
        catch (Exception | Error e) { }
        
        try
        {
            this.updateLocalNodeContext.close();
        }
        catch (Exception | Error e) { }
        
        try
        {
            this.persistLocalNodesContext.close();
        }
        catch (Exception | Error e) { }
        
        try
        {
            this.disposeChunkPhaseContext.close();
        }
        catch (Exception | Error e) { }
    }
    
    public static class TTSBuilder<T extends BranchNodeMetaModel>
    {
        private TTSBuilder()
        {
            super();
        }
        
        private Consumer<FindLocalNodesContext<T>> finder = null;
        private Function<CheckUpToDateContext<T>, Boolean> checker = null;
        private Function<LocalNodeFactoryContext<T>, BranchNode<? extends BranchNodeMetaModel, T>> factory = null;
        private Consumer<UpdateLocalNodeContext<T>> update = null;
        private Consumer<PersistLocalNodesContext<T>> persist = null;
        private Consumer<DisposeChunkPhaseContext<T>> dispose = null;
        
        public TTSBuilder2 findLocalNodes(Consumer<FindLocalNodesContext<T>> finder)
        {
            Objects.requireNonNull(finder);
            TTSBuilder.this.finder = finder;
            return new TTSBuilder2();
        }
        
        public class TTSBuilder2
        {
            private TTSBuilder2()
            {
                super();
            }
            
            public TTSBuilder3 checkUpToDate(Function<CheckUpToDateContext<T>, Boolean> checker)
            {
                Objects.requireNonNull(checker);
                TTSBuilder.this.checker = checker;
                return new TTSBuilder3();
            }
        }
        
        public class TTSBuilder3
        {
            private TTSBuilder3()
            {
                super();
            }
            
            public TTSBuilder4 localNodeFactory(Function<LocalNodeFactoryContext<T>, BranchNode<? extends BranchNodeMetaModel, T>> factory)
            {
                Objects.requireNonNull(factory);
                TTSBuilder.this.factory = factory;
                return new TTSBuilder4();
            }
            
        }
        
        public class TTSBuilder4
        {
            private TTSBuilder4()
            {
                super();
            }
            
            public TTSBuilder5 updateLocalNode(Consumer<UpdateLocalNodeContext<T>> update)
            {
                Objects.requireNonNull(update);
                TTSBuilder.this.update = update;
                return new TTSBuilder5();
            }
        }
        
        public class TTSBuilder5
        {
            private TTSBuilder5()
            {
                super();
            }
            
            public TTSBuilder6 persistLocalNodes(Consumer<PersistLocalNodesContext<T>> persist)
            {
                Objects.requireNonNull(persist);
                TTSBuilder.this.persist = persist;
                return new TTSBuilder6();
            }
        }
        
        public class TTSBuilder6
        {
            private TTSBuilder6()
            {
                super();
            }
            
            public PreparedTTSBuilder disposeChunkPhase(Consumer<DisposeChunkPhaseContext<T>> dispose)
            {
                if (dispose == null)
                {
                    dispose = ctx -> { };
                }
                TTSBuilder.this.dispose = dispose;
                return new PreparedTTSBuilder();
            }
        }
        
        public class PreparedTTSBuilder
        {
            private PreparedTTSBuilder()
            {
                super();
            }
            
            public TypedTreeJDBCSynchronization<T> buildForDatasource(DataSource dataSource)
            {
                Objects.requireNonNull(dataSource);
                TypedTreeJDBCSynchronization<T> synchronization = new TypedTreeJDBCSynchronization<T>();
                
                synchronization.dataSource = dataSource;
                synchronization.finder = TTSBuilder.this.finder;
                synchronization.checker = TTSBuilder.this.checker;
                synchronization.factory = TTSBuilder.this.factory;
                synchronization.update = TTSBuilder.this.update;
                synchronization.persist = TTSBuilder.this.persist;
                synchronization.dispose = TTSBuilder.this.dispose;
                
                synchronization.cruder = TypedTreeJDBCCruder.get();
                
                return synchronization;
            }
        }
    }
}
