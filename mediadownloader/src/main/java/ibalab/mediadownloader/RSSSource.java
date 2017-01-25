package ibalab.mediadownloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class RSSSource {
    private URL source = null;
    private String workDirectory;
    private String filePrefix;
    private String contentTagName;
    
    public RSSSource(String source,String workDirectory,String filePrefix, String contentTagName){
        try{
            this.source = new URL(source);
        }catch(MalformedURLException e){
            MediaDownloader.log.error("Can not open source " + source + ":\n" + e);
        }
        this.workDirectory = workDirectory;
        this.filePrefix = filePrefix;
        this.contentTagName = contentTagName;
        
        prepareWorkDirectory();
    }

    public URL getSource() {
        return source;
    }

    public String getWorkDirectory() {
        return workDirectory;
    }

    public String getFilePrefix() {
        return filePrefix;
    }

    public String getContentTagName() {
        return contentTagName;
    }
    
    private void prepareWorkDirectory(){
        File directory = new File(workDirectory);
        if(!directory.exists()) directory.mkdirs();
    }
}
