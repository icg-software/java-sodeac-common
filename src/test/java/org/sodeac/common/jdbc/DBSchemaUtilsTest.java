package org.sodeac.common.jdbc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Test;
import org.sodeac.common.misc.CloseableCollector;

public class DBSchemaUtilsTest
{
    @Test
    public void t00001h2DriverTest() throws Exception
    {
        try (CloseableCollector closeableCollector = CloseableCollector.newInstance())
        {
            DriverManager.registerDriver(org.h2.Driver.class.newInstance());
            
            String tempDir = System.getProperty("java.io.tmpdir");
            String database = "drivertest";
            
            // create database from dump
            
            if (tempDir.endsWith("\\"))
            {
                tempDir = tempDir.substring(0, tempDir.length() - 1);
            }
            if (tempDir.endsWith("/"))
            {
                tempDir = tempDir.substring(0, tempDir.length() - 1);
            }
            
            new File(tempDir + "/" + database + ".mv.db").delete();
            
            int len;
            byte[] buffer = new byte[1080];
            ZipInputStream zis = closeableCollector.register(new ZipInputStream(new FileInputStream("./src/test/resources/classicmodelcars.zip")));
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null)
            {
                if (!"script.sql".equals(zipEntry.getName()))
                {
                    continue;
                }
                FileOutputStream fos = closeableCollector.register(new FileOutputStream(new File(tempDir, zipEntry.getName())));
                while ((len = zis.read(buffer)) > 0)
                {
                    fos.write(buffer, 0, len);
                }
                closeableCollector.close(fos);
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            
            org.h2.tools.RunScript.execute("jdbc:h2:" + tempDir + "/" + database, "", "", tempDir + "/script.sql", null, false);
            
            new File(tempDir, "script.sql").delete();
            
            Connection conn = closeableCollector.register(DriverManager.getConnection("jdbc:h2:" + tempDir + "/" + database, "", ""));
            DBSchemaUtils dbUtils = DBSchemaUtils.get(conn);
            
            closeableCollector.close(conn);
            new File(tempDir + "/" + database + ".mv.db").delete();
        }
    }
}
