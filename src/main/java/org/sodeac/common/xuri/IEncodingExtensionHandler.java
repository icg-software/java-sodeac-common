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

/**
 * An encoding extension handler encodes an extension data object of type {@link T} to formated string
 *
 * @param <T>
 *
 * @author Sebastian Palarus
 * @version 1.0
 * @since 1.0
 */
public interface IEncodingExtensionHandler<T>
{
    /**
     * extension type of encoding extension handler
     *
     * @return
     */
    public String getType();
    
    /**
     * encodes extension object of type {@link T} to string encoded extension
     *
     * @param extensionDataObject
     *
     * @return
     */
    public String encodeToString(T extensionDataObject);
}
