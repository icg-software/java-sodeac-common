package org.sodeac.common.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sodeac.common.jdbc.TypedTreeJDBCCruder.Session;
import org.sodeac.common.jdbc.cruder.ArticleExtension1NodeType;
import org.sodeac.common.jdbc.cruder.ArticleExtension2NodeType;
import org.sodeac.common.jdbc.cruder.ArticleGroupNodeType;
import org.sodeac.common.jdbc.cruder.ArticleNodeType;
import org.sodeac.common.jdbc.cruder.MiniMerchandiseManagementModel;
import org.sodeac.common.model.CommonGenericPropertyNodeType;
import org.sodeac.common.model.CoreTreeModel;
import org.sodeac.common.model.dbschema.DBSchemaNodeType;
import org.sodeac.common.typedtree.BranchNode;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.TypedTreeMetaModel.RootBranchNode;
import org.sodeac.common.typedtree.annotation.SQLTable;

@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CruderTest
{
    public static List<Object[]> connectionList = null;
    public static final Map<String, Boolean> createdSchema = new HashMap<String, Boolean>();
    
    @Parameters
    public static List<Object[]> connections()
    {
        if (connectionList != null)
        {
            return connectionList;
        }
        return connectionList = Statics.connections(createdSchema, "cruder");
    }
    
    public CruderTest(final Callable<TestConnection> connectionFactory)
    {
        this.testConnectionFactory = connectionFactory;
    }
    
    Callable<TestConnection> testConnectionFactory = null;
    TestConnection testConnection = null;
    
    @Before
    public void setUp() throws Exception
    {
        this.testConnection = this.testConnectionFactory.call();
    }
    
    @After
    public void tearDown()
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        if (this.testConnection.connection != null)
        {
            try
            {
                this.testConnection.connection.close();
            }
            catch (final Exception e) { }
        }
    }
    
    @Test
    public void t00001CreateSchema() throws Exception
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        
        final ParseDBSchemaHandler parseDBSchemaHandler = new ParseDBSchemaHandler("ArticleCruderTest");
        ModelRegistry.parse(MiniMerchandiseManagementModel.class, parseDBSchemaHandler); // TODO Model only
        final RootBranchNode<?, DBSchemaNodeType> schemaSpec = parseDBSchemaHandler.fillSchemaSpec(MiniMerchandiseManagementModel.class, CoreTreeModel.class);
        schemaSpec.setValue(DBSchemaNodeType.logUpdates, false);
        
        schemaSpec.setValue(DBSchemaNodeType.dbmsSchemaName, this.testConnection.connection.getSchema());
        
        this.testConnection.connection.setAutoCommit(false);
        final DBSchemaUtils schemaUtils = DBSchemaUtils.get(this.testConnection.connection);
        schemaUtils.adaptSchema(schemaSpec);
        this.testConnection.connection.commit();
        schemaSpec.dispose();
    }
    
    @Test
    public void t00010SimpleTests() throws Exception
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        
        final TypedTreeJDBCCruder cruder = TypedTreeJDBCCruder.get();
        final Session session = cruder.openSession(this.testConnection.getDataSource());
        
        final BranchNode<MiniMerchandiseManagementModel, ArticleGroupNodeType> hotDrink = session.persist(ArticleGroupNodeType.newNode()
                                                                                                                              .setValue(ArticleGroupNodeType.number, 1000L)
                                                                                                                              .setValue(ArticleGroupNodeType.name, "HotDrink")
                                                                                                                              .setValue(ArticleGroupNodeType.tax, 1.0));
        
        final BranchNode<MiniMerchandiseManagementModel, ArticleGroupNodeType> beverage = session.persist(ArticleGroupNodeType.newNode()
                                                                                                                              .setValue(ArticleGroupNodeType.number, 1001L)
                                                                                                                              .setValue(ArticleGroupNodeType.name, "Beverage")
                                                                                                                              .setValue(ArticleGroupNodeType.tax, 1.0));
        
        final BranchNode<MiniMerchandiseManagementModel, ArticleGroupNodeType> food = session.persist(ArticleGroupNodeType.newNode()
                                                                                                                          .setValue(ArticleGroupNodeType.number, 1002L)
                                                                                                                          .setValue(ArticleGroupNodeType.name, "Food")
                                                                                                                          .setValue(ArticleGroupNodeType.tax, 1.0));
        
        session.persist(beverage.create(ArticleGroupNodeType.propertyList)
                                .setValue(CommonGenericPropertyNodeType.type, "org.sodeac.property.generic")
                                .setValue(CommonGenericPropertyNodeType.key, "TESTKEY")
                                .setValue(CommonGenericPropertyNodeType.value, "TESTVALUE"));
        
        final BranchNode node = ArticleNodeType.newNode()
                                               .setValue(ArticleNodeType.number, 1000001L)
                                               .setValue(ArticleNodeType.name, "Grüner Tee")
                                               .setValue(ArticleNodeType.description, "Sencha")
                                               .create(ArticleNodeType.group).copyFrom(beverage).getParentNode();
        
        session.persist(node);
        
        session.flush();
        session.commit();
        
        RootBranchNode<MiniMerchandiseManagementModel, ArticleNodeType> article = session.loadRootNode(MiniMerchandiseManagementModel.article, node.getValue(ArticleNodeType.id));
        
        session.loadItem(article.get(ArticleNodeType.group));
        session.loadReferencedChildNodes(article.get(ArticleNodeType.group), ArticleGroupNodeType.propertyList);
        
        assertEquals("value should be correct", node.getValue(ArticleNodeType.id), article.getValue(ArticleNodeType.id));
        assertEquals("value should be correct", node.getValue(ArticleNodeType.name), article.getValue(ArticleNodeType.name));
        assertEquals("value should be correct", node.getValue(ArticleNodeType.number), article.getValue(ArticleNodeType.number));
        assertEquals("value should be correct", node.getValue(ArticleNodeType.description), article.getValue(ArticleNodeType.description));
        
        assertEquals("value should be correct", beverage.getValue(ArticleGroupNodeType.id), article.get(ArticleNodeType.group).getValue(ArticleGroupNodeType.id));
        assertEquals("value should be correct", beverage.getValue(ArticleGroupNodeType.name), article.get(ArticleNodeType.group).getValue(ArticleGroupNodeType.name));
        assertEquals("value should be correct", beverage.getValue(ArticleGroupNodeType.number), article.get(ArticleNodeType.group).getValue(ArticleGroupNodeType.number));
        
        assertEquals("list size should be correct", 1, article.get(ArticleNodeType.group).getUnmodifiableNodeList(ArticleGroupNodeType.propertyList).size());
        
        assertEquals("value should be correct", "org.sodeac.property.generic", article.get(ArticleNodeType.group).getUnmodifiableNodeList(ArticleGroupNodeType.propertyList).get(0).getValue(CommonGenericPropertyNodeType.type));
        assertEquals("value should be correct", "TESTKEY", article.get(ArticleNodeType.group).getUnmodifiableNodeList(ArticleGroupNodeType.propertyList).get(0).getValue(CommonGenericPropertyNodeType.key));
        assertEquals("value should be correct", "TESTVALUE", article.get(ArticleNodeType.group).getUnmodifiableNodeList(ArticleGroupNodeType.propertyList).get(0).getValue(CommonGenericPropertyNodeType.value));
        
        assertEquals("value should be correct", 1, article.getValue(ArticleNodeType.persistVersionNumber).longValue());
        final UUID uuid1 = article.getValue(ArticleNodeType.persistVersionId);
        
        Thread.sleep(3000);
        session.persist(article.setValue(ArticleNodeType.description, "Gunpowder").create(ArticleNodeType.group).copyFrom(hotDrink).getParentNode());
        session.flush();
        session.commit();
        
        assertEquals("value should be correct", "Gunpowder", article.getValue(ArticleNodeType.description));
        assertEquals("value should be correct", hotDrink.getValue(ArticleGroupNodeType.id), article.get(ArticleNodeType.group).getValue(ArticleGroupNodeType.id));
        assertEquals("value should be correct", hotDrink.getValue(ArticleGroupNodeType.name), article.get(ArticleNodeType.group).getValue(ArticleGroupNodeType.name));
        assertEquals("value should be correct", hotDrink.getValue(ArticleGroupNodeType.number), article.get(ArticleNodeType.group).getValue(ArticleGroupNodeType.number));
        
        article = session.loadRootNode(MiniMerchandiseManagementModel.article, node.getValue(ArticleNodeType.id));
        session.loadItem(article.get(ArticleNodeType.group));
        session.loadReferencedChildNodes(article.get(ArticleNodeType.group), ArticleGroupNodeType.propertyList);
        
        assertEquals("value should be correct", node.getValue(ArticleNodeType.id), article.getValue(ArticleNodeType.id));
        assertEquals("value should be correct", node.getValue(ArticleNodeType.name), article.getValue(ArticleNodeType.name));
        assertEquals("value should be correct", node.getValue(ArticleNodeType.number), article.getValue(ArticleNodeType.number));
        assertEquals("value should be correct", "Gunpowder", article.getValue(ArticleNodeType.description));
        
        assertEquals("value should be correct", hotDrink.getValue(ArticleGroupNodeType.id), article.get(ArticleNodeType.group).getValue(ArticleGroupNodeType.id));
        assertEquals("value should be correct", hotDrink.getValue(ArticleGroupNodeType.name), article.get(ArticleNodeType.group).getValue(ArticleGroupNodeType.name));
        assertEquals("value should be correct", hotDrink.getValue(ArticleGroupNodeType.number), article.get(ArticleNodeType.group).getValue(ArticleGroupNodeType.number));
        
        assertEquals("list size should be correct", 0, article.get(ArticleNodeType.group).getUnmodifiableNodeList(ArticleGroupNodeType.propertyList).size());
        
        assertEquals("value should be correct", 2, article.getValue(ArticleNodeType.persistVersionNumber).longValue());
        final UUID uuid2 = article.getValue(ArticleNodeType.persistVersionId);
        assertNotEquals("value should be different", uuid1, uuid2);
        
        session.delete(article);
        session.commit();
        
        final PreparedStatement preparedStatement = this.testConnection.connection.prepareStatement("select count(*) from " + ArticleNodeType.class.getAnnotation(SQLTable.class).name());
        try
        {
            final ResultSet resultSet = preparedStatement.executeQuery();
            try
            {
                resultSet.next();
                assertEquals("value should be correct", 0, resultSet.getInt(1));
            }
            finally
            {
                resultSet.close();
            }
        }
        finally
        {
            preparedStatement.close();
        }
        
        session.close();
        
        cruder.close();
    }
    
    @Test
    public void t00110TestExtension2() throws Exception
    {
        if (!this.testConnection.enabled)
        {
            return;
        }
        
        final TypedTreeJDBCCruder cruder = TypedTreeJDBCCruder.get();
        final Session session = cruder.openSession(this.testConnection.getDataSource());
        
        final BranchNode<MiniMerchandiseManagementModel, ArticleNodeType> article1 = ArticleNodeType.newNode()
                                                                                                    .setValue(ArticleNodeType.number, 1001001L)
                                                                                                    .setValue(ArticleNodeType.name, "BaseArticle1")
                                                                                                    .setValue(ArticleNodeType.description, "....");
        
        final BranchNode<MiniMerchandiseManagementModel, ArticleNodeType> article2 = ArticleNodeType.newNode()
                                                                                                    .setValue(ArticleNodeType.number, 1001002L)
                                                                                                    .setValue(ArticleNodeType.name, "BaseArticle2")
                                                                                                    .setValue(ArticleNodeType.description, "....");
        
        session.persist(article1);
        session.persist(article2);
        
        session.persist(article1.create(ArticleNodeType.extension2).setValue(ArticleExtension2NodeType.featureA, "Feature1"));
        
        session.commit();
        
        PreparedStatement preparedStatement = this.testConnection.connection.prepareStatement("select count(*) from article_extension_2 where article_id = ? and id = ?");
        try
        {
            preparedStatement.setObject(1, article1.getValue(ArticleNodeType.id));
            preparedStatement.setObject(2, article1.get(ArticleNodeType.extension2).getValue(ArticleExtension2NodeType.id));
            ResultSet resultSet = preparedStatement.executeQuery();
            try
            {
                resultSet.next();
                assertEquals("value should be correct", 1L, resultSet.getLong(1));
            }
            finally
            {
                resultSet.close();
                resultSet = null;
            }
            
            article2.create(ArticleNodeType.extension2).copyFrom(article1.get(ArticleNodeType.extension2)).setValue(ArticleExtension2NodeType.featureA, "Feature2");
            
            session.persist(article2.get(ArticleNodeType.extension2));
            session.commit();
            
            preparedStatement.setObject(1, article1.getValue(ArticleNodeType.id));
            preparedStatement.setObject(2, article1.get(ArticleNodeType.extension2).getValue(ArticleExtension2NodeType.id));
            resultSet = preparedStatement.executeQuery();
            try
            {
                resultSet.next();
                assertEquals("value should be correct", 0L, resultSet.getLong(1));
            }
            finally
            {
                resultSet.close();
                resultSet = null;
            }
            
            preparedStatement.setObject(1, article2.getValue(ArticleNodeType.id));
            preparedStatement.setObject(2, article1.get(ArticleNodeType.extension2).getValue(ArticleExtension2NodeType.id));
            resultSet = preparedStatement.executeQuery();
            try
            {
                resultSet.next();
                assertEquals("value should be correct", 1L, resultSet.getLong(1));
            }
            finally
            {
                resultSet.close();
                resultSet = null;
            }
            
            preparedStatement.setObject(1, article2.getValue(ArticleNodeType.id));
            preparedStatement.setObject(2, article2.get(ArticleNodeType.extension2).getValue(ArticleExtension2NodeType.id));
            resultSet = preparedStatement.executeQuery();
            try
            {
                resultSet.next();
                assertEquals("value should be correct", 1L, resultSet.getLong(1));
            }
            finally
            {
                resultSet.close();
                resultSet = null;
            }
        }
        finally
        {
            preparedStatement.close();
            preparedStatement = null;
        }
        
        session.loadItem(article2);
        article2.remove(ArticleNodeType.extension2);
        
        session.loadReferencedChildNode(article2, ArticleNodeType.extension2);
        
        assertEquals("value should be correct", article1.get(ArticleNodeType.extension2).getValue(ArticleExtension1NodeType.id), article2.get(ArticleNodeType.extension2).getValue(ArticleExtension1NodeType.id));
        
        article2.create(ArticleNodeType.extension2).setValue(ArticleExtension1NodeType.id, article1.get(ArticleNodeType.extension2).getValue(ArticleExtension1NodeType.id));
        
        session.loadItem(article2.get(ArticleNodeType.extension2));
        
        assertEquals("value should be correct", article1.get(ArticleNodeType.extension2).getValue(ArticleExtension1NodeType.id), article2.get(ArticleNodeType.extension2).getValue(ArticleExtension1NodeType.id));
        
        session.close();
        cruder.close();
    }
    
    //@Test
	/*public void t0000100InsertAutogenerated() throws Exception
	{
		try(CloseableCollector closeableCollector = CloseableCollector.newInstance())
		{
			DriverManager.registerDriver(org.h2.Driver.class.newInstance());
			
			String tempDir = System.getProperty("java.io.tmpdir");
			String database = "testautogenkey";
			
			// create database from dump
			
			if(tempDir.endsWith("\\")) {tempDir = tempDir.substring(0, tempDir.length() -1);}
			if(tempDir.endsWith("/")) {tempDir = tempDir.substring(0, tempDir.length() -1);}
			
			new File("./target/" + database + ".mv.db").delete();
			
			int len;
			byte[] buffer = new byte[1080];
			ZipInputStream zis = closeableCollector.register(new ZipInputStream(new FileInputStream("./src/test/resources/classicmodelcars.zip")));
			ZipEntry zipEntry = zis.getNextEntry();
			while (zipEntry != null) 
			{
				if(! "script.sql".equals(zipEntry.getName()))
				{
					continue;
				}
				FileOutputStream fos = closeableCollector.register(new FileOutputStream(new File(tempDir,zipEntry.getName())));
				while ((len = zis.read(buffer)) > 0)
				{
					fos.write(buffer, 0, len);
				}
				closeableCollector.close(fos);
				zipEntry = zis.getNextEntry();
			}
	        zis.closeEntry();
			
			org.h2.tools.RunScript.execute("jdbc:h2:" + "./target/" + database, "", "",tempDir +"/script.sql", null,false);
			
			new File(tempDir,"script.sql").delete();
			
			JdbcDataSource ds = new JdbcDataSource();
			ds.setURL("jdbc:h2:" + tempDir + "/" + database);
			ds.setUser("");
			ds.setPassword("");
			
			
			TypedTreeJDBCCruder cruder = TypedTreeJDBCCruder.get();
			
			Session session = cruder.openSession(ds);
			
			RootBranchNode<OfficeTreeModel, CustomerNodeType> customer = session.loadRootNode(OfficeTreeModel.customer, 496);
			
			BranchNode<CustomerNodeType, PaymentNodeType> payment = customer.create(CustomerNodeType.PAYMENTS)
				.setValue(PaymentNodeType.PAYMENTAMOUNT, 1.0)
				.setValue(PaymentNodeType.PAYMENTCHECKNUMBER, "xxxxx")
				.setValue(PaymentNodeType.PAYMENTDATE, new Date());
			
			session.persist(payment);
			
			session.flush();
			session.commit();
			
			cruder.close();
			
			Integer retrievedId = payment.getValue(PaymentNodeType.PAYMENTID);
			Connection connection = closeableCollector.register(ds.getConnection());
			
			assertNotNull("value should be correct", retrievedId);
			assertTrue("value should gt 0", retrievedId.intValue() > 0);
			
			PreparedStatement preparedStatement = closeableCollector.register(connection.prepareStatement("select * from PAYMENTS where CHECKNUMBER = ?"));
			
			preparedStatement.setString(1, "xxxxx");
			
			Integer checkedId = null;
			ResultSet resultSet = closeableCollector.register(preparedStatement.executeQuery());
			while(resultSet.next())
			{
				checkedId = resultSet.getInt("ID");
			}	
			closeableCollector.close(resultSet);
			closeableCollector.close(preparedStatement);
			closeableCollector.close(connection);
			
			assertNotNull("value should be correct", checkedId);
			assertEquals("value should be correct", retrievedId, checkedId);
			
			new File(tempDir + "/" + database + ".mv.db").delete();
		}
	}*/
 
}
