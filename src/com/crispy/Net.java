package com.crispy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;

import com.maxmind.geoip.LookupService;

public class Net {
	private static LookupService IP_SERVICE;
	
	public static void init() throws FileNotFoundException, IOException {
		File tmpFile = File.createTempFile("GeoIP", "dat");
		IOUtils.copy(Net.class.getResourceAsStream("/GeoIP.dat"),
				new FileOutputStream(tmpFile));
		
		IP_SERVICE = new LookupService(tmpFile);
	}
	
	public static String countryCodeFromIP(String ip) {
		ip = ip.trim();
		if (ip.split("\\.").length == 4)
			return IP_SERVICE.getCountry(ip).getCode();
		else 
			return IP_SERVICE.getCountryV6(ip).getCode();
	}
	
	public static String[] countries() {
		return IP_SERVICE.getCountrycode();
	}
	
	/**
	 * Return the millisecond offset for the country as computed by averaging.
	 * 
	 * @param countryCode
	 * @return
	 */
	public static int timezoneOffset(String countryCode) {
		if (countryCode.equals("--"))
			return 0;
		String[] zoneIds = com.ibm.icu.util.TimeZone.getAvailableIDs(countryCode);
		int total = 0;
		int count = 0;
		for (String zone : zoneIds) {
			TimeZone tz = TimeZone.getTimeZone(zone);
			total += tz.getRawOffset();
			count++;
		}
		if (count == 0)
			return 0;
		return total / count;
	}
}
