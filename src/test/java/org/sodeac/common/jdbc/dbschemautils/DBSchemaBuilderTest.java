package org.sodeac.common.jdbc.dbschemautils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
//import org.sodeac.common.model.dbschema.DBSchemaBow;
//import org.sodeac.common.model.dbschema.DBSchemaBowFactory;
//import org.sodeac.common.model.dbschema.TableBow;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DBSchemaBuilderTest
{
//	@Test
//	public void test0001Schema()
//	{
//		DBSchemaBow<?> schema1 = DBSchemaBowFactory.createSchema();
//		assertNotNull("Object should not be null",schema1);
//		assertNull("value should be null",schema1.getName());
//		assertNull("value should be null",schema1.getDbmsSchemaName());
//		
//		DBSchemaBow<?> schema2 = DBSchemaBowFactory.createSchema("AAA");
//		assertNotNull("Object should not be null",schema2);
//		assertEquals("value should be correct","AAA",schema2.getName());
//		assertNull("value should be null",schema2.getDbmsSchemaName());
//		
//		DBSchemaBow<?> schema3 = DBSchemaBowFactory.createSchema("BBB","public");
//		assertNotNull("Object should not be null",schema3);
//		assertEquals("value should be correct","BBB",schema3.getName());
//		assertEquals("value should be correct","public",schema3.getDbmsSchemaName());
//		
//		assertNull("value should be null",schema3.getTableSpaceData());
//		assertNull("value should be null",schema3.getTableSpaceIndex());
//		assertNull("value should be null",schema3.isLogUpdates());
//		assertNull("value should be null",schema3.isSkipChecks());
//		
//		schema3.setTableSpaceData("ts_data");
//		schema3.setTableSpaceIndex("ts_index");
//		schema3.setSkipChecks(true);
//		schema3.setLogUpdates(false);
//		
//		assertEquals("value should be correct","ts_data",schema3.getTableSpaceData());
//		assertEquals("value should be correct","ts_index",schema3.getTableSpaceIndex());
//		assertTrue("value should be correct", schema3.isSkipChecks());
//		assertFalse("value should be correct", schema3.isLogUpdates());
//		
//		assertEquals("value should be correct",0,schema3.getUnmodifiableListOfTables().size());
//		assertEquals("value should be correct",0,schema3.getUnmodifiableListOfConsumers().size());
//		
//		schema3.createOneOfConsumers().setEventConsumer(e -> {});
//		
//		assertEquals("value should be correct",0,schema3.getUnmodifiableListOfTables().size());
//		assertEquals("value should be correct",1,schema3.getUnmodifiableListOfConsumers().size());
//	}
	
//	@Test
//	public void test0002Table()
//	{
//		DBSchemaBow<?> schema1 = DBSchemaBowFactory.createSchema();
//		
//		schema1
//			.createOneOfTables().setName("EMPLOYEE").build()
//			.createOneOfTables().setName("ADDRESS").build()
//			.createOneOfTables().setName("TASKS").build();
//		
//		assertEquals("value should be correct",3,schema1.getUnmodifiableListOfTables().size());
//		assertEquals("value should be correct","EMPLOYEE",schema1.getUnmodifiableListOfTables().get(0).getName());
//		assertEquals("value should be correct","ADDRESS",schema1.getUnmodifiableListOfTables().get(1).getName());
//		assertEquals("value should be correct","TASKS",schema1.getUnmodifiableListOfTables().get(2).getName());
//		
//		TableBow<?> table = schema1.getUnmodifiableListOfTables().get(0)
//			.createUUIDColumn("ID", false).createPrimaryKey().build().build()
//			.createVarcharColumn("EMPLOYEE_NAME", false, 128).build()
//			.createBigIntAutoIncrementColumn("REC_NO", "SEQ_EMPLOYEE_NUMBER", null).build()
//			.createBooleanColumnWithDefault("REC_ENABLED", true).build()
//			.createTimestampColumnDefaultCurrent("REC_CREATED").build()
//			.createIndex(true, "UNQ1_EMPLOYEE", "EMPLOYEE_NAME").build();
//		
//		assertEquals("value should be correct",5,table.getUnmodifiableListOfColumns().size());
//		assertEquals("value should be correct",1,table.getUnmodifiableListOfIndices().size());
//	}
}
