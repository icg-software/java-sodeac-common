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
package org.sodeac.common.impl;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sodeac.common.jdbc.IColumnType;
import org.sodeac.common.jdbc.IDBSchemaUtilsDriver;
import org.sodeac.common.jdbc.schemax.IDefaultBySequence;
import org.sodeac.common.jdbc.schemax.IDefaultCurrentDate;
import org.sodeac.common.jdbc.schemax.IDefaultCurrentTime;
import org.sodeac.common.jdbc.schemax.IDefaultCurrentTimestamp;
import org.sodeac.common.jdbc.schemax.IDefaultStaticValue;
import org.sodeac.common.message.dispatcher.api.IDispatcherChannelSystemManager;
import org.sodeac.common.message.dispatcher.impl.MessageDispatcherManagerComponent;
import org.sodeac.common.misc.OSGiDriverRegistry;
import org.sodeac.common.xuri.IExtension;
import org.sodeac.common.xuri.json.JsonExtension;
import org.sodeac.common.xuri.ldapfilter.LDAPFilterExtension;

@Component(service = InternalDriverLoaded.class)
public class InternalDriverLoaded
{
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    protected volatile OSGiDriverRegistry internalBootstrapDep;
    
    // XURI
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, target = "(type=" + JsonExtension.TYPE + ")")
    protected volatile IExtension<?> jsonExtension;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, target = "(type=" + LDAPFilterExtension.TYPE + ")")
    protected volatile IExtension<?> ldapFilterExtension;
    
    // DBSchema
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, target = "(defaultdriver=true)")
    protected volatile IColumnType defaultColumnType;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, target = "(defaultdriver=true)")
    protected volatile IDefaultCurrentDate defaultCurrentDate;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, target = "(defaultdriver=true)")
    protected volatile IDefaultCurrentTime defaultCurrentTime;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, target = "(defaultdriver=true)")
    protected volatile IDefaultCurrentTimestamp defaultCurrentTimestamp;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, target = "(defaultdriver=true)")
    protected volatile IDefaultStaticValue defaultStaticValue;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, target = "(&(defaultdriver=true)(type=h2))")
    protected volatile IDBSchemaUtilsDriver h2DBUtils;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, target = "(&(defaultdriver=true)(type=h2))")
    protected volatile IDefaultBySequence h2DefaultBySequence;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, target = "(&(defaultdriver=true)(type=postgresql))")
    protected volatile IDBSchemaUtilsDriver pgDBUtils;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, target = "(&(defaultdriver=true)(type=postgresql))")
    protected volatile IDefaultBySequence pgDefaultBySequence;
    
    // Message Dispatcher Components
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    protected volatile MessageDispatcherManagerComponent messageDispatcherManagerComponent;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, target = "(&(type=consume-messages)(role=consumer))")
    protected volatile IDispatcherChannelSystemManager consumeMessagesConsumerManager;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, target = "(&(type=consume-messages)(role=planner))")
    protected volatile IDispatcherChannelSystemManager consumeMessagesPlannerManager;
    
}
