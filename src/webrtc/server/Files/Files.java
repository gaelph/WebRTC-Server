/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server.Files;

import java.util.Locale;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.Optional;
import java.util.TimeZone;

/**
 *
 * @author gaelph
 */
public class Files {

    private static Files instance = null;

    private static final String[] INDEX_FILES = {"index.htm", "index.html", "index.php"};

    protected Files() {
    }

    public static Files getInstance() {
        if (instance == null) {
            synchronized (Files.class) {
                instance = new Files();
            }
        }

        return instance;
    }

    private String rootPath = null;

    public void setRoot(String rootPath) throws RootFolderNotFoundException {
        File file = new File(rootPath);

        if (!file.exists() || !file.isDirectory()) {
            throw new RootFolderNotFoundException(rootPath);
        }

        this.rootPath = rootPath;
    }

    public String getRoot() {
        return this.rootPath;
    }

    public String getLastModified(File file) throws FileNotFoundException, IOException {

        long lastMod = file.lastModified();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(lastMod);
    }

    public File getFile(String relativePath) throws NoRootFolderException, FileNotFoundException {
        if (this.rootPath == null) {
            throw new NoRootFolderException();
        }

        File file = new File(this.rootPath + relativePath);
        boolean found = false;

        if (relativePath.charAt(relativePath.length() - 1) == '/') {

            for (String index : INDEX_FILES) {
                file = new File(this.rootPath + relativePath + index);
                if (file.exists()) {
                    found = true;
                    break;
                }
            }
        }
        else if (file.exists()) {
            found = true;
        }

        if (found) {
            return file;
        }
        else {
            throw new FileNotFoundException(relativePath + " not found");
        }
    }

    public String getContent(File file) throws NoRootFolderException, FileNotFoundException, IOException {

        if (this.rootPath == null) {
            throw new NoRootFolderException();
        }
        final StringBuilder sb = new StringBuilder();

        BufferedReader inputStream = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

        String line;
        while ((line = inputStream.readLine()) != null) {
            sb.append(line).append('\n');
        }

        return sb.toString();
    }
}
