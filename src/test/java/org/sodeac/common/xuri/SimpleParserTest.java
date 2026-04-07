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
package org.sodeac.common.xuri;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.sodeac.common.xuri.ldapfilter.ComparativeOperator;
import org.sodeac.common.xuri.ldapfilter.Criteria;
import org.sodeac.common.xuri.ldapfilter.CriteriaLinker;
import org.sodeac.common.xuri.ldapfilter.IFilterItem;
import org.sodeac.common.xuri.ldapfilter.LogicalOperator;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SimpleParserTest
{
    
    @Test
    public void test001Scheme()
    {
        // https://de.wikipedia.org/wiki/Uniform_Resource_Identifier#Beispiele
        
        URI uri = new URI("http://de.wikipedia.org/wiki/Uniform_Resource_Identifier");
        assertEquals("scheme should be correct", "http", uri.getScheme().getValue());
        
        uri = new URI("ftp://ftp.is.co.za/rfc/rfc1808.txt");
        assertEquals("scheme should correct", "ftp", uri.getScheme().getValue());
        
        uri = new URI("file:///C:/Users/Benutzer/Desktop/Uniform%20Resource%20Identifier.html");
        assertEquals("scheme should be correct", "file", uri.getScheme().getValue());
        
        uri = new URI("file:///etc/fstab");
        assertEquals("scheme should be correct", "file", uri.getScheme().getValue());
        
        uri = new URI("geo:48.33,14.122;u=22.5");
        assertEquals("scheme should be correct", "geo", uri.getScheme().getValue());
        
        uri = new URI("ldap://[2001:db8::7]/c=GB?objectClass?one");
        assertEquals("scheme should be correct", "ldap", uri.getScheme().getValue());
        
        uri = new URI("gopher://gopher.floodgap.com");
        assertEquals("scheme should be correct", "gopher", uri.getScheme().getValue());
        
        uri = new URI("mailto:John.Doe@example.com");
        assertEquals("scheme should be correct", "mailto", uri.getScheme().getValue());
        
        uri = new URI("sip:912@pbx.myhome.net");
        assertEquals("scheme should be correct", "sip", uri.getScheme().getValue());
        
        uri = new URI("news:comp.infosystems.www.servers.unix");
        assertEquals("scheme should be correct", "news", uri.getScheme().getValue());
        
        uri = new URI("data:text/plain;charset=iso-8859-7,%be%fa%be");
        assertEquals("scheme should be correct", "data", uri.getScheme().getValue());
        
        uri = new URI("tel:+1-816-555-1212");
        assertEquals("scheme should be correct", "tel", uri.getScheme().getValue());
        
        uri = new URI("telnet://192.0.2.16:80/");
        assertEquals("scheme should be correct", "telnet", uri.getScheme().getValue());
        
        uri = new URI("urn:oasis:names:specification:docbook:dtd:xml:4.1.2");
        assertEquals("scheme should be correct", "urn", uri.getScheme().getValue());
        
        uri = new URI("git://github.com/rails/rails.git");
        assertEquals("scheme should be correct", "git", uri.getScheme().getValue());
        
        uri = new URI("crid://broadcaster.com/movies/BestActionMovieEver");
        assertEquals("scheme should be correct", "crid", uri.getScheme().getValue());
        
        uri = new URI("http://nobody:password@example.org:8080/cgi-bin/script.php?action=submit&pageid=86392001#section_2");
        assertEquals("scheme should be correct", "http", uri.getScheme().getValue());
    }
    
    @Test
    public void test002Authority()
    {
        // https://de.wikipedia.org/wiki/Uniform_Resource_Identifier#Beispiele
        
        URI uri = new URI("http://de.wikipedia.org/wiki/Uniform_Resource_Identifier");
        assertEquals("authority should contains correct expression", "de.wikipedia.org", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 1, uri.getAuthority().getSubComponentList().size());
        assertEquals("subauthority[x] should contains correct expression", "de.wikipedia.org", uri.getAuthority().getSubComponentList().get(0).getExpression());
        assertEquals("subauthority[x] should contains correct value", "de.wikipedia.org", uri.getAuthority().getSubComponentList().get(0).getValue());
        
        uri = new URI("ftp://ftp.is.co.za/rfc/rfc1808.txt");
        assertEquals("authority should contains correct expression", "ftp.is.co.za", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 1, uri.getAuthority().getSubComponentList().size());
        assertEquals("subauthority[x] should contains correct expression", "ftp.is.co.za", uri.getAuthority().getSubComponentList().get(0).getExpression());
        assertEquals("subauthority[x] should contains correct value", "ftp.is.co.za", uri.getAuthority().getSubComponentList().get(0).getValue());
        
        uri = new URI("file:///C:/Users/Benutzer/Desktop/Uniform%20Resource%20Identifier.html");
        assertEquals("authority should contains correct expression", "", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 1, uri.getAuthority().getSubComponentList().size());
        assertEquals("subauthority[x] should contains correct expression", "", uri.getAuthority().getSubComponentList().get(0).getExpression());
        assertEquals("subauthority[x] should contains correct value", "", uri.getAuthority().getSubComponentList().get(0).getValue());
        
        uri = new URI("file:///etc/fstab");
        assertEquals("authority should contains correct expression", "", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 1, uri.getAuthority().getSubComponentList().size());
        assertEquals("subauthority[x] should contains correct expression", "", uri.getAuthority().getSubComponentList().get(0).getExpression());
        assertEquals("subauthority[x] should contains correct value", "", uri.getAuthority().getSubComponentList().get(0).getValue());
        
        uri = new URI("geo:48.33,14.122;u=22.5");
        assertNull("authority should contains correct expression", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 0, uri.getAuthority().getSubComponentList().size());
        
        uri = new URI("ldap://[2001:db8::7]/c=GB?objectClass?one");
        assertEquals("authority should contains correct expression", "[2001:db8::7]", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 1, uri.getAuthority().getSubComponentList().size());
        assertEquals("subauthority[x] should contains correct expression", "[2001:db8::7]", uri.getAuthority().getSubComponentList().get(0).getExpression());
        assertEquals("subauthority[x] should contains correct value", "[2001:db8::7]", uri.getAuthority().getSubComponentList().get(0).getValue());
        
        uri = new URI("gopher://gopher.floodgap.com");
        assertEquals("authority should contains correct expression", "gopher.floodgap.com", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 1, uri.getAuthority().getSubComponentList().size());
        assertEquals("subauthority[x] should contains correct expression", "gopher.floodgap.com", uri.getAuthority().getSubComponentList().get(0).getExpression());
        
        uri = new URI("mailto:John.Doe@example.com");
        assertNull("authority should contains correct expression", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 0, uri.getAuthority().getSubComponentList().size());
        
        uri = new URI("sip:912@pbx.myhome.net");
        assertNull("authority should contains correct expression", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 0, uri.getAuthority().getSubComponentList().size());
        
        uri = new URI("news:comp.infosystems.www.servers.unix");
        assertNull("authority should contains correct expression", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 0, uri.getAuthority().getSubComponentList().size());
        
        uri = new URI("data:text/plain;charset=iso-8859-7,%be%fa%be");
        assertNull("authority should contains correct expression", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 0, uri.getAuthority().getSubComponentList().size());
        
        uri = new URI("tel:+1-816-555-1212");
        assertNull("authority should contains correct expression", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 0, uri.getAuthority().getSubComponentList().size());
        
        uri = new URI("telnet://192.0.2.16:80/");
        assertEquals("authority should contains correct expression", "192.0.2.16:80", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 2, uri.getAuthority().getSubComponentList().size());
        assertEquals("subauthority[x] should contains correct expression", "192.0.2.16", uri.getAuthority().getSubComponentList().get(0).getExpression());
        assertEquals("subauthority[x] should contains correct expression", "80", uri.getAuthority().getSubComponentList().get(1).getExpression());
        
        uri = new URI("urn:oasis:names:specification:docbook:dtd:xml:4.1.2");
        assertNull("authority should contains correct expression", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 0, uri.getAuthority().getSubComponentList().size());
        
        uri = new URI("git://github.com/rails/rails.git");
        assertEquals("authority should contains correct expression", "github.com", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 1, uri.getAuthority().getSubComponentList().size());
        assertEquals("subauthority[x] should contains correct expression", "github.com", uri.getAuthority().getSubComponentList().get(0).getExpression());
        
        uri = new URI("crid://broadcaster.com/movies/BestActionMovieEver");
        assertEquals("authority should contains correct expression", "broadcaster.com", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 1, uri.getAuthority().getSubComponentList().size());
        assertEquals("subauthority[x] should contains correct expression", "broadcaster.com", uri.getAuthority().getSubComponentList().get(0).getExpression());
        
        uri = new URI("http://nobody:password@example.org:8080/cgi-bin/script.php?action=submit&pageid=86392001#section_2");
        assertEquals("authority should contains correct expression", "nobody:password@example.org:8080", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 4, uri.getAuthority().getSubComponentList().size());
        
        assertEquals("subauthority[x] should contains correct prefixdelimiter", '/', uri.getAuthority().getSubComponentList().get(0).getPrefixDelimiter());
        assertEquals("subauthority[x] should contains correct expression", "nobody", uri.getAuthority().getSubComponentList().get(0).getExpression());
        assertEquals("subauthority[x] should contains correct postfixdelimiter", ':', uri.getAuthority().getSubComponentList().get(0).getPostfixDelimiter());
        
        assertEquals("subauthority[x] should contains correct prefixdelimiter", ':', uri.getAuthority().getSubComponentList().get(1).getPrefixDelimiter());
        assertEquals("subauthority[x] should contains correct expression", "password", uri.getAuthority().getSubComponentList().get(1).getExpression());
        assertEquals("subauthority[x] should contains correct postfixdelimiter", '@', uri.getAuthority().getSubComponentList().get(1).getPostfixDelimiter());
        
        assertEquals("subauthority[x] should contains correct prefixdelimiter", '@', uri.getAuthority().getSubComponentList().get(2).getPrefixDelimiter());
        assertEquals("subauthority[x] should contains correct expression", "example.org", uri.getAuthority().getSubComponentList().get(2).getExpression());
        assertEquals("subauthority[x] should contains correct postfixdelimiter", ':', uri.getAuthority().getSubComponentList().get(2).getPostfixDelimiter());
        
        assertEquals("subauthority[x] should contains correct prefixdelimiter", ':', uri.getAuthority().getSubComponentList().get(3).getPrefixDelimiter());
        assertEquals("subauthority[x] should contains correct expression", "8080", uri.getAuthority().getSubComponentList().get(3).getExpression());
        assertEquals("subauthority[x] should contains correct postfixdelimiter", '/', uri.getAuthority().getSubComponentList().get(3).getPostfixDelimiter());
    }
    
    @Test
    public void test003AuthorityAndFilter()
    {
        
        URI uri = new URI("sdc://eventdispatcher(id=default)");
        assertEquals("authority should contains correct expression", "eventdispatcher(id=default)", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 1, uri.getAuthority().getSubComponentList().size());
        assertEquals("subauthority[x] should contains correct expression", "eventdispatcher(id=default)", uri.getAuthority().getSubComponentList().get(0).getExpression());
        assertEquals("subauthority[x] should contains correct value", "eventdispatcher", uri.getAuthority().getSubComponentList().get(0).getValue());
        assertEquals("subauthority[x] should contains correct extension.size", 1, uri.getAuthority().getSubComponentList().get(0).getExtensionList().size());
        
        uri = new URI("sdc://eventdispatcher(id=default)/");
        assertEquals("authority should contains correct expression", "eventdispatcher(id=default)", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 1, uri.getAuthority().getSubComponentList().size());
        assertEquals("subauthority[x] should contains correct expression", "eventdispatcher(id=default)", uri.getAuthority().getSubComponentList().get(0).getExpression());
        assertEquals("subauthority[x] should contains correct value", "eventdispatcher", uri.getAuthority().getSubComponentList().get(0).getValue());
        assertEquals("subauthority[x] should contains correct extension.size", 1, uri.getAuthority().getSubComponentList().get(0).getExtensionList().size());
        
        uri = new URI("sdc://aaa:eventdispatcher(id=default):bbb/");
        assertEquals("authority should contains correct expression", "aaa:eventdispatcher(id=default):bbb", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 3, uri.getAuthority().getSubComponentList().size());
        assertEquals("subauthority[x] should contains correct expression", "aaa", uri.getAuthority().getSubComponentList().get(0).getExpression());
        assertEquals("subauthority[x] should contains correct value", "aaa", uri.getAuthority().getSubComponentList().get(0).getValue());
        assertEquals("subauthority[x] should contains correct extension.size", 0, uri.getAuthority().getSubComponentList().get(0).getExtensionList().size());
        assertEquals("subauthority[x] should contains correct expression", "eventdispatcher(id=default)", uri.getAuthority().getSubComponentList().get(1).getExpression());
        assertEquals("subauthority[x] should contains correct value", "eventdispatcher", uri.getAuthority().getSubComponentList().get(1).getValue());
        assertEquals("subauthority[x] should contains correct extension.size", 1, uri.getAuthority().getSubComponentList().get(1).getExtensionList().size());
        assertEquals("subauthority[x] should contains correct expression", "bbb", uri.getAuthority().getSubComponentList().get(2).getExpression());
        assertEquals("subauthority[x] should contains correct value", "bbb", uri.getAuthority().getSubComponentList().get(2).getValue());
        assertEquals("subauthority[x] should contains correct extension.size", 0, uri.getAuthority().getSubComponentList().get(2).getExtensionList().size());
        
        uri = new URI("sdc://aaa:bbb:eventdispatcher(id=default)(&(timeout=1000)(mode=soft))/");
        assertEquals("authority should contains correct expression", "aaa:bbb:eventdispatcher(id=default)(&(timeout=1000)(mode=soft))", uri.getAuthority().getExpression());
        assertEquals("subauthority.size should be correct", 3, uri.getAuthority().getSubComponentList().size());
        assertEquals("subauthority[x] should contains correct expression", "aaa", uri.getAuthority().getSubComponentList().get(0).getExpression());
        assertEquals("subauthority[x] should contains correct value", "aaa", uri.getAuthority().getSubComponentList().get(0).getValue());
        assertEquals("subauthority[x] should contains correct extension.size", 0, uri.getAuthority().getSubComponentList().get(0).getExtensionList().size());
        assertEquals("subauthority[x] should contains correct expression", "bbb", uri.getAuthority().getSubComponentList().get(1).getExpression());
        assertEquals("subauthority[x] should contains correct value", "bbb", uri.getAuthority().getSubComponentList().get(1).getValue());
        assertEquals("subauthority[x] should contains correct extension.size", 0, uri.getAuthority().getSubComponentList().get(1).getExtensionList().size());
        assertEquals("subauthority[x] should contains correct expression", "eventdispatcher(id=default)(&(timeout=1000)(mode=soft))", uri.getAuthority().getSubComponentList().get(2).getExpression());
        assertEquals("subauthority[x] should contains correct value", "eventdispatcher", uri.getAuthority().getSubComponentList().get(2).getValue());
        assertEquals("subauthority[x] should contains correct extension.size", 2, uri.getAuthority().getSubComponentList().get(2).getExtensionList().size());
        IExtension<IFilterItem> filter1Extension = (IExtension<IFilterItem>) uri.getAuthority().getSubComponentList().get(2).getExtensionList().get(0);
        IExtension<IFilterItem> filter2Extension = (IExtension<IFilterItem>) uri.getAuthority().getSubComponentList().get(2).getExtensionList().get(1);
        
        Criteria filter1 = (Criteria) filter1Extension.getDecoder().decodeFromString(filter1Extension.getExpression());
        CriteriaLinker filter2 = (CriteriaLinker) filter2Extension.getDecoder().decodeFromString(filter2Extension.getExpression());
        assertEquals("filter1 name should be correct", "id", filter1.getName());
        assertEquals("filter1 operator should be correct", ComparativeOperator.EQUAL.name(), filter1.getOperator().name());
        assertEquals("filter1 value should be correct", "default", filter1.getValue());
        
        Criteria filter2a = (Criteria) filter2.getLinkedItemList().get(0);
        Criteria filter2b = (Criteria) filter2.getLinkedItemList().get(1);
        
        assertEquals("filter2 logical operator should be correct", LogicalOperator.AND.name(), filter2.getOperator().name());
        
        assertEquals("filter2a name should be correct", "timeout", filter2a.getName());
        assertEquals("filter2a operator should be correct", ComparativeOperator.EQUAL.name(), filter2a.getOperator().name());
        assertEquals("filter2a value should be correct", "1000", filter2a.getValue());
        
        assertEquals("filter2b name should be correct", "mode", filter2b.getName());
        assertEquals("filter2b operator should be correct", ComparativeOperator.EQUAL.name(), filter2b.getOperator().name());
        assertEquals("filter2b value should be correct", "soft", filter2b.getValue());
        
    }
    
    @Test
    public void test004Path()
    {
        // https://de.wikipedia.org/wiki/Uniform_Resource_Identifier#Beispiele
        
        URI uri = new URI("http://de.wikipedia.org/wiki/Uniform_Resource_Identifier");
        assertEquals("path should contains correct expression", "/wiki/Uniform_Resource_Identifier", uri.getPath().getExpression());
        assertEquals("pathsegment.size should be correct", 2, uri.getPath().getSubComponentList().size());
        assertEquals("pathsegment[x] should contains correct expression", "wiki", uri.getPath().getSubComponentList().get(0).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "wiki", uri.getPath().getSubComponentList().get(0).getValue());
        assertEquals("pathsegment[x] should contains correct expression", "Uniform_Resource_Identifier", uri.getPath().getSubComponentList().get(1).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "Uniform_Resource_Identifier", uri.getPath().getSubComponentList().get(1).getValue());
        
        uri = new URI("ftp://ftp.is.co.za/rfc/rfc1808.txt");
        assertEquals("path should contains correct expression", "/rfc/rfc1808.txt", uri.getPath().getExpression());
        assertEquals("pathsegment.size should be correct", 2, uri.getPath().getSubComponentList().size());
        assertEquals("pathsegment[x] should contains correct expression", "rfc", uri.getPath().getSubComponentList().get(0).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "rfc", uri.getPath().getSubComponentList().get(0).getValue());
        assertEquals("pathsegment[x] should contains correct expression", "rfc1808.txt", uri.getPath().getSubComponentList().get(1).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "rfc1808.txt", uri.getPath().getSubComponentList().get(1).getValue());
        
        uri = new URI("file:///C:/Users/Benutzer/Desktop/Uniform%20Resource%20Identifier.html");
        assertEquals("path should contains correct expression", "/C:/Users/Benutzer/Desktop/Uniform%20Resource%20Identifier.html", uri.getPath().getExpression());
        assertEquals("pathsegment.size should be correct", 5, uri.getPath().getSubComponentList().size());
        assertEquals("pathsegment[x] should contains correct expression", "C:", uri.getPath().getSubComponentList().get(0).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "C:", uri.getPath().getSubComponentList().get(0).getValue());
        assertEquals("pathsegment[x] should contains correct expression", "Users", uri.getPath().getSubComponentList().get(1).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "Users", uri.getPath().getSubComponentList().get(1).getValue());
        assertEquals("pathsegment[x] should contains correct expression", "Benutzer", uri.getPath().getSubComponentList().get(2).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "Benutzer", uri.getPath().getSubComponentList().get(2).getValue());
        assertEquals("pathsegment[x] should contains correct expression", "Desktop", uri.getPath().getSubComponentList().get(3).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "Desktop", uri.getPath().getSubComponentList().get(3).getValue());
        assertEquals("pathsegment[x] should contains correct expression", "Uniform%20Resource%20Identifier.html", uri.getPath().getSubComponentList().get(4).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "Uniform Resource Identifier.html", uri.getPath().getSubComponentList().get(4).getValue());
        
        uri = new URI("file:///etc/fstab");
        assertEquals("path should contains correct expression", "/etc/fstab", uri.getPath().getExpression());
        assertEquals("pathsegment.size should be correct", 2, uri.getPath().getSubComponentList().size());
        assertEquals("pathsegment[x] should contains correct expression", "etc", uri.getPath().getSubComponentList().get(0).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "etc", uri.getPath().getSubComponentList().get(0).getValue());
        assertEquals("pathsegment[x] should contains correct expression", "fstab", uri.getPath().getSubComponentList().get(1).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "fstab", uri.getPath().getSubComponentList().get(1).getValue());
        
        uri = new URI("geo:48.33,14.122;u=22.5");
        assertEquals("path should contains correct expression", "48.33,14.122;u=22.5", uri.getPath().getExpression());
        assertEquals("pathsegment.size should be correct", 1, uri.getPath().getSubComponentList().size());
        assertEquals("pathsegment[x] should contains correct expression", "48.33,14.122;u=22.5", uri.getPath().getSubComponentList().get(0).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "48.33,14.122;u=22.5", uri.getPath().getSubComponentList().get(0).getValue());
        
        uri = new URI("ldap://[2001:db8::7]/c=GB?objectClass?one");
        assertEquals("path should contains correct expression", "/c=GB", uri.getPath().getExpression());
        assertEquals("pathsegment.size should be correct", 1, uri.getPath().getSubComponentList().size());
        assertEquals("pathsegment[x] should contains correct expression", "c=GB", uri.getPath().getSubComponentList().get(0).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "c=GB", uri.getPath().getSubComponentList().get(0).getValue());
        
        uri = new URI("gopher://gopher.floodgap.com");
        assertEquals("path should contains correct expression", "", uri.getPath().getExpression());
        assertEquals("pathsegment.size should be correct", 0, uri.getPath().getSubComponentList().size());
        
        uri = new URI("mailto:John.Doe@example.com");
        assertEquals("path should contains correct expression", "John.Doe@example.com", uri.getPath().getExpression());
        assertEquals("pathsegment.size should be correct", 1, uri.getPath().getSubComponentList().size());
        assertEquals("pathsegment[x] should contains correct expression", "John.Doe@example.com", uri.getPath().getSubComponentList().get(0).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "John.Doe@example.com", uri.getPath().getSubComponentList().get(0).getValue());
        
        uri = new URI("sip:912@pbx.myhome.net");
        assertEquals("path should contains correct expression", "912@pbx.myhome.net", uri.getPath().getExpression());
        assertEquals("pathsegment.size should be correct", 1, uri.getPath().getSubComponentList().size());
        assertEquals("pathsegment[x] should contains correct expression", "912@pbx.myhome.net", uri.getPath().getSubComponentList().get(0).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "912@pbx.myhome.net", uri.getPath().getSubComponentList().get(0).getValue());
        
        uri = new URI("news:comp.infosystems.www.servers.unix");
        assertEquals("path should contains correct expression", "comp.infosystems.www.servers.unix", uri.getPath().getExpression());
        assertEquals("pathsegment.size should be correct", 1, uri.getPath().getSubComponentList().size());
        assertEquals("pathsegment[x] should contains correct expression", "comp.infosystems.www.servers.unix", uri.getPath().getSubComponentList().get(0).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "comp.infosystems.www.servers.unix", uri.getPath().getSubComponentList().get(0).getValue());
        
        uri = new URI("data:text/plain;charset=iso-8859-7,%5b%2f%5d");
        assertEquals("path should contains correct expression", "text/plain;charset=iso-8859-7,%5b%2f%5d", uri.getPath().getExpression());
        assertEquals("pathsegment.size should be correct", 2, uri.getPath().getSubComponentList().size());
        assertEquals("pathsegment[x] should contains correct expression", "text", uri.getPath().getSubComponentList().get(0).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "text", uri.getPath().getSubComponentList().get(0).getValue());
        assertEquals("pathsegment[x] should contains correct expression", "plain;charset=iso-8859-7,%5b%2f%5d", uri.getPath().getSubComponentList().get(1).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "plain;charset=iso-8859-7,[/]", uri.getPath().getSubComponentList().get(1).getValue());
        
        uri = new URI("tel:+1-816-555-1212");
        assertEquals("path should contains correct expression", "+1-816-555-1212", uri.getPath().getExpression());
        assertEquals("pathsegment.size should be correct", 1, uri.getPath().getSubComponentList().size());
        assertEquals("pathsegment[x] should contains correct expression", "+1-816-555-1212", uri.getPath().getSubComponentList().get(0).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "+1-816-555-1212", uri.getPath().getSubComponentList().get(0).getValue());
        
        uri = new URI("telnet://192.0.2.16:80/");
        assertEquals("path should contains correct expression", "/", uri.getPath().getExpression());
        assertEquals("pathsegment.size should be correct", 0, uri.getPath().getSubComponentList().size());
        
        uri = new URI("urn:oasis:names:specification:docbook:dtd:xml:4.1.2");
        assertEquals("path should contains correct expression", "oasis:names:specification:docbook:dtd:xml:4.1.2", uri.getPath().getExpression());
        assertEquals("pathsegment.size should be correct", 1, uri.getPath().getSubComponentList().size());
        assertEquals("pathsegment[x] should contains correct expression", "oasis:names:specification:docbook:dtd:xml:4.1.2", uri.getPath().getSubComponentList().get(0).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "oasis:names:specification:docbook:dtd:xml:4.1.2", uri.getPath().getSubComponentList().get(0).getValue());
        
        uri = new URI("git://github.com/rails/rails.git");
        assertEquals("path should contains correct expression", "/rails/rails.git", uri.getPath().getExpression());
        assertEquals("pathsegment.size should be correct", 2, uri.getPath().getSubComponentList().size());
        assertEquals("pathsegment[x] should contains correct expression", "rails", uri.getPath().getSubComponentList().get(0).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "rails", uri.getPath().getSubComponentList().get(0).getValue());
        assertEquals("pathsegment[x] should contains correct expression", "rails.git", uri.getPath().getSubComponentList().get(1).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "rails.git", uri.getPath().getSubComponentList().get(1).getValue());
        
        uri = new URI("crid://broadcaster.com/movies/BestActionMovieEver");
        assertEquals("path should contains correct expression", "/movies/BestActionMovieEver", uri.getPath().getExpression());
        assertEquals("pathsegment.size should be correct", 2, uri.getPath().getSubComponentList().size());
        assertEquals("pathsegment[x] should contains correct expression", "movies", uri.getPath().getSubComponentList().get(0).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "movies", uri.getPath().getSubComponentList().get(0).getValue());
        assertEquals("pathsegment[x] should contains correct expression", "BestActionMovieEver", uri.getPath().getSubComponentList().get(1).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "BestActionMovieEver", uri.getPath().getSubComponentList().get(1).getValue());
        
        uri = new URI("http://nobody:password@example.org:8080/cgi-bin/script.php?action=submit&pageid=86392001#section_2");
        assertEquals("path should contains correct expression", "/cgi-bin/script.php", uri.getPath().getExpression());
        assertEquals("pathsegment.size should be correct", 2, uri.getPath().getSubComponentList().size());
        assertEquals("pathsegment[x] should contains correct expression", "cgi-bin", uri.getPath().getSubComponentList().get(0).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "cgi-bin", uri.getPath().getSubComponentList().get(0).getValue());
        assertEquals("pathsegment[x] should contains correct expression", "script.php", uri.getPath().getSubComponentList().get(1).getExpression());
        assertEquals("pathsegment[x] should contains correct value", "script.php", uri.getPath().getSubComponentList().get(1).getValue());
        
    }
    
    @Test
    public void test006Query()
    {
        // https://de.wikipedia.org/wiki/Uniform_Resource_Identifier#Beispiele
        
        URI uri = new URI("http://de.wikipedia.org/wiki/Uniform_Resource_Identifier");
        assertEquals("query should contains correct expression", "", uri.getQuery().getExpression());
        assertEquals("query.size should be correct", 0, uri.getQuery().getSubComponentList().size());
        
        uri = new URI("ftp://ftp.is.co.za/rfc/rfc1808.txt");
        assertEquals("query should contains correct expression", "", uri.getQuery().getExpression());
        assertEquals("query.size should be correct", 0, uri.getQuery().getSubComponentList().size());
        
        uri = new URI("file:///C:/Users/Benutzer/Desktop/Uniform%20Resource%20Identifier.html");
        assertEquals("query should contains correct expression", "", uri.getQuery().getExpression());
        assertEquals("query.size should be correct", 0, uri.getQuery().getSubComponentList().size());
        
        uri = new URI("file:///etc/fstab");
        assertEquals("query should contains correct expression", "", uri.getQuery().getExpression());
        assertEquals("query.size should be correct", 0, uri.getQuery().getSubComponentList().size());
        
        uri = new URI("geo:48.33,14.122;u=22.5");
        assertEquals("query should contains correct expression", "", uri.getQuery().getExpression());
        assertEquals("query.size should be correct", 0, uri.getQuery().getSubComponentList().size());
        
        uri = new URI("ldap://[2001:db8::7]/c=GB?objectClass?one");
        assertEquals("query should contains correct expression", "objectClass?one", uri.getQuery().getExpression());
        assertEquals("query.size should be correct", 1, uri.getQuery().getSubComponentList().size()); // TODO
        assertEquals("querysegment[x] should contains correct expression", "objectClass?one", uri.getQuery().getSubComponentList().get(0).getExpression());
        assertNull("querysegment[x] should contains correct type", uri.getQuery().getSubComponentList().get(0).getType());
        assertNull("querysegment[x] should contains correct name", uri.getQuery().getSubComponentList().get(0).getName());
        assertNull("querysegment[x] should contains correct format", uri.getQuery().getSubComponentList().get(0).getCoding());
        assertEquals("querysegment[x] should contains correct value", "objectClass?one", uri.getQuery().getSubComponentList().get(0).getValue());
        
        uri = new URI("gopher://gopher.floodgap.com");
        assertEquals("query should contains correct expression", "", uri.getQuery().getExpression());
        assertEquals("query.size should be correct", 0, uri.getQuery().getSubComponentList().size());
        
        uri = new URI("mailto:John.Doe@example.com");
        assertEquals("query should contains correct expression", "", uri.getQuery().getExpression());
        assertEquals("query.size should be correct", 0, uri.getQuery().getSubComponentList().size());
        
        uri = new URI("sip:912@pbx.myhome.net");
        assertEquals("query should contains correct expression", "", uri.getQuery().getExpression());
        assertEquals("query.size should be correct", 0, uri.getQuery().getSubComponentList().size());
        
        uri = new URI("news:comp.infosystems.www.servers.unix");
        assertEquals("query should contains correct expression", "", uri.getQuery().getExpression());
        assertEquals("query.size should be correct", 0, uri.getQuery().getSubComponentList().size());
        
        uri = new URI("data:text/plain;charset=iso-8859-7,%5b%2f%5d");
        assertEquals("query should contains correct expression", "", uri.getQuery().getExpression());
        assertEquals("query.size should be correct", 0, uri.getQuery().getSubComponentList().size());
        
        uri = new URI("tel:+1-816-555-1212");
        assertEquals("query should contains correct expression", "", uri.getQuery().getExpression());
        assertEquals("query.size should be correct", 0, uri.getQuery().getSubComponentList().size());
        
        uri = new URI("telnet://192.0.2.16:80/");
        assertEquals("query should contains correct expression", "", uri.getQuery().getExpression());
        assertEquals("query.size should be correct", 0, uri.getQuery().getSubComponentList().size());
        
        uri = new URI("urn:oasis:names:specification:docbook:dtd:xml:4.1.2");
        assertEquals("query should contains correct expression", "", uri.getQuery().getExpression());
        assertEquals("query.size should be correct", 0, uri.getQuery().getSubComponentList().size());
        
        uri = new URI("git://github.com/rails/rails.git");
        assertEquals("query should contains correct expression", "", uri.getQuery().getExpression());
        assertEquals("query.size should be correct", 0, uri.getQuery().getSubComponentList().size());
        
        uri = new URI("crid://broadcaster.com/movies/BestActionMovieEver");
        assertEquals("query should contains correct expression", "", uri.getQuery().getExpression());
        assertEquals("query.size should be correct", 0, uri.getQuery().getSubComponentList().size());
        
        uri = new URI("http://nobody:password@example.org:8080/cgi-bin/script.php?action=submit&pageid=86392001#section_2");
        assertEquals("query should contains correct expression", "action=submit&pageid=86392001", uri.getQuery().getExpression());
        assertEquals("query.size should be correct", 2, uri.getQuery().getSubComponentList().size());
        assertEquals("querysegment[x] should contains correct expression", "action=submit", uri.getQuery().getSubComponentList().get(0).getExpression());
        assertEquals("querysegment[x] should contains correct expression", "pageid=86392001", uri.getQuery().getSubComponentList().get(1).getExpression());
        
        assertNull("querysegment[x] should contains correct type", uri.getQuery().getSubComponentList().get(0).getType());
        assertEquals("querysegment[x] should contains correct name", "action", uri.getQuery().getSubComponentList().get(0).getName());
        assertNull("querysegment[x] should contains correct format", uri.getQuery().getSubComponentList().get(0).getCoding());
        assertEquals("querysegment[x] should contains correct value", "submit", uri.getQuery().getSubComponentList().get(0).getValue());
        
        assertNull("querysegment[x] should contains correct type", uri.getQuery().getSubComponentList().get(1).getType());
        assertEquals("querysegment[x] should contains correct name", "pageid", uri.getQuery().getSubComponentList().get(1).getName());
        assertNull("querysegment[x] should contains correct format", uri.getQuery().getSubComponentList().get(1).getCoding());
        assertEquals("querysegment[x] should contains correct value", "86392001", uri.getQuery().getSubComponentList().get(1).getValue());
        
        // parse String / JSON
        
        String str = "utf8 = &{( formated \\'text\\' \\t # ";
        // {"id":13, "name":"Max\tMuster-\nmann", mother:{"id":1, "name":"Diana }" }}
        String json = "{\"id\":13, \"name\":\"Max\\tMuster-\\nmann\", mother:{\"id\":1, \"name\":\"Diana }\" }}";
        String queryString =
            "action=check" + "&"
            + "test=string:'" + str + "'" + "&"
            + "org.sodeac.user.User:user=json:" + json + "";
        
        uri = new URI("sdc://eventdispatcher(|(id=default)(id=userservice))/org.sodeac.user.service?" + queryString);
        assertEquals("query should contains correct expression", queryString, uri.getQuery().getExpression());
        
        assertNull("querysegment[x] should contains correct type", uri.getQuery().getSubComponentList().get(0).getType());
        assertEquals("querysegment[x] should contains correct name", "action", uri.getQuery().getSubComponentList().get(0).getName());
        assertNull("querysegment[x] should contains correct format", uri.getQuery().getSubComponentList().get(0).getCoding());
        assertEquals("querysegment[x] should contains correct value", "check", uri.getQuery().getSubComponentList().get(0).getValue());
        
        assertNull("querysegment[x] should contains correct type", uri.getQuery().getSubComponentList().get(1).getType());
        assertEquals("querysegment[x] should contains correct name", "test", uri.getQuery().getSubComponentList().get(1).getName());
        assertEquals("querysegment[x] should contains correct format", "string", uri.getQuery().getSubComponentList().get(1).getCoding());
        assertEquals("querysegment[x] should contains correct value", "utf8 = &{( formated 'text' \\t # ", uri.getQuery().getSubComponentList().get(1).getValue());
        
        assertEquals("querysegment[x] should contains correct type", "org.sodeac.user.User", uri.getQuery().getSubComponentList().get(2).getType());
        assertEquals("querysegment[x] should contains correct name", "user", uri.getQuery().getSubComponentList().get(2).getName());
        assertEquals("querysegment[x] should contains correct format", "json", uri.getQuery().getSubComponentList().get(2).getCoding());
        assertEquals("querysegment[x] should contains correct value", json, uri.getQuery().getSubComponentList().get(2).getValue());
        
    }
    
    @Test
    public void test008Fragment()
    {
        // https://de.wikipedia.org/wiki/Uniform_Resource_Identifier#Beispiele
        
        URI uri = new URI("http://de.wikipedia.org/wiki/Uniform_Resource_Identifier");
        assertNull("fragment should be null", uri.getFragment());
        
        uri = new URI("ftp://ftp.is.co.za/rfc/rfc1808.txt");
        assertNull("fragment should be null", uri.getFragment());
        
        uri = new URI("file:///C:/Users/Benutzer/Desktop/Uniform%20Resource%20Identifier.html");
        assertNull("fragment should be null", uri.getFragment());
        
        uri = new URI("file:///etc/fstab");
        assertNull("fragment should be null", uri.getFragment());
        
        uri = new URI("geo:48.33,14.122;u=22.5");
        assertNull("fragment should be null", uri.getFragment());
        
        uri = new URI("ldap://[2001:db8::7]/c=GB?objectClass?one");
        assertNull("fragment should be null", uri.getFragment());
        
        uri = new URI("gopher://gopher.floodgap.com");
        assertNull("fragment should be null", uri.getFragment());
        
        uri = new URI("mailto:John.Doe@example.com");
        assertNull("fragment should be null", uri.getFragment());
        
        uri = new URI("sip:912@pbx.myhome.net");
        assertNull("fragment should be null", uri.getFragment());
        
        uri = new URI("news:comp.infosystems.www.servers.unix");
        assertNull("fragment should be null", uri.getFragment());
        
        uri = new URI("data:text/plain;charset=iso-8859-7,%5b%2f%5d");
        assertNull("fragment should be null", uri.getFragment());
        
        uri = new URI("tel:+1-816-555-1212");
        assertNull("fragment should be null", uri.getFragment());
        
        uri = new URI("telnet://192.0.2.16:80/");
        assertNull("fragment should be null", uri.getFragment());
        
        uri = new URI("urn:oasis:names:specification:docbook:dtd:xml:4.1.2");
        assertNull("fragment should be null", uri.getFragment());
        
        uri = new URI("git://github.com/rails/rails.git");
        assertNull("fragment should be null", uri.getFragment());
        
        uri = new URI("crid://broadcaster.com/movies/BestActionMovieEver");
        assertNull("fragment should be null", uri.getFragment());
        
        uri = new URI("http://nobody:password@example.org:8080/cgi-bin/script.php?action=submit&pageid=86392001#section_2");
        assertNotNull("fragment should not be null", uri.getFragment());
        assertEquals("fragment should contains correct expression", "section_2", uri.getFragment().getExpression());
        assertEquals("fragment should contains correct expression", "section_2", uri.getFragment().getValue());
    }
    
}
