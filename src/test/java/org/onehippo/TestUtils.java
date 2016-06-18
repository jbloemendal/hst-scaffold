package org.onehippo;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class TestUtils {

    public static Map<String, String> dirHash(File dir) {
        final Map<String, String> hashes = new HashMap<String, String>();
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                hashes.putAll(dirHash(file));
            } else if(file.isFile()) {
                try {
                    String md5 = DigestUtils.md5Hex(new BufferedInputStream(new FileInputStream(file)));
                    hashes.put(file.getAbsolutePath(), md5);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return hashes;
    }

    public static boolean hashesContained(Map<String, String> dir1, Map<String, String> dir2) {
        for (Map.Entry<String, String> entry : dir1.entrySet()) {
            String filePath = entry.getKey();

            if (!dir2.containsKey(filePath)) {
                return false;
            }
            if (!entry.getValue().equals(dir2.get(filePath))) {
                return false;
            }
        }
        return true;
    }

    public static boolean dirChanged(Map<String, String> dir1, Map<String, String> dir2) {
        if (dir1.size() != dir2.size()) {
            return false;
        }

        return hashesContained(dir1, dir2) && hashesContained(dir2, dir1);
    }

}
