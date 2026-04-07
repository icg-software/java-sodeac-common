/*******************************************************************************
 * Copyright (c) 2016, 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/

package org.sodeac.common.misc;

import java.util.ArrayList;
import java.util.List;

public class Version implements Comparable<Version>
{
    public static final Version DEFAULT = new Version(1, 0, 0);
    
    public Version()
    {
        super();
    }
    
    public Version(int major)
    {
        this(major, 0, 0);
    }
    
    public Version(int major, int minor)
    {
        this(major, minor, 0);
    }
    
    public Version(int major, int minor, int service)
    {
        super();
        this.major = major;
        this.minor = minor;
        this.service = service;
    }
    
    private int major = 1;
    private int minor = 0;
    private int service = 0;
    
    public int getMajor()
    {
        return major;
    }
    
    public int getMinor()
    {
        return minor;
    }
    
    public int getService()
    {
        return service;
    }
    
    public String toString()
    {
        return this.major + "." + this.minor + "." + this.service;
    }
    
    public static Version fromAnnotation(org.sodeac.common.annotation.Version version)
    {
        if (version.major() == -1 && version.minor() == -1 && version.service() == -1)
        {
            return null;
        }
        return new Version(version.major(), version.minor(), version.service());
    }
    
    public static Version fromString(String versionText)
    {
        int major = 1;
        int minor = 0;
        int service = 0;
        
        if ((versionText != null) && (!versionText.isEmpty()))
        {
            String[] splitArray = versionText.split("\\.");
            List<String> splitList = new ArrayList<String>();
            
            for (String part : splitArray)
            {
                part = part.trim();
                if (part.isEmpty())
                {
                    continue;
                }
                splitList.add(part);
            }
            
            if (splitList.size() > 0)
            {
                major = Integer.parseInt(splitList.get(0));
            }
            if (splitList.size() > 1)
            {
                minor = Integer.parseInt(splitList.get(1));
            }
            if (splitList.size() > 2)
            {
                service = Integer.parseInt(splitList.get(2));
            }
            
            splitList.clear();
            splitList = null;
            splitArray = null;
        }
        
        return new Version(major, minor, service);
    }
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + major;
        result = prime * result + minor;
        result = prime * result + service;
        return result;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        Version other = (Version) obj;
        if (major != other.major)
        {
            return false;
        }
        if (minor != other.minor)
        {
            return false;
        }
        if (service != other.service)
        {
            return false;
        }
        return true;
    }
    
    @Override
    public int compareTo(Version o)
    {
        if (o == null)
        {
            o = DEFAULT;
        }
        if (this.major != o.major)
        {
            return this.major < o.major ? -1 : 1;
        }
        if (this.minor != o.minor)
        {
            return this.minor < o.minor ? -1 : 1;
        }
        if (this.service != o.service)
        {
            return this.service < o.service ? -1 : 1;
        }
        return 0;
    }
}
