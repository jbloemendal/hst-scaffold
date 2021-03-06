package org.onehippo;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TestUtils {

    final static Logger log = Logger.getLogger(TestUtils.class);

    final static Set<Pattern> IGNORE = new HashSet<Pattern>() {
        {
            this.add(Pattern.compile(HSTScaffold.SCAFFOLD_DIR_NAME));
            this.add(Pattern.compile(".*log$"));
            this.add(Pattern.compile("logs"));
            this.add(Pattern.compile("target"));
            this.add(Pattern.compile("storage"));
        }
    };

    public static Map<String, String> dirHash(File dir) {
        final Map<String, String> hashes = new HashMap<String, String>();
        for (File file : dir.listFiles()) {
            boolean ignore = false;
            for (Pattern pattern : IGNORE) {
                Matcher matcher = pattern.matcher(file.getName());
                if (matcher.matches()) {
                    ignore = true;
                    continue;
                }
            }
            if (ignore) {
                continue;
            }

            if (file.isDirectory()) {
                hashes.putAll(dirHash(file));
            } else if (file.isFile()) {
                try {
                    String md5 = DigestUtils.md5Hex(new BufferedInputStream(new FileInputStream(file)));
                    hashes.put(file.getAbsolutePath(), md5);
                } catch (IOException e) {
                    log.error("Error reading file hash.", e);
                }
            }
        }
        return hashes;
    }

    public static boolean hashesChanged(Map<String, String> dir1, Map<String, String> dir2) {
        for (Map.Entry<String, String> entry : dir1.entrySet()) {
            String filePath = entry.getKey();

            if (dir2.containsKey(filePath)) {
                if (!entry.getValue().equals(dir2.get(filePath))) {
                    log.debug(filePath+": checksum changed");
                    return true;
                }
            } else {
                log.debug("dir2 doesn't contain file "+filePath);
                return true;
            }
        }
        return false;
    }

    public static boolean dirChanged(Map<String, String> dir1, Map<String, String> dir2) {
        return hashesChanged(dir1, dir2) && hashesChanged(dir2, dir1);
    }

    public static Document loadXml(String fileName) throws ParserConfigurationException, IOException, SAXException {
        File componentsFile = new File(fileName);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(componentsFile);
        return doc;
    }

    public static String readFile(String fileName) throws IOException {
        Reader reader = new BufferedReader(new FileReader(new File(fileName)));

        StringBuilder templateBuilder = new StringBuilder();

        char[] buffer = new char[1024];
        while (reader.read(buffer) != -1) {
            templateBuilder.append(buffer);
        }

        return templateBuilder.toString();
    }
}
