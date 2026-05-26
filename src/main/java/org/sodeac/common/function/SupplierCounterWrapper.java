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
package org.sodeac.common.function;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class SupplierCounterWrapper<T> implements Supplier<T>
{
    private Supplier<T> supplier = null;
    private AtomicLong count = null;
    
    private SupplierCounterWrapper(final Supplier<T> supplier)
    {
        super();
        this.supplier = supplier;
        this.count = new AtomicLong(0);
    }
    
    public static <T> SupplierCounterWrapper<T> forSupplier(final Supplier<T> supplier)
    {
        return new SupplierCounterWrapper<>(supplier);
    }
    
    @Override
    public T get()
    {
        T t = this.supplier.get();
        this.count.incrementAndGet();
        return t;
    }
    
    public long getSuppliedCount()
    {
        return this.count.get();
    }
    
}
