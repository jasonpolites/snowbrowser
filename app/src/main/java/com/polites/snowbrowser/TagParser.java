package com.polites.snowbrowser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagParser {

    private static String getTag(Reader reader) throws IOException {
        StringBuilder buffer = new StringBuilder();
        boolean found = false;
        while(!found) {
            int current = reader.read();
            char currentChr = (char) current;
            if(current == -1) {
                found = true;
            }
            if (currentChr == '>') {
                found = true;
            } else {
                buffer.append(currentChr);
            }
        }

        return buffer.toString();
    }

    /**
     * VERY specialized HTML parser that JUST looks for <link rel="canonical"...
      */
    public static String getCanonicalLink(InputStream in) throws IOException {
        // TODO: this will be slow if we don't check for HEAD tags
        InputStreamReader reader = new InputStreamReader(in);
        boolean found = false;
        while(!found) {
            int current =  reader.read();
            char currentChr = (char) current;
            if(current == -1) {
                found = true;
            }
            if(currentChr == '<') {
                String tagMatched = getTag(reader);
                String ciTagMatched = tagMatched.toLowerCase();
                if(ciTagMatched.startsWith("link")) {
                    // get "rel"
                    if(ciTagMatched.contains("canonical")) {
                        final String regex = "(?<=\\bhref=\")[^\"]*";
                        Pattern p = Pattern.compile(regex);
                        Matcher m = p.matcher(tagMatched);
                        if (m.find()) {
                            return m.group(0);
                        }
                    }
                } else if (tagMatched.startsWith("/head")) {
                    // We didn't find it.. abort
                    found = true;
                }
            }
        }
        return null;
    }
}
