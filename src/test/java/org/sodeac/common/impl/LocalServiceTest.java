package org.sodeac.common.impl;

import static org.junit.Assert.assertEquals;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.sodeac.common.IService.ServiceSelectorAddress;
import org.sodeac.common.misc.Version;
import org.sodeac.common.xuri.URI;
import org.sodeac.common.xuri.ldapfilter.FilterBuilder;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LocalServiceTest
{
	/*@Test
	public void test00001ServiceRegistrationURI()
	{
		URI uri = ServiceRegistrationAddressBuilder.newBuilder()
					.forDomain("sodeac.org")
					.withServiceName("TestService")
					.andVersion(1, 0, 0)
					.addOption("priority", 1000) // TODO classes 
				.build(); // TODO Returns RegistrationAddress  (@Potentiality fromURI toURI)
		
		assertEquals("value should be correct", "sdc://serviceaddress:sodeac.org/TestService/1.0.0/options{\"priority\":1000}", uri.toString());
	}*/
    
    @Test
    public void test00003ServiceSelectorURI()
    {
        URI uri = ServiceSelectorAddress.newBuilder()
                                        .forDomain("sodeac.org")
                                        .withServiceName("TestService")
                                        .setFilter
                                            (
                                                FilterBuilder.andLinker()
                                                             .criteriaWithName("version").gte(new Version(1).toString())
                                                             .criteriaWithName("version").notGte(new Version(2).toString())
                                                             .build()
                                            )
                                        .scoreThePreferenceFilter
                                            (
                                                FilterBuilder.andLinker().criteriaWithName("version").gte(new Version(1, 5).toString()).build()
                                            ).with(1000).points()
                                        .scoreThePreferenceFilter
                                            (
                                                FilterBuilder.andLinker().criteriaWithName("writable").eq(Boolean.TRUE.toString()).build()
                                            ).with(2000).points()
                                        .build();
        
        assertEquals("value should be correct",
                     "sdc://serviceselector:sodeac.org/TestService(&(version>=1.0.0)(!(version>=2.0.0)))/preferences{\"score\":1000,\"filter\":\"(version>=1.5.0)\"}{\"score\":2000,\"filter\":\"(writable=true)\"}",
                     uri.toString()
        );
    }
    
    @Test
    public void test00003XXX()
    {
		/*IServiceRegistry serviceRegistry1 = LocalService.getLocalServiceRegistryImpl();
		assertNotNull("value should no be null",serviceRegistry1);
		
		IServiceRegistry serviceRegistry2 = LocalService.getServiceProvider(IServiceRegistry.class, IService.URI_SERVICE_LOCATOR_SERVICE_REGISTRY).getService().get();
		assertNotNull("value should no be null",serviceRegistry2);
		
		assertSame("value shuld be correct",serviceRegistry1, serviceRegistry2);*/
    }
}
