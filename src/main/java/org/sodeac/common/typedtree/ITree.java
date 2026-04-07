/*******************************************************************************
 * Copyright (c) 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.typedtree;

import org.sodeac.common.typedtree.TypedTreeMetaModel.RootBranchNode;

/**
 * A typed tree is a generic tree corresponds with a meta model
 * 
 * @author Sebastian Palarus
 *
 * @param <P> parent model type
 * @param <R> type of root node
 */
public interface ITree<P extends TypedTreeMetaModel,R  extends BranchNodeMetaModel>
{
	/**
	 * Dispose complete tree.
	 */
	public void dispose();
	
	/**
	 * Getter for synchronized option 
	 * 
	 * @return true, if access to tree is synchronized, otherwise false
	 */
	public boolean isSynchronized();
	
	/**
	 * Setter for synchronized option. This option determines, whether access to tree is synchronized or not.
	 * 
	 * @param nodeSynchronized synchronized option to set
	 * @return root node
	 */
	public RootBranchNode<P,R> setSynchronized(boolean nodeSynchronized);
	
	/**
	 * Getter for immutable option.
	 * 
	 * @return true, if tree is immutable, otherwise false
	 */
	public boolean isImmutable();
	
	/**
	 * Sets tree immutable. After this the tree structure can not be changed any more.
	 * 
	 * @return root node
	 */
	public RootBranchNode<P,R> setImmutable();
	
	/**
	 * Getter for getter-auto-create-option. This option determines, whether {@link BranchNode#get(BranchNodeType)} will automatically create child nodes, if requested node is null.
	 * 
	 * @return getter-auto-create-option of child nodes
	 */
	public boolean isBranchNodeGetterAutoCreate();
	
	/**
	 *  Setter for getter-auto-create-option. This option determines, whether {@link BranchNode#get(BranchNodeType)} will automatically create child nodes, if requested node is null.
	 *  
	 * @param branchNodeGetterAutoCreate getter-auto-create-option of child node
	 * @return root node
	 */
	public RootBranchNode<P,R> setBranchNodeGetterAutoCreate(boolean branchNodeGetterAutoCreate);
	
	
	/**
	 * Getter for apply-to-consumer-auto-create-option. This option determines, whether {@link BranchNode#applyToConsumer(BranchNodeType, java.util.function.BiConsumer)} and 
	 *  {@link BranchNode#applyToConsumer(BranchNodeType, java.util.function.BiConsumer, java.util.function.BiConsumer)} will automatically create child nodes, if requested node is null.
	 * 
	 * @return apply-to-consumer-auto-create-option
	 */
	public boolean isBranchNodeApplyToConsumerAutoCreate();
	
	/**
	 *  Setter for apply-to-consumer-auto-create-option of child nodes. This option determines, whether {@link BranchNode#applyToConsumer(BranchNodeType, java.util.function.BiConsumer)} and 
	 *  {@link BranchNode#applyToConsumer(BranchNodeType, java.util.function.BiConsumer, java.util.function.BiConsumer)} will automatically create child nodes, if requested node is null.
	 *  
	 * @param branchNodeApplyToConsumerAutoCreate apply-to-consumer-auto-create-option of child node
	 * @return root node
	 */
	public RootBranchNode<P,R> setBranchNodeApplyToConsumerAutoCreate(boolean branchNodeApplyToConsumerAutoCreate);
	
	/**
	 * Adds modify listener to tree.
	 * 
	 * @param modifyListener modify listener to add
	 * @return root node
	 */
	public RootBranchNode<P,R> addTreeModifyListener(ITreeModifyListener modifyListener);
	
	/**
	 * Adds modify listeners to tree.
	 * 
	 * @param modifyListeners modify listeners to add
	 * @return root node
	 */
	public RootBranchNode<P,R> addTreeModifyListeners(ITreeModifyListener... modifyListeners);
	
	/**
	 * Removes modify listener to tree.
	 * 
	 * @param modifyListener modify listeners to remove
	 * @return root node
	 */
	public RootBranchNode<P,R> removeTreeModifyListener(ITreeModifyListener modifyListener);
	
	public boolean isDisableAllListener();
	
	public RootBranchNode<P,R> setDisableAllListener(boolean disableAllListener);
}
