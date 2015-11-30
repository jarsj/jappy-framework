package com.crispy.net;

import com.maxmind.geoip2.DatabaseReader;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.TimeZone;

/**
 * Created by harsh on 11/15/15.
 */
public class Country {
    private DatabaseReader reader;
    private String code;
    private String ip;

    private Country(String url) throws IOException {
        File tmpFile = File.createTempFile("GeoIP-country", "dat");
        URL u = new URL(url);
        IOUtils.copy(u.openStream(), new FileOutputStream(tmpFile));
        reader = new DatabaseReader.Builder(tmpFile).build();
    }

    private Country(DatabaseReader reader) {
        this.reader = reader;
        this.code = null;
        this.ip = null;
    }

    public static Country withUrl(String url) {
        try {
            Country c = new Country(url);
            return c;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Country withIP(String ip) {
        Country ret = new Country(reader);
        ret.ip = ip.trim();
        return ret;
    }

    public Country withCode(String code) {
        Country ret = new Country(reader);
        ret.code = code;
        ret.ip = null;
        return ret;
    }

    public String getCode() {
        if (code == null) {
            try {
                code = reader.country(InetAddress.getByName(ip)).getCountry().getIsoCode();
            } catch (Exception e) {
                code = "--";
            }
        }
        return code;
    }

    public int timezoneOffset() {
        getCode();
        if (code.equals("--"))
            return 0;
        String[] zoneIds = com.ibm.icu.util.TimeZone.getAvailableIDs(code);
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
