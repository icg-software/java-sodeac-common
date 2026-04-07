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
package org.sodeac.common.misc;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;

public class Converter
{
    private static final String EMPTY_STRING = "";

    public static final Function<String, Long> StringToLong = p -> p == null || p.isEmpty() ? null : Long.parseLong(p);
    public static final Function<Long, String> LongToString = p -> p == null ? EMPTY_STRING : Long.toString(p);

    public static final Function<String, Integer> StringToInteger = p -> p == null || p.isEmpty() ? null : Integer.parseInt(p);
    public static final Function<Integer, String> IntegerToString = p -> p == null ? EMPTY_STRING : Integer.toString(p);

    public static final Function<String, Short> StringToShort = p -> p == null || p.isEmpty() ? null : Short.parseShort(p);
    public static final Function<Short, String> ShortToString = p -> p == null ? EMPTY_STRING : Short.toString(p);

    public static final Function<String, Double> StringToDouble = p -> p == null || p.isEmpty() ? null : Double.parseDouble(p);
    public static final Function<Double, String> DoubleToString = p -> p == null ? EMPTY_STRING : Double.toString(p);

    public static final Function<String, Boolean> StringToBoolean = p -> p == null || p.isEmpty() ? null : Boolean.parseBoolean(p);
    public static final Function<Boolean, String> BooleanToString = p -> p == null ? EMPTY_STRING : Boolean.toString(p);

    public static final Function<String, UUID> StringToUUID = p -> p == null || p.isEmpty() ? null : UUID.fromString(p);
    public static final Function<UUID, String> UUIDToString = p -> p == null ? EMPTY_STRING : p.toString();

    public static final Function<String, Version> StringToVersion = p -> p == null || p.isEmpty() ? null : Version.fromString(p);
    public static final Function<Version, String> VersionToString = p -> p == null ? EMPTY_STRING : p.toString();

    private static final String DEFAULT_KEY = "sdc://identifier.specs/org.sodeac.encryption/key/default";
	
	/*private static final SimpleDateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	private static final DecimalFormat TWO_DIGITS_FORMAT = new DecimalFormat("00");
	
	public static final Function<String, Date> ISO8601ToDate = p -> 
	{
		if(p == null || p.isEmpty())
		{
			return null;
		}
		try
		{
			TimeZone timeZone = TimeZone.getDefault(); //local JVM time zone
			ISO8601_FORMAT.setTimeZone(timeZone);
			return ISO8601_FORMAT.parse(p); 
		}
		catch (ParseException e) 
		{
			throw new RuntimeException(e);
		}
	};
	public static final Function<Date, String> DateToISO8601 = p -> 
	{
		if(p == null)
		{
			return EMPTY_STRING;
		}
		TimeZone timeZone = TimeZone.getDefault(); //local JVM time zone
		ISO8601_FORMAT.setTimeZone(timeZone);
		
		int offset = ISO8601_FORMAT.getTimeZone().getOffset(p.getTime());
		String sign = "+";
		
		if (offset < 0)
		{
			offset = -offset;
			sign = "-";
		}
		int hours = offset / 3600000;
		int minutes = (offset - hours * 3600000) / 60000;
		
		return ISO8601_FORMAT.format(p) + sign + TWO_DIGITS_FORMAT.format(hours) + ":" + TWO_DIGITS_FORMAT.format(minutes);
	};*/

    public static final Function<String, Date> ISO8601ToDate = p ->
    {
        try
        {
            SimpleDateFormat ISO8601Local = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            TimeZone timeZone = TimeZone.getDefault(); // local JVM time zone
            ISO8601Local.setTimeZone(timeZone);
            return ISO8601Local.parse(p);
        }
        catch (ParseException e)
        {
            throw new RuntimeException(e);
        }
    };

    public static final Function<Date, String> DateToISO8601 = p ->
    {
        SimpleDateFormat ISO8601Local = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        TimeZone timeZone = TimeZone.getDefault(); // local JVM time zone
        ISO8601Local.setTimeZone(timeZone);

        DecimalFormat twoDigits = new DecimalFormat("00");

        int offset = ISO8601Local.getTimeZone().getOffset(p.getTime());
        String sign = "+";

        if(offset < 0)
        {
            offset = -offset;
            sign = "-";
        }
        int hours = offset / 3600000;
        int minutes = (offset - hours * 3600000) / 60000;

        String ISO8601Now = ISO8601Local.format(p) + sign + twoDigits.format(hours) + ":" + twoDigits.format(minutes);
        return ISO8601Now;
    };

    public static final Function<String, Class> StringToClass = p ->
    {
        if(p == null || p.isEmpty())
        {
            return null;
        }

        JsonReader reader = Json.createReader(new StringReader(p));
        try
        {
            JsonObject clazzObject = reader.readObject();

            String className = clazzObject.getString("class");

            if(OSGiUtils.isOSGi())
            {
                String bundleSymbolicName = clazzObject.getString("bundlesymbolicname");
                String bundleVersion = clazzObject.getString("bundleversion");

                Class clazz = OSGiUtils.loadClass(className, bundleSymbolicName, bundleVersion);
                if(clazz == null)
                {
                    throw new RuntimeWrappedException(new ClassNotFoundException());
                }
                return clazz;
            }
            else
            {
                try
                {
                    return Class.forName(className);
                }
                catch (ClassNotFoundException e)
                {
                    throw new RuntimeWrappedException(e);
                }
            }
        }
        finally
        {
            reader.close();
        }

    };
    public static final Function<Class, String> ClassToString = p ->
    {
        if(p == null)
        {
            return EMPTY_STRING;
        }

        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        jsonBuilder.add("class", p.getCanonicalName());

        if(OSGiUtils.isOSGi())
        {
            String bundleName = OSGiUtils.getSymbolicName(p);
            String bundleVersion = OSGiUtils.getVersion(p);

            jsonBuilder.add("bundlesymbolicname", bundleName == null ? "" : bundleName);
            jsonBuilder.add("bundleversion", bundleVersion == null ? "" : bundleVersion);
        }

        return jsonBuilder.build().toString();
    };

    public static final Function<InputStream, Document> StreamToDocument = (s) ->
    {
        try
        {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            return dBuilder.parse(s);
        }
        catch (Exception e)
        {
            if(e instanceof RuntimeException)
            {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    };

    public static final BiConsumer<Document, OutputStream> DocumentToStream = (d, s) ->
    {
        DOMImplementation domImplementation = d.getImplementation();
        DOMImplementationLS domImplementationLS = (DOMImplementationLS) domImplementation.getFeature("LS", "3.0");
        LSSerializer lsSerializer = domImplementationLS.createLSSerializer();
        LSOutput lsOutput = domImplementationLS.createLSOutput();
        lsOutput.setEncoding("UTF-8");
        lsOutput.setByteStream(s);
        lsSerializer.write(d, lsOutput);

        try
        {
            s.flush();
        }
        catch (Exception e) { }
    };

    public static final BiConsumer<Document, OutputStream> DocumentToPrettyStream = (d, s) ->
    {
        DOMImplementation domImplementation = d.getImplementation();
        if(domImplementation.hasFeature("LS", "3.0") && domImplementation.hasFeature("Core", "2.0"))
        {
            DOMImplementationLS domImplementationLS = (DOMImplementationLS) domImplementation.getFeature("LS", "3.0");
            LSSerializer lsSerializer = domImplementationLS.createLSSerializer();
            DOMConfiguration domConfiguration = lsSerializer.getDomConfig();
            if(domConfiguration.canSetParameter("format-pretty-print", Boolean.TRUE))
            {
                lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
            }
            LSOutput lsOutput = domImplementationLS.createLSOutput();
            lsOutput.setEncoding("UTF-8");
            lsOutput.setByteStream(s);
            lsSerializer.write(d, lsOutput);

            try
            {
                s.flush();
            }
            catch (Exception e) { }
        }
        else
        {
            throw new RuntimeException("DOM 3.0 LS and/or DOM 2.0 Core not supported.");
        }
    };

    public static final BiFunction<Double, Integer, Double> roundDouble = (p, d) ->
    {
        if(p == null)
        {
            return null;
        }

        if(d == null)
        {
            d = 0;
        }

        BigDecimal bigDecimal = BigDecimal.valueOf(p);
        if(bigDecimal.scale() <= d)
        {
            return p;
        }

        String stringValueForScalePP = bigDecimal.setScale(d + 1, RoundingMode.HALF_UP).toPlainString();
        if(stringValueForScalePP.endsWith("5"))
        {
            // Case 1: scale++ ends with '5' => round up
            // Converter.roundDouble.apply(1.004999999999998,2) results in 1.01 (instead of 1.00 by  BigDecimal.valueOf(1.004999999999998).setScale(2,RoundingMode.HALF_UP))

            bigDecimal = bigDecimal.setScale(d, RoundingMode.UP);
        }
        else
        {
            // Case 2: round half up
            bigDecimal = bigDecimal.setScale(d, RoundingMode.HALF_UP);
        }
        return bigDecimal.doubleValue();
    };

    public static final BiFunction<Double, Integer, Double> roundDoubleRecursive = (p, d) ->
    {
        if(p == null)
        {
            return null;
        }

        if((d == null) || (d < 0))
        {
            d = 0;
        }

        BigDecimal bigDecimal = BigDecimal.valueOf(p);
        if(bigDecimal.scale() <= d)
        {
            return p;
        }

        if(bigDecimal.scale() == (d + 1))
        {
            // Case 1: recursion == simple round half up
            return bigDecimal.setScale(d, RoundingMode.HALF_UP).doubleValue();
        }

        String stringValueForScalePP = bigDecimal.setScale(d + 1, RoundingMode.HALF_UP).toPlainString();
        if(stringValueForScalePP.endsWith("5"))
        {
            // Case 2: scale++ ends with '5' => recursion would not change the result compared to a round up
            // Converter.roundDouble.apply(1.004999999999998,2) results in 1.01 (instead of 1.00 by  BigDecimal.valueOf(1.004999999999998).setScale(2,RoundingMode.HALF_UP))

            bigDecimal = bigDecimal.setScale(d, RoundingMode.UP);
        }
        else if(stringValueForScalePP.endsWith("4"))
        {
            // Case 3: recursion is required, maybe the the digit on position scale++ change from 4 to 5
            // Converter.roundDouble.apply(0.7744999999999999,2) results in 0.78 (instead of 0.77 by  BigDecimal.valueOf(0.7744999999999999).setScale(2,RoundingMode.HALF_UP))

            while (bigDecimal.scale() > d)
            {
                bigDecimal = bigDecimal.setScale(bigDecimal.scale() - 1, RoundingMode.HALF_UP);
            }
        }
        else
        {
            // Case 4: recursion would not produce a different result compared to a simple round half up
            bigDecimal = bigDecimal.setScale(d, RoundingMode.HALF_UP);
        }
        return bigDecimal.doubleValue();
    };

    public static final BiFunction<InputStream, String, InputStream> CryptedInputStreamToInputStream = (s, k) ->
    {
        if((k == null) || k.isEmpty())
        {
            k = DEFAULT_KEY;
        }

        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedKey = digest.digest(k.getBytes(StandardCharsets.UTF_8));

            MessageDigest vectorDigest = MessageDigest.getInstance("MD5");
            byte[] vectorBytes = vectorDigest.digest(k.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec iv = new IvParameterSpec(vectorBytes);
            SecretKeySpec keySpec = new SecretKeySpec(encodedKey, "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);

            return new GZIPInputStream(new CipherInputStream(s, cipher));
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeWrappedException(e);
        }
    };

    public static final BiFunction<OutputStream, String, OutputStream> OutputStreamToCryptedOutputStream = (s, k) ->
    {
        if((k == null) || k.isEmpty())
        {
            k = DEFAULT_KEY;
        }

        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedKey = digest.digest(k.getBytes(StandardCharsets.UTF_8));

            MessageDigest vectorDigest = MessageDigest.getInstance("MD5");
            byte[] vectorBytes = vectorDigest.digest(k.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec iv = new IvParameterSpec(vectorBytes);
            SecretKeySpec keySpec = new SecretKeySpec(encodedKey, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);

            return new GZIPOutputStream(new CipherOutputStream(s, cipher));
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeWrappedException(e);
        }
    };
}
