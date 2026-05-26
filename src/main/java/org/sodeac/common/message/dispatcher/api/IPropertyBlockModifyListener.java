/*******************************************************************************
 * Copyright (c) 2018, 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.message.dispatcher.api;

import java.util.List;

public interface IPropertyBlockModifyListener
{
    enum ModifyType
    {INSERT, UPDATE, REMOVE}
    
    void onModify(ModifyType type, String key, Object valueOld, Object valueNew);
    
    void onModifySet(List<PropertyBlockModifyItem> modifySet);
}
