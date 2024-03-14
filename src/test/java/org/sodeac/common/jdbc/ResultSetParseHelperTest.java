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
package org.sodeac.common.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Test;
import org.sodeac.common.jdbc.ResultSetParseHelper.ResultSetParseHelperBuilder;
import org.sodeac.common.jdbc.classicmodelcars.CustomerNodeType;
import org.sodeac.common.jdbc.classicmodelcars.EmployeeNodeType;
import org.sodeac.common.jdbc.classicmodelcars.OfficeNodeType;
import org.sodeac.common.jdbc.classicmodelcars.OfficeTreeModel;
import org.sodeac.common.jdbc.classicmodelcars.PaymentNodeType;
import org.sodeac.common.jdbc.classicmodelcars.ProductNodeType;
import org.sodeac.common.jdbc.classicmodelcars.OfficeTreeModel.OfficeResultSetNodeType;
import org.sodeac.common.jdbc.classicmodelcars.OrderDetailNodeType;
import org.sodeac.common.jdbc.classicmodelcars.OrderNodeType;
import org.sodeac.common.misc.CloseableCollector;
import org.sodeac.common.misc.ResourceLoader;
//import org.sodeac.common.model.dbschema.DBSchemaBowFactory;
import org.sodeac.common.typedtree.BranchNode;
import org.sodeac.common.typedtree.ModelRegistry;
import org.sodeac.common.typedtree.TypedTreeMetaModel.RootBranchNode;

public class ResultSetParseHelperTest
{
	@Test
	public void officeTest() throws Exception
	{
		try(CloseableCollector closeableCollector = CloseableCollector.newInstance())
		{
			DriverManager.registerDriver(org.h2.Driver.class.newInstance());
			
			String tempDir = System.getProperty("java.io.tmpdir");
			String database = "demo";
			
			// create database from dump
			
			if(tempDir.endsWith("\\")) {tempDir = tempDir.substring(0, tempDir.length() -1);}
			if(tempDir.endsWith("/")) {tempDir = tempDir.substring(0, tempDir.length() -1);}
			
			new File(tempDir + "/" + database + ".mv.db").delete();
			
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
			
			org.h2.tools.RunScript.execute("jdbc:h2:" + tempDir + "/" + database, "", "",tempDir +"/script.sql", null,false);
			
			new File(tempDir,"script.sql").delete();
			
			// run query
			
			List<String> rootIdList = new ArrayList<String>();
			AtomicInteger rootIdCount = new AtomicInteger();
			List<String> rootIdList2 = new ArrayList<String>();
			AtomicInteger rootIdCount2 = new AtomicInteger();
			AtomicInteger employeeWithoutCustomerCount = new AtomicInteger();
			
			RootBranchNode<OfficeTreeModel,OfficeResultSetNodeType> resultSetTree = ModelRegistry.getTypedTreeMetaModel(OfficeTreeModel.class).createRootNode(OfficeTreeModel.resultSet);
			
			String officeSQL = ResourceLoader.loadPackageFileAsString("office.sql", OfficeTreeModel.class);
			Connection conn = closeableCollector.register(DriverManager.getConnection("jdbc:h2:" + tempDir + "/" + database, "", ""));
			PreparedStatement preparedStatement = closeableCollector.register(conn.prepareStatement(officeSQL, ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY));
			ResultSetParseHelper parseHelper = ResultSetParseHelperBuilder.newBuilder
			(
				OfficeNodeType.OFFICECODE.getNodeName(), String.class, OfficeResultSetNodeType.officeList.getBranchNodeClass(),OfficeTreeModel.resultSet.getBranchNodeClass(),
					
				c -> c.getParentObject().create(OfficeResultSetNodeType.officeList)
					.setValue(OfficeNodeType.OFFICECODE,c.getId())
					.setValue(OfficeNodeType.OFFICECITY,c.getString(OfficeNodeType.OFFICECITY.getNodeName()))
					.setValue(OfficeNodeType.OFFICECOUNTRY,c.getString(OfficeNodeType.OFFICECOUNTRY.getNodeName()))
					.setValue(OfficeNodeType.OFFICEADDRESSLINE1,c.getString(OfficeNodeType.OFFICEADDRESSLINE1.getNodeName()))
					.setValue(OfficeNodeType.OFFICEADDRESSLINE2,c.getString(OfficeNodeType.OFFICEADDRESSLINE2.getNodeName()))
					.setValue(OfficeNodeType.OFFICEPOSTALCODE,c.getString(OfficeNodeType.OFFICEPOSTALCODE.getNodeName()))
					.setValue(OfficeNodeType.OFFICESTATE,c.getString(OfficeNodeType.OFFICESTATE.getNodeName()))
					.setValue(OfficeNodeType.OFFICETERRITORY,c.getString(OfficeNodeType.OFFICETERRITORY.getNodeName()))
					.setValue(OfficeNodeType.OFFICEPHONE,c.getString(OfficeNodeType.OFFICEPHONE.getNodeName())),
				r -> rootIdList.add(r.getValue(OfficeNodeType.OFFICECODE)),
				l -> rootIdCount.addAndGet(l.size())
			)
				.subParser(EmployeeNodeType.EMPLOYEENUMBER.getNodeName(), Integer.class, OfficeNodeType.EMPLOYEES.getBranchNodeClass(),
							
					c -> c.getParentObject().create(OfficeNodeType.EMPLOYEES)
						.setValue(EmployeeNodeType.EMPLOYEENUMBER,c.getId())
						.setValue(EmployeeNodeType.EMPLOYEEEXTENSION,c.getString(EmployeeNodeType.EMPLOYEEEXTENSION.getNodeName()))
						.setValue(EmployeeNodeType.EMPLOYEEJOBTITLE,c.getString(EmployeeNodeType.EMPLOYEEJOBTITLE.getNodeName()))
						.setValue(EmployeeNodeType.EMPLOYEEFIRSTNAME,c.getString(EmployeeNodeType.EMPLOYEEFIRSTNAME.getNodeName()))
						.setValue(EmployeeNodeType.EMPLOYEELASTNAME,c.getString(EmployeeNodeType.EMPLOYEELASTNAME.getNodeName()))
						.setValue(EmployeeNodeType.EMPLOYEEEMAIL,c.getString(EmployeeNodeType.EMPLOYEEEMAIL.getNodeName()))
						.setValue(EmployeeNodeType.EMPLOYEEREPORTSTO,c.getInteger(EmployeeNodeType.EMPLOYEEREPORTSTO.getNodeName()))
				)
					.subParser(CustomerNodeType.CUSTOMERNUMBER.getNodeName(), Integer.class, EmployeeNodeType.CUSTOMERS.getBranchNodeClass(),
							
						c -> c.getParentObject().create(EmployeeNodeType.CUSTOMERS)
							.setValue(CustomerNodeType.CUSTOMERNUMBER, c.getId())
							.setValue(CustomerNodeType.CUSTOMERNAME,c.getString(CustomerNodeType.CUSTOMERNAME.getNodeName()))
							.setValue(CustomerNodeType.CUSTOMERCONTACTLASTNAME,c.getString(CustomerNodeType.CUSTOMERCONTACTLASTNAME.getNodeName()))
							.setValue(CustomerNodeType.CUSTOMERCONTACTFIRSTNAME,c.getString(CustomerNodeType.CUSTOMERCONTACTFIRSTNAME.getNodeName()))
							.setValue(CustomerNodeType.CUSTOMERADDRESSLINE1,c.getString(CustomerNodeType.CUSTOMERADDRESSLINE1.getNodeName()))
							.setValue(CustomerNodeType.CUSTOMERADDRESSLINE2,c.getString(CustomerNodeType.CUSTOMERADDRESSLINE2.getNodeName()))
							.setValue(CustomerNodeType.CUSTOMERCITY,c.getString(CustomerNodeType.CUSTOMERCITY.getNodeName()))
							.setValue(CustomerNodeType.CUSTOMERCOUNTRY,c.getString(CustomerNodeType.CUSTOMERCOUNTRY.getNodeName()))
							.setValue(CustomerNodeType.CUSTOMERSTATE,c.getString(CustomerNodeType.CUSTOMERSTATE.getNodeName()))
							.setValue(CustomerNodeType.CUSTOMERPOSTALCODE,c.getString(CustomerNodeType.CUSTOMERPOSTALCODE.getNodeName()))
							.setValue(CustomerNodeType.CUSTOMERPHONE,c.getString(CustomerNodeType.CUSTOMERPHONE.getNodeName()))
							.setValue(CustomerNodeType.CUSTOMERCREDITLIMIT,c.getInteger(CustomerNodeType.CUSTOMERCREDITLIMIT.getNodeName()))
					).onNullRecord( c -> {employeeWithoutCustomerCount.incrementAndGet(); return null;} )
						.subParser(PaymentNodeType.PAYMENTID.getNodeName(), Integer.class, CustomerNodeType.PAYMENTS.getBranchNodeClass(),
								
							c -> c.getParentObject().create(CustomerNodeType.PAYMENTS)
								.setValue(PaymentNodeType.PAYMENTID, c.getId())
								.setValue(PaymentNodeType.PAYMENTCHECKNUMBER,c.getString(PaymentNodeType.PAYMENTCHECKNUMBER.getNodeName()))
								.setValue(PaymentNodeType.PAYMENTAMOUNT,c.getDouble(PaymentNodeType.PAYMENTAMOUNT.getNodeName()))
								.setValue(PaymentNodeType.PAYMENTDATE,c.getDate(PaymentNodeType.PAYMENTDATE.getNodeName()))
						).build()
						.subParser(OrderNodeType.ORDERNUMBER.getNodeName(), Integer.class, CustomerNodeType.ORDERS.getBranchNodeClass(),
								
							c -> c.getParentObject().create(CustomerNodeType.ORDERS)
								.setValue(OrderNodeType.ORDERNUMBER, c.getId())
								.setValue(OrderNodeType.ORDERREQUIREDDATE,c.getDate(OrderNodeType.ORDERREQUIREDDATE.getNodeName()))
								.setValue(OrderNodeType.ORDERORDERDATE,c.getDate(OrderNodeType.ORDERORDERDATE.getNodeName()))
								.setValue(OrderNodeType.ORDERSHIPPEDDATE,c.getDate(OrderNodeType.ORDERSHIPPEDDATE.getNodeName()))
								.setValue(OrderNodeType.ORDERCOMMENTS,c.getString(OrderNodeType.ORDERCOMMENTS.getNodeName()))
								.setValue(OrderNodeType.ORDERSTATUS,c.getString(OrderNodeType.ORDERSTATUS.getNodeName()))
						)
							.subParser(OrderDetailNodeType.ORDERDETAILID.getNodeName(), Integer.class, OrderNodeType.ORDERDETAILS.getBranchNodeClass(), 
								c -> c.getParentObject().create(OrderNodeType.ORDERDETAILS)
									.setValue(OrderDetailNodeType.ORDERDETAILID, c.getId())
									.setValue(OrderDetailNodeType.ORDERDETAILORDERLINENUMBER,c.getInteger(OrderDetailNodeType.ORDERDETAILORDERLINENUMBER.getNodeName()))
									.setValue(OrderDetailNodeType.ORDERDETAILPRICEEACH,c.getDouble(OrderDetailNodeType.ORDERDETAILPRICEEACH.getNodeName()))
									.setValue(OrderDetailNodeType.ORDERDETAILQUANTITYORDERED,c.getInteger(OrderDetailNodeType.ORDERDETAILQUANTITYORDERED.getNodeName()))
							)
								.subParser(ProductNodeType.PRODUCTCODE.getNodeName(), String.class, OrderDetailNodeType.PRODUCT.getBranchNodeClass(), 
									c -> c.getParentObject().create(OrderDetailNodeType.PRODUCT)
										.setValue(ProductNodeType.PRODUCTCODE, c.getId())
										.setValue(ProductNodeType.PRODUCTNAME,c.getString(ProductNodeType.PRODUCTNAME.getNodeName()))
										.setValue(ProductNodeType.PRODUCTLINE,c.getString(ProductNodeType.PRODUCTLINE.getNodeName()))
										.setValue(ProductNodeType.PRODUCTSCALE,c.getString(ProductNodeType.PRODUCTSCALE.getNodeName()))
										.setValue(ProductNodeType.PRODUCTVENDOR,c.getString(ProductNodeType.PRODUCTVENDOR.getNodeName()))
										.setValue(ProductNodeType.PRODUCTDESCRIPTION,c.getString(ProductNodeType.PRODUCTDESCRIPTION.getNodeName()))
										.setValue(ProductNodeType.PRODUCTQUANTITYINSTOCK,c.getInteger(ProductNodeType.PRODUCTQUANTITYINSTOCK.getNodeName()))
										.setValue(ProductNodeType.PRODUCTBUYPRICE,c.getDouble(ProductNodeType.PRODUCTBUYPRICE.getNodeName()))
										.setValue(ProductNodeType.PRODUCTMSRP,c.getDouble(ProductNodeType.PRODUCTMSRP.getNodeName()))
								)
			.newParsePhase
			(
				"Phase2", 
				OfficeNodeType.OFFICECODE.getNodeName(), 
				String.class, 
				String.class,
				OfficeTreeModel.resultSet.getBranchNodeClass(), 
				c -> c.getId(),
				r -> rootIdList2.add(r), 
				l -> rootIdCount2.addAndGet(l.size())
			)
			.buildParser();
				
			parseHelper.parse(preparedStatement,resultSetTree,3);
			parseHelper.close();
	
			closeableCollector.close(preparedStatement);
			
			// test result
			
			assertEquals("values should be equal", rootIdList.size(), rootIdCount.get());
			assertEquals("values should be equal", rootIdList, rootIdList2);
			assertEquals("values should be equal", rootIdCount.get(),rootIdCount2.get());
			
			PreparedStatement preparedStatementOffice = closeableCollector.register(conn.prepareStatement("SELECT * FROM OFFICES ORDER BY OFFICECODE"));
			PreparedStatement preparedStatementEmployee = closeableCollector.register(conn.prepareStatement("SELECT * FROM EMPLOYEES WHERE OFFICECODE = ? ORDER BY EMPLOYEENUMBER"));
			PreparedStatement preparedStatementCustomers = closeableCollector.register(conn.prepareStatement("SELECT * FROM CUSTOMERS WHERE SALESREPEMPLOYEENUMBER = ? ORDER BY CUSTOMERNUMBER"));
			PreparedStatement preparedStatementPayments = closeableCollector.register(conn.prepareStatement("SELECT * FROM PAYMENTS WHERE CUSTOMERNUMBER = ? ORDER BY ID"));
			PreparedStatement preparedStatementOrders = closeableCollector.register(conn.prepareStatement("SELECT * FROM ORDERS WHERE CUSTOMERNUMBER = ? ORDER BY ORDERNUMBER"));
			PreparedStatement preparedStatementOrderDetails = closeableCollector.register(conn.prepareStatement("SELECT * FROM ORDERDETAILS WHERE ORDERNUMBER = ? ORDER BY ORDERLINENUMBER"));
			PreparedStatement preparedStatementProduct = closeableCollector.register(conn.prepareStatement("SELECT * FROM PRODUCTS WHERE PRODUCTCODE = ?"));
			
			ResultSet resultSetOffice = preparedStatementOffice.executeQuery();
			try
			{
				Iterator<BranchNode<OfficeResultSetNodeType, OfficeNodeType>> iteratorOffice = resultSetTree.getUnmodifiableNodeList(OfficeResultSetNodeType.officeList).iterator();
				
				while(resultSetOffice.next())
				{
					assertTrue("iteratorOffice.next should returns true", iteratorOffice.hasNext());
					BranchNode<OfficeResultSetNodeType, OfficeNodeType> officeNode = iteratorOffice.next();
					
					assertEquals("values should be equal", resultSetOffice.getString("OFFICECODE"), officeNode.getValue(OfficeNodeType.OFFICECODE));
					assertEquals("values should be equal", resultSetOffice.getString("CITY"), officeNode.getValue(OfficeNodeType.OFFICECITY));
					assertEquals("values should be equal", resultSetOffice.getString("PHONE"), officeNode.getValue(OfficeNodeType.OFFICEPHONE));
					assertEquals("values should be equal", resultSetOffice.getString("ADDRESSLINE1"), officeNode.getValue(OfficeNodeType.OFFICEADDRESSLINE1));
					assertEquals("values should be equal", resultSetOffice.getString("ADDRESSLINE2"), officeNode.getValue(OfficeNodeType.OFFICEADDRESSLINE2));
					assertEquals("values should be equal", resultSetOffice.getString("STATE"), officeNode.getValue(OfficeNodeType.OFFICESTATE));
					assertEquals("values should be equal", resultSetOffice.getString("COUNTRY"), officeNode.getValue(OfficeNodeType.OFFICECOUNTRY));
					assertEquals("values should be equal", resultSetOffice.getString("POSTALCODE"), officeNode.getValue(OfficeNodeType.OFFICEPOSTALCODE));
					assertEquals("values should be equal", resultSetOffice.getString("TERRITORY"), officeNode.getValue(OfficeNodeType.OFFICETERRITORY));
					
					assertTrue("list should contains value",rootIdList.contains(officeNode.getValue(OfficeNodeType.OFFICECODE)));
					
					preparedStatementEmployee.setString(1, officeNode.getValue(OfficeNodeType.OFFICECODE));
					ResultSet resultSetEmployee = preparedStatementEmployee.executeQuery();
					try
					{
						Iterator<BranchNode<OfficeNodeType,EmployeeNodeType>> iteratorEmployee = officeNode.getUnmodifiableNodeList(OfficeNodeType.EMPLOYEES).iterator();
						while(resultSetEmployee.next())
						{
							
							assertTrue("iteratorEmployee.next should returns true", iteratorEmployee.hasNext());
							BranchNode<OfficeNodeType,EmployeeNodeType> employee = iteratorEmployee.next();
							
							assertEquals("values should be equal", (Integer)resultSetEmployee.getInt("EMPLOYEENUMBER"), employee.getValue(EmployeeNodeType.EMPLOYEENUMBER));
							assertEquals("values should be equal", resultSetEmployee.getString("LASTNAME"), employee.getValue(EmployeeNodeType.EMPLOYEELASTNAME));
							assertEquals("values should be equal", resultSetEmployee.getString("FIRSTNAME"), employee.getValue(EmployeeNodeType.EMPLOYEEFIRSTNAME));
							assertEquals("values should be equal", resultSetEmployee.getString("EXTENSION"), employee.getValue(EmployeeNodeType.EMPLOYEEEXTENSION));
							assertEquals("values should be equal", resultSetEmployee.getString("EMAIL"), employee.getValue(EmployeeNodeType.EMPLOYEEEMAIL));
							assertEquals("values should be equal", resultSetEmployee.getString("JOBTITLE"), employee.getValue(EmployeeNodeType.EMPLOYEEJOBTITLE));
							assertEquals("values should be equal", (Integer)resultSetEmployee.getInt("REPORTSTO"), employee.getValue(EmployeeNodeType.EMPLOYEEREPORTSTO) == null ? new Integer(0) : employee.getValue(EmployeeNodeType.EMPLOYEEREPORTSTO));
							
							preparedStatementCustomers.setInt(1,employee.getValue(EmployeeNodeType.EMPLOYEENUMBER));
							
							ResultSet resultSetCustomer = preparedStatementCustomers.executeQuery();
							try
							{
								if(employee.getUnmodifiableNodeList(EmployeeNodeType.CUSTOMERS).isEmpty())
								{
									employeeWithoutCustomerCount.decrementAndGet();
								}
								Iterator<BranchNode<EmployeeNodeType,CustomerNodeType>> iteratorCustomer = employee.getUnmodifiableNodeList(EmployeeNodeType.CUSTOMERS).iterator();
								
								while(resultSetCustomer.next())
								{
									assertTrue("iteratorCustomer.next should returns true", iteratorCustomer.hasNext());
									BranchNode<EmployeeNodeType,CustomerNodeType> customer = iteratorCustomer.next();
									
									assertEquals("values should be equal", (Integer)resultSetCustomer.getInt("CUSTOMERNUMBER"), customer.getValue(CustomerNodeType.CUSTOMERNUMBER));
									assertEquals("values should be equal", resultSetCustomer.getString("CUSTOMERNAME"), customer.getValue(CustomerNodeType.CUSTOMERNAME));
									assertEquals("values should be equal", resultSetCustomer.getString("CONTACTLASTNAME"), customer.getValue(CustomerNodeType.CUSTOMERCONTACTLASTNAME));
									assertEquals("values should be equal", resultSetCustomer.getString("CONTACTFIRSTNAME"), customer.getValue(CustomerNodeType.CUSTOMERCONTACTFIRSTNAME));
									assertEquals("values should be equal", resultSetCustomer.getString("PHONE"), customer.getValue(CustomerNodeType.CUSTOMERPHONE));
									assertEquals("values should be equal", resultSetCustomer.getString("ADDRESSLINE1"), customer.getValue(CustomerNodeType.CUSTOMERADDRESSLINE1));
									assertEquals("values should be equal", resultSetCustomer.getString("ADDRESSLINE2"), customer.getValue(CustomerNodeType.CUSTOMERADDRESSLINE2));
									assertEquals("values should be equal", resultSetCustomer.getString("CITY"), customer.getValue(CustomerNodeType.CUSTOMERCITY));
									assertEquals("values should be equal", resultSetCustomer.getString("STATE"), customer.getValue(CustomerNodeType.CUSTOMERSTATE));
									assertEquals("values should be equal", resultSetCustomer.getString("POSTALCODE"), customer.getValue(CustomerNodeType.CUSTOMERPOSTALCODE));
									assertEquals("values should be equal", resultSetCustomer.getString("COUNTRY"), customer.getValue(CustomerNodeType.CUSTOMERCOUNTRY));
									assertEquals("values should be equal", (Integer)resultSetCustomer.getInt("CREDITLIMIT"), customer.getValue(CustomerNodeType.CUSTOMERCREDITLIMIT));
									
									Iterator<BranchNode<CustomerNodeType,PaymentNodeType>> iteratorPayment = customer.getUnmodifiableNodeList(CustomerNodeType.PAYMENTS).iterator();
									preparedStatementPayments.setInt(1, customer.getValue(CustomerNodeType.CUSTOMERNUMBER));
									ResultSet resultSetPayment = preparedStatementPayments.executeQuery();
									try
									{
										while(resultSetPayment.next())
										{
											assertTrue("iteratorPayment.next should returns true", iteratorPayment.hasNext());
											BranchNode<CustomerNodeType,PaymentNodeType> payment = iteratorPayment.next();
											
											assertEquals("values should be equal", (Integer)resultSetPayment.getInt("ID"), payment.getValue(PaymentNodeType.PAYMENTID));
											assertEquals("values should be equal", resultSetPayment.getString("CHECKNUMBER"), payment.getValue(PaymentNodeType.PAYMENTCHECKNUMBER));
											assertEquals("values should be equal", (Double)resultSetPayment.getDouble("AMOUNT"), payment.getValue(PaymentNodeType.PAYMENTAMOUNT));
											assertEquals("values should be equal", resultSetPayment.getDate("PAYMENTDATE"), payment.getValue(PaymentNodeType.PAYMENTDATE));
											
										}
										assertFalse("iteratorPayment.next should returns false", iteratorPayment.hasNext());
									}
									finally 
									{
										resultSetPayment.close();
										resultSetPayment = null;
									}
									
									Iterator<BranchNode<CustomerNodeType,OrderNodeType>> iteratorOrders = customer.getUnmodifiableNodeList(CustomerNodeType.ORDERS).iterator();
									preparedStatementOrders.setInt(1, customer.getValue(CustomerNodeType.CUSTOMERNUMBER));
									ResultSet resultSetOrders = preparedStatementOrders.executeQuery();
									try
									{
										
										while(resultSetOrders.next())
										{
											assertTrue("iteratorOrders.next should returns true", iteratorOrders.hasNext());
											BranchNode<CustomerNodeType,OrderNodeType> order = iteratorOrders.next();
											
											assertEquals("values should be equal", (Integer)resultSetOrders.getInt("ORDERNUMBER"), order.getValue(OrderNodeType.ORDERNUMBER));
											assertEquals("values should be equal", resultSetOrders.getDate("ORDERDATE"), order.getValue(OrderNodeType.ORDERORDERDATE));
											assertEquals("values should be equal", resultSetOrders.getDate("REQUIREDDATE"), order.getValue(OrderNodeType.ORDERREQUIREDDATE));
											assertEquals("values should be equal", resultSetOrders.getDate("SHIPPEDDATE"), order.getValue(OrderNodeType.ORDERSHIPPEDDATE));
											assertEquals("values should be equal", resultSetOrders.getString("STATUS"), order.getValue(OrderNodeType.ORDERSTATUS));
											assertEquals("values should be equal", resultSetOrders.getString("COMMENTS"), order.getValue(OrderNodeType.ORDERCOMMENTS));
					
											Iterator<BranchNode<OrderNodeType, OrderDetailNodeType>> iteratorOrderDetails = order.getUnmodifiableNodeList(OrderNodeType.ORDERDETAILS).iterator();
											preparedStatementOrderDetails.setInt(1, order.getValue(OrderNodeType.ORDERNUMBER));
											ResultSet resultSetOrderDetails = preparedStatementOrderDetails.executeQuery();
											try
											{
												while(resultSetOrderDetails.next())
												{
													assertTrue("iteratorOrderDetails.next should returns true", iteratorOrderDetails.hasNext());
													BranchNode<OrderNodeType, OrderDetailNodeType> detail = iteratorOrderDetails.next();
													
													assertEquals("values should be equal", (Integer)resultSetOrderDetails.getInt("ID"), detail.getValue(OrderDetailNodeType.ORDERDETAILID));
													assertEquals("values should be equal", (Integer)resultSetOrderDetails.getInt("ORDERLINENUMBER"), detail.getValue(OrderDetailNodeType.ORDERDETAILORDERLINENUMBER));
													assertEquals("values should be equal", (Integer)resultSetOrderDetails.getInt("QUANTITYORDERED"), detail.getValue(OrderDetailNodeType.ORDERDETAILQUANTITYORDERED));
													assertEquals("values should be equal", (Double)resultSetOrderDetails.getDouble("PRICEEACH"), detail.getValue(OrderDetailNodeType.ORDERDETAILPRICEEACH));
													
													String productCode = resultSetOrderDetails.getString("PRODUCTCODE");if(resultSetOrderDetails.wasNull()) {productCode = null;}
													
													if(productCode == null)
													{
														assertNotNull("value should be null", detail.get(OrderDetailNodeType.PRODUCT));
													}
													else
													{
														preparedStatementProduct.setString(1, productCode);
														ResultSet resultSetProduct = preparedStatementProduct.executeQuery();
														try
														{
															assertTrue("resultSetProduct.next should be true", resultSetProduct.next());
															BranchNode<OrderDetailNodeType, ProductNodeType> product = detail.get(OrderDetailNodeType.PRODUCT);
															assertNotNull("value should not be null",product);
															
															assertEquals("values should be equal", resultSetOrderDetails.getString("PRODUCTCODE"), product.getValue(ProductNodeType.PRODUCTCODE));
															assertEquals("values should be equal", resultSetProduct.getString("PRODUCTCODE"), product.getValue(ProductNodeType.PRODUCTCODE));
															assertEquals("values should be equal", resultSetProduct.getString("PRODUCTNAME"), product.getValue(ProductNodeType.PRODUCTNAME));
															assertEquals("values should be equal", resultSetProduct.getString("PRODUCTLINE"), product.getValue(ProductNodeType.PRODUCTLINE));
															assertEquals("values should be equal", resultSetProduct.getString("PRODUCTSCALE"), product.getValue(ProductNodeType.PRODUCTSCALE));
															assertEquals("values should be equal", resultSetProduct.getString("PRODUCTVENDOR"), product.getValue(ProductNodeType.PRODUCTVENDOR));
															assertEquals("values should be equal", resultSetProduct.getString("PRODUCTDESCRIPTION"), product.getValue(ProductNodeType.PRODUCTDESCRIPTION));
															assertEquals("values should be equal", (Integer)resultSetProduct.getInt("QUANTITYINSTOCK"), product.getValue(ProductNodeType.PRODUCTQUANTITYINSTOCK));
															assertEquals("values should be equal", (Double)resultSetProduct.getDouble("BUYPRICE"), product.getValue(ProductNodeType.PRODUCTBUYPRICE));
															assertEquals("values should be equal", (Double)resultSetProduct.getDouble("MSRP"), product.getValue(ProductNodeType.PRODUCTMSRP));
															
															assertFalse("resultSetProduct.next should be false", resultSetProduct.next());
														}
														finally 
														{
															resultSetProduct.close();
														}
													}
												}
												assertFalse("iteratorOrderDetails.next should returns false", iteratorOrderDetails.hasNext());
												
											}
											finally 
											{
												resultSetOrderDetails.close();
											}
										}
										
										assertFalse("iteratorOrders.next should returns false", iteratorOrders.hasNext());
									}
									finally 
									{
										resultSetOrders.close();
									}
								}
								
								assertFalse("iteratorCustomer.next should returns false", iteratorCustomer.hasNext());
							}
							finally 
							{
								resultSetCustomer.close();
							}
						}
						
						assertFalse("iteratorEmployee.next should returns false", iteratorEmployee.hasNext());
					}
					finally 
					{
						resultSetEmployee.close();
					}
				}
				assertFalse("iteratorOffice.next should returns false", iteratorOffice.hasNext());
			}
			finally 
			{
				resultSetOffice.close();
			}
			
			assertEquals("counter should be correct", 0, employeeWithoutCustomerCount.get());
			
			closeableCollector.close(preparedStatementOffice);
			closeableCollector.close(preparedStatementEmployee);
			closeableCollector.close(preparedStatementCustomers);
			closeableCollector.close(preparedStatementPayments);
			closeableCollector.close(preparedStatementOrders);
			closeableCollector.close(preparedStatementOrderDetails);
			closeableCollector.close(preparedStatementProduct);
			
			
			closeableCollector.close(conn);
			new File(tempDir + "/" + database + ".mv.db").delete();
		}
	}
	
//	@Test
//	public void getterTest() throws Exception
//	{
//		try(CloseableCollector closeableCollector = CloseableCollector.newInstance())
//		{
//			DriverManager.registerDriver(org.h2.Driver.class.newInstance());
//			
//			Connection connection = closeableCollector.register(DriverManager.getConnection("jdbc:h2:mem:"));
//			
//			connection.setAutoCommit(false);
//			
//			DBSchemaUtils.get(connection).adaptSchema
//			(
//				DBSchemaBowFactory.createSchema("GETTER-TEST").createOneOfTables().setName("GETTER_TEST")
//					.createBigIntColumn("ID", false).createPrimaryKey().build().build()
//					.createVarcharColumn("COL_STRING", true, 108).build()
//					.createSmallIntColumn("COL_SMALLINT", true).build()
//					.createIntegerColumn("COL_INT", true).build()
//					.createBigIntColumn("COL_BIGINT", true).build()
//					.createRealColumn("COL_REAL", true).build()
//					.createDoubleColumn("COL_DOUBLE", true).build()
//					.createTimestampColumn("COL_TS", true).build()
//					.createDateColumn("COL_DATE", true).build()
//					.createTimeColumn("COL_TIME", true).build()
//					.createUUIDColumn("COL_UUID", true).build()
//					.createBinaryColumn("COL_BINARY", true).build()
//				.build().setLogUpdates(false)
//			);
//			
//			connection.commit();
//			
//			PreparedStatement preparedStatementInsert = closeableCollector.register(connection.prepareStatement
//			(
//				"INSERT INTO GETTER_TEST (ID,COL_STRING,COL_SMALLINT,COL_INT,COL_BIGINT,COL_REAL,COL_DOUBLE,COL_TS,COL_DATE,COL_TIME,COL_UUID,COL_BINARY) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"
//			));
//			
//			Calendar calTS = Calendar.getInstance(); calTS.set(Calendar.MILLISECOND, 0);
//			Date now = calTS.getTime(); 
//			UUID uuid = UUID.randomUUID();
//			
//			preparedStatementInsert.setLong(1, 1L);
//			preparedStatementInsert.setString(2, "TWO");
//			preparedStatementInsert.setShort(3, (short)3);
//			preparedStatementInsert.setInt(4, 4);
//			preparedStatementInsert.setLong(5, 5L);
//			preparedStatementInsert.setFloat(6, 6.6f);
//			preparedStatementInsert.setDouble(7, 7.7d);
//			preparedStatementInsert.setTimestamp(8, new Timestamp(now.getTime()));
//			preparedStatementInsert.setDate(9, new java.sql.Date(now.getTime()));
//			preparedStatementInsert.setTime(10, new java.sql.Time(now.getTime()));
//			preparedStatementInsert.setObject(11, uuid);
//			preparedStatementInsert.setBytes(12, "TWELVE".getBytes());
//			
//			preparedStatementInsert.executeUpdate();
//			
//			preparedStatementInsert.setLong(1, 2L);
//			preparedStatementInsert.setNull(2, Types.VARCHAR);
//			preparedStatementInsert.setNull(3, Types.SMALLINT);
//			preparedStatementInsert.setNull(4, Types.INTEGER);
//			preparedStatementInsert.setNull(5, Types.BIGINT);
//			preparedStatementInsert.setNull(6, Types.FLOAT);
//			preparedStatementInsert.setNull(7, Types.DOUBLE);
//			preparedStatementInsert.setNull(8, Types.TIMESTAMP);
//			preparedStatementInsert.setNull(9, Types.DATE);
//			preparedStatementInsert.setNull(10, Types.TIME);
//			preparedStatementInsert.setObject(11, null);
//			preparedStatementInsert.setNull(12, Types.BINARY);
//			
//			preparedStatementInsert.executeUpdate();
//			
//			connection.commit();
//			
//			Calendar calDate = Calendar.getInstance(); calDate.setTime(now);
//			calDate.set(Calendar.SECOND, 0);
//			calDate.set(Calendar.MINUTE, 0);
//			calDate.set(Calendar.HOUR_OF_DAY, 0);
//			
//			Calendar calTime = Calendar.getInstance(); calTime.setTime(now);
//			calTime.set(Calendar.YEAR, 1970);
//			calTime.set(Calendar.MONTH, 0);
//			calTime.set(Calendar.DAY_OF_MONTH, 1);
//			
//			AtomicLong expectedId = new AtomicLong(1L);
//			
//			ResultSetParseHelper parseHelper = closeableCollector.register(ResultSetParseHelperBuilder.newBuilder
//			(
//				"ID", Long.class, Object.class,Object.class,
//				c -> 
//				{
//					assertEquals("id should be correct", expectedId.getAndIncrement(), c.getId().longValue());
//					
//					if(c.getId().longValue() == 1)
//					{
//						assertEquals("value should be correct", "TWO", c.getString("COL_STRING"));
//						assertEquals("value should be correct", 3, (int) c.getShort("COL_SMALLINT").shortValue());
//						assertEquals("value should be correct", 4, c.getInteger("COL_INT").intValue());
//						assertEquals("value should be correct", 5, c.getLong("COL_BIGINT").longValue());
//						assertEquals("value should be correct", 6.6, c.getFloat("COL_REAL").floatValue(),0.01);
//						assertEquals("value should be correct", 7.7, c.getDouble("COL_DOUBLE").doubleValue(),0.01);
//						assertEquals("value should be correct", now.getTime(), c.getTimestamp("COL_TS").getTime());
//						assertEquals("value should be correct", calDate.getTimeInMillis(), c.getDate("COL_DATE").getTime());
//						assertEquals("value should be correct", calTime.getTimeInMillis(), c.getTime("COL_TIME").getTime());
//						assertEquals("value should be correct", uuid, c.getUUID("COL_UUID"));
//						assertEquals("value should be correct", uuid, c.getUUIDFromString("COL_UUID"));
//						assertEquals("value should be correct", "TWELVE", new String(c.getBytes("COL_BINARY")));
//					}
//					
//					if(c.getId().longValue() == 2)
//					{
//						assertNull("value should be correct", c.getString("COL_STRING"));
//						assertNull("value should be correct", c.getShort("COL_SMALLINT"));
//						assertNull("value should be correct", c.getInteger("COL_INT"));
//						assertNull("value should be correct", c.getLong("COL_BIGINT"));
//						assertNull("value should be correct", c.getFloat("COL_REAL"));
//						assertNull("value should be correct", c.getFloat("COL_DOUBLE"));
//						assertNull("value should be correct", c.getTimestamp("COL_TS"));
//						assertNull("value should be correct", c.getDate("COL_DATE"));
//						assertNull("value should be correct", c.getTime("COL_TIME"));
//						assertNull("value should be correct", c.getUUID("COL_UUID"));
//						assertNull("value should be correct", c.getUUIDFromString("COL_UUID"));
//						assertNull("value should be correct", c.getBytes("COL_BINARY"));
//					}
//						
//					return new Object();
//				}
//			).buildParser());
//			
//			parseHelper.parse
//			(
//				closeableCollector.register(connection.prepareStatement
//				(
//					"SELECT * FROM GETTER_TEST ORDER BY ID", ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY)
//				),
//				new Object(),
//				3
//			);
//			
//			assertEquals("id should be correct", 3L, expectedId.getAndIncrement());
//			
//		}
//	}
	
}
