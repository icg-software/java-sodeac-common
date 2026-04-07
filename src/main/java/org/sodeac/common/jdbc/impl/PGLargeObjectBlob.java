/*******************************************************************************
 * Copyright (c) 2018, 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.jdbc.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;

public class PGLargeObjectBlob implements Blob
{
    private LargeObject largeObject;
    private final org.postgresql.core.BaseConnection nativeConnection;
    private long oid = -1;
    private boolean isFree = false;
    private boolean writable = false;
    private final List<LargeObject> usedList = new ArrayList<LargeObject>();
    
    public PGLargeObjectBlob(final org.postgresql.core.BaseConnection nativeConnection, final long oid)
    {
        super();
        this.oid = oid;
        this.nativeConnection = nativeConnection;
        this.writable = false;
    }
    
    public PGLargeObjectBlob(final org.postgresql.core.BaseConnection nativeConnection) throws SQLException
    {
        super();
        this.nativeConnection = nativeConnection;
        this.writable = true;
        this.largeObject = createLargeObject();
        this.oid = this.largeObject.getLongOID();
    }
    
    @Override
    public long length() throws SQLException
    {
        if (this.isFree)
        {
            throw new SQLException("Blob is free");
        }
        checkLargeObject();
        return this.largeObject.size64();
    }
    
    @Override
    public byte[] getBytes(final long pos, final int length) throws SQLException
    {
        if (this.isFree)
        {
            throw new SQLException("Blob is free");
        }
        checkLargeObject();
        this.largeObject.seek64(pos - 1L, LargeObject.SEEK_SET);
        byte[] ret = this.largeObject.read(length);
        this.usedList.add(this.largeObject);
        this.largeObject = null;
        return ret;
    }
    
    @Override
    public InputStream getBinaryStream() throws SQLException
    {
        if (this.isFree)
        {
            throw new SQLException("Blob is free");
        }
        checkLargeObject();
        InputStream is = this.largeObject.getInputStream();
        this.usedList.add(this.largeObject);
        this.largeObject = null;
        return is;
    }
    
    @Override
    public long position(final byte[] pattern, final long start) throws SQLException
    {
        if (this.isFree)
        {
            throw new SQLException("Blob is free");
        }
        if (pattern == null)
        {
            return -1;
        }
        
        if (pattern.length == 0)
        {
            return 1;
        }
        
        InputStream is = start == 1 ? getBinaryStream() : getBinaryStream(start, length());
        try
        {
            byte[] buffer = new byte[1080];
            int len;
            int matchedLength = 0;
            long matchStart = -1;
            long pos = 0;
            while ((len = is.read(buffer)) > 0)
            {
                for (int i = 0; i < len; i++)
                {
                    pos++;
                    
                    if (buffer[i] == pattern[matchedLength])
                    {
                        matchedLength++;
                    }
                    else
                    {
                        matchStart = -1;
                        matchedLength = 0;
                        continue;
                    }
                    
                    if (matchedLength == 1)
                    {
                        matchStart = pos;
                    }
                    
                    if (matchedLength == pattern.length)
                    {
                        return matchStart;
                    }
                }
            }
        }
        catch (final Exception e)
        {
            throw new SQLException(e.getMessage(), e);
        }
        finally
        {
            try
            {
                is.close();
            }
            catch (final Exception e) { }
        }
        return -1;
    }
    
    @Override
    public long position(final Blob pattern, final long start) throws SQLException
    {
        if (this.isFree)
        {
            throw new SQLException("Blob is free");
        }
        return position(pattern.getBytes(1, (int) pattern.length()), start);
    }
    
    @Override
    public int setBytes(final long pos, final byte[] bytes) throws SQLException
    {
        if (this.isFree)
        {
            throw new SQLException("Blob is free");
        }
        return setBytes(pos, bytes, 0, bytes.length);
    }
    
    @Override
    public int setBytes(final long pos, final byte[] bytes, final int offset, final int len) throws SQLException
    {
        if (this.isFree)
        {
            throw new SQLException("Blob is free");
        }
        checkLargeObject();
        this.largeObject.seek64(pos - 1L, LargeObject.SEEK_SET);
        this.largeObject.write(bytes, offset, len);
        this.usedList.add(this.largeObject);
        this.largeObject = null;
        return len;
    }
    
    @Override
    public OutputStream setBinaryStream(final long pos) throws SQLException
    {
        if (this.isFree)
        {
            throw new SQLException("Blob is free");
        }
        checkLargeObject();
        
        if (pos != 1)
        {
            this.largeObject.seek64(pos - 1L, LargeObject.SEEK_SET);
        }
        OutputStream os = this.largeObject.getOutputStream();
        this.usedList.add(this.largeObject);
        this.largeObject = null;
        return os;
    }
    
    @Override
    public void truncate(final long len) throws SQLException
    {
        if (this.isFree)
        {
            throw new SQLException("Blob is free");
        }
        checkLargeObject();
        this.largeObject.truncate64(len);
        this.usedList.add(this.largeObject);
        this.largeObject = null;
    }
    
    @Override
    public void free() throws SQLException
    {
        if (this.isFree)
        {
            return;
        }
        this.isFree = true;
        try
        {
            if (this.largeObject != null)
            {
                this.largeObject.close();
            }
        }
        catch (final Exception e) { }
        for (final LargeObject used : this.usedList)
        {
            try
            {
                used.close();
            }
            catch (final Exception e) { }
        }
        this.largeObject = null;
    }
    
    @Override
    public InputStream getBinaryStream(final long pos, final long length) throws SQLException
    {
        if (this.isFree)
        {
            throw new SQLException("Blob is free");
        }
        checkLargeObject();
        
        if (pos != 1)
        {
            this.largeObject.seek64(pos - 1L, LargeObject.SEEK_SET);
        }
        InputStream is = this.largeObject.getInputStream();
        this.usedList.add(this.largeObject);
        this.largeObject = null;
        return is;
    }
    
    public long getOID()
    {
        return this.oid;
    }
    
    private void checkLargeObject() throws SQLException
    {
        if (this.largeObject == null)
        {
            LargeObjectManager lobj = this.nativeConnection.getLargeObjectAPI();
            this.largeObject = lobj.open(this.oid, this.writable ? LargeObjectManager.READWRITE : LargeObjectManager.READ);
        }
    }
    
    private LargeObject createLargeObject() throws SQLException
    {
        LargeObjectManager lobj = this.nativeConnection.getLargeObjectAPI();
        long oid = lobj.createLO(LargeObjectManager.READ | LargeObjectManager.WRITE);
        LargeObject obj = lobj.open(oid, LargeObjectManager.READWRITE);
        return obj;
    }
    
    public boolean isWritable()
    {
        return this.writable;
    }
    
}
