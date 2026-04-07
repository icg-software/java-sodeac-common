/*******************************************************************************
 * Copyright (c) 2016, 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.xuri;

import java.util.List;

/**
 * Interface for extension capabilities
 * 
 * @author Sebastian Palarus
 *
 */
public interface IExtensible
{
	/**
	 * getter for single extension
	 * 
	 * @param type extensiontype
	 * @return extension
	 */
	public IExtension<?> getExtension(String type);
	
	/**
	 * getter for all extensions
	 * @return extension list
	 */
	public List<IExtension<?>> getExtensionList();
	
	/**
	 * getter for extension list of defined type
	 * @param type filter
	 * @return filtered extension list 
	 */
	public List<IExtension<?>> getExtensionList(String type);
}
