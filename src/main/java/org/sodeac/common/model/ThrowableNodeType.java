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
package org.sodeac.common.model;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import org.sodeac.common.annotation.BowMethod;
import org.sodeac.common.annotation.BowParameter;
import org.sodeac.common.annotation.BowMethod.ReturnBowMode;
import org.sodeac.common.annotation.BowParameter.AutomaticConsumer;
import org.sodeac.common.annotation.GenerateBow;
import org.sodeac.common.typedtree.BranchNode;
import org.sodeac.common.typedtree.BranchNodeMetaModel;
import org.sodeac.common.typedtree.BranchNodeType;
import org.sodeac.common.typedtree.LeafNodeType;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.TypedTreeMetaModel;
import org.sodeac.common.typedtree.TypedTreeMetaModel.RootBranchNode;
import org.sodeac.common.typedtree.annotation.IgnoreIfFalse;
import org.sodeac.common.typedtree.annotation.IgnoreIfNull;
import org.sodeac.common.typedtree.annotation.Transient;
import org.sodeac.common.typedtree.annotation.TypedTreeModel;

@TypedTreeModel(modelClass=CoreTreeModel.class)
@GenerateBow
public class ThrowableNodeType extends BranchNodeMetaModel 
{
	static{ModelRegistry.getBranchNodeMetaModel(ThrowableNodeType.class);}
	
	@Transient
	public static volatile LeafNodeType<ThrowableNodeType,Throwable> origin;
	
	@XmlAttribute(name="class")
	public static volatile LeafNodeType<ThrowableNodeType,String> className;
	
	@XmlElement(name="Message")
	public static volatile LeafNodeType<ThrowableNodeType,String> message;
	
	@XmlElement(name="Stacktrace")
	public static volatile BranchNodeType<ThrowableNodeType,StacktraceNodeType> stacktrace;
	
	@XmlElement(name="Cause")
	@IgnoreIfNull
	public static volatile BranchNodeType<ThrowableNodeType,ThrowableNodeType> cause;
	
	@XmlElement(name="Next")
	@IgnoreIfNull
	public static volatile BranchNodeType<ThrowableNodeType,ThrowableNodeType> next;
	
	@XmlElement(name="State")
	@IgnoreIfNull
	public static volatile LeafNodeType<ThrowableNodeType,String> state;
	
	@XmlAttribute(name="code")
	@IgnoreIfNull
	public static volatile LeafNodeType<ThrowableNodeType,Long> code;
	
	@XmlAttribute(name="errorinstance")
	@IgnoreIfFalse
	public static volatile LeafNodeType<ThrowableNodeType,Boolean> isError;
	
	@XmlAttribute(name="runtimeinstance")
	@IgnoreIfFalse
	public static volatile LeafNodeType<ThrowableNodeType,Boolean> isRuntimeException;
	
	public static RootBranchNode<CoreTreeModel,StacktraceNodeType> nodeFromStacktrace(StackTraceElement[] stacktrace)
	{
		return nodeFromStacktrace(stacktrace,null);
	}
	
	@BowMethod(convertReturnValueToBow=true,keepStatic=true,returnBowMode=ReturnBowMode.UNDEFINED_PARENT_TYPE)
	protected static RootBranchNode<CoreTreeModel,StacktraceNodeType> nodeFromStacktrace(StackTraceElement[] stacktrace,@BowParameter(automaticConsumerMode=AutomaticConsumer.NEW_BOW_BY_RETURNTYPE) Consumer<RootBranchNode<CoreTreeModel,StacktraceNodeType>> onRootNodeCreated)
	{
		RootBranchNode<CoreTreeModel,StacktraceNodeType> stacktraceNode = TypedTreeMetaModel.getInstance(CoreTreeModel.class).createRootNode(CoreTreeModel.stacktrace);
		if(onRootNodeCreated != null)
		{
			onRootNodeCreated.accept(stacktraceNode);
		}
		for(StackTraceElement stackTraceElement :  stacktrace)
		{
			stacktraceNode.create(StacktraceNodeType.elements)
				.setValue(StacktraceElementNodeType.className, stackTraceElement.getClassName())
				.setValue(StacktraceElementNodeType.fileName, stackTraceElement.getFileName())
				.setValue(StacktraceElementNodeType.methodName, stackTraceElement.getMethodName())
				.setValue(StacktraceElementNodeType.lineNumber, stackTraceElement.getLineNumber())
				.setValue(StacktraceElementNodeType.nativeMethod, stackTraceElement.isNativeMethod());
		}
		return stacktraceNode;
	}
	
	public static RootBranchNode<CoreTreeModel,ThrowableNodeType> nodeFromThrowable(Throwable throwable)
	{
		return nodeFromThrowable(throwable,null);
	}
	
	@BowMethod(convertReturnValueToBow=true,keepStatic=true,returnBowMode=ReturnBowMode.UNDEFINED_PARENT_TYPE)
	protected static RootBranchNode<CoreTreeModel,ThrowableNodeType> nodeFromThrowable(Throwable throwable, @BowParameter(automaticConsumerMode=AutomaticConsumer.NEW_BOW_BY_RETURNTYPE) Consumer<RootBranchNode<CoreTreeModel,ThrowableNodeType>> onRootNodeCreated)
	{
		if(throwable == null)
		{
			return null;
		}
		Set<Throwable> doneIndex = new HashSet<Throwable>();
		
		RootBranchNode<CoreTreeModel,ThrowableNodeType> throwableNode = TypedTreeMetaModel.getInstance(CoreTreeModel.class).createRootNode(CoreTreeModel.throwable);
		if(onRootNodeCreated != null)
		{
			onRootNodeCreated.accept(throwableNode);
		}
		throwableNode.setValue(ThrowableNodeType.origin, throwable);
		doneIndex.add(throwable);
		recursiveConvertThrowable(throwableNode, throwable, doneIndex);
		return throwableNode;
	}
	
	private static void recursiveConvertThrowable(BranchNode<?,ThrowableNodeType> throwableNode, Throwable throwable, Set<Throwable> doneIndex)
	{
		throwableNode.setValue(ThrowableNodeType.className, throwable.getClass().getCanonicalName());
		throwableNode.setValue(ThrowableNodeType.message, throwable.getMessage());
		throwableNode.setValue(ThrowableNodeType.isError,throwable instanceof Error);
		throwableNode.setValue(ThrowableNodeType.isRuntimeException,throwable instanceof RuntimeException);
		if(throwable.getStackTrace() != null)
		{
			BranchNode<ThrowableNodeType,StacktraceNodeType> stacktraceNode = throwableNode.create(ThrowableNodeType.stacktrace);
			for(StackTraceElement stackTraceElement :  throwable.getStackTrace())
			{
				stacktraceNode.create(StacktraceNodeType.elements)
					.setValue(StacktraceElementNodeType.className, stackTraceElement.getClassName())
					.setValue(StacktraceElementNodeType.fileName, stackTraceElement.getFileName())
					.setValue(StacktraceElementNodeType.methodName, stackTraceElement.getMethodName())
					.setValue(StacktraceElementNodeType.lineNumber, stackTraceElement.getLineNumber())
					.setValue(StacktraceElementNodeType.nativeMethod, stackTraceElement.isNativeMethod());
			}
		}
		
		if(throwable instanceof SQLException)
		{
			SQLException sqlException = (SQLException) throwable;
			throwableNode.setValue(ThrowableNodeType.state,sqlException.getSQLState());
			throwableNode.setValue(ThrowableNodeType.code,(long)sqlException.getErrorCode());
			
			if((sqlException.getNextException() != null) && (! doneIndex.contains(sqlException.getNextException())))
			{
				doneIndex.add(sqlException.getNextException());
				
				BranchNode<ThrowableNodeType,ThrowableNodeType> nextNode = throwableNode.create(ThrowableNodeType.next);
				recursiveConvertThrowable(nextNode, sqlException.getNextException(), doneIndex);
			}
		}
		
		if((throwable.getCause() != null) && (! doneIndex.contains(throwable.getCause())))
		{
			doneIndex.add(throwable.getCause());
			
			BranchNode<ThrowableNodeType,ThrowableNodeType> causeNode = throwableNode.create(ThrowableNodeType.cause);
			recursiveConvertThrowable(causeNode, throwable.getCause(), doneIndex);
		}
	}
	
}
