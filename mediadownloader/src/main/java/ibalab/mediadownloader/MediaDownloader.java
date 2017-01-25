package ibalab.mediadownloader;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaDownloader {
    
	static Logger log = Logger.getLogger(MediaDownloader.class.getName());
	
    ArrayList<RSSSource> sources;
    
    DocumentBuilderFactory dbf;
    DocumentBuilder db;
    
    final static DateFormat FROM_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
    final static DateFormat TO_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    final static Pattern UNICODE_PATTERN = Pattern.compile("\\\\u(\\p{XDigit}{4})");
    
    public MediaDownloader(){
        sources = new ArrayList<RSSSource>();
        dbf = DocumentBuilderFactory.newInstance();
        db = null;
    }
    
    boolean itemExists(String workDirectory, String fileName){
        String file = workDirectory + "\\" + fileName;
        return new File(file).exists();
    }
    
    void handleItems(RSSSource source){
        Document doc = null;
        try {
            doc = db.parse(source.getSource().openStream());
        }catch(IOException e){
            log.error("Can not open stream for " + source.getSource().toString() + " :\n" + e);
        }catch(SAXException e){
            log.error("Can not parse for " + source.getSource().toString() + " :\n" + e);
        }catch(NullPointerException e){
            log.error("Empty source:\n" + e);
        }
        if(doc != null){
        	log.debug("Scanning source RSS: " + source.getSource().toString());
            NodeList items = doc.getElementsByTagName("item");
            for(int i = 0; i < items.getLength(); i++){
                Node itemNode = items.item(i);
                Element el = (Element) itemNode;
                String title = el.getElementsByTagName("title").item(0).getTextContent();
                String link = el.getElementsByTagName("link").item(0).getTextContent();
                URL itemSource = null;
                try{
                    itemSource = new URL(link);
                }catch(MalformedURLException e){
                    log.error("Can not fetch item URL for " + source.getSource().toString() + " :\n" + e);
                }
                Date date = formatDate(el.getElementsByTagName("pubDate").item(0).getTextContent());
                if(itemExists(
                        source.getWorkDirectory(),
                        getItemFileName(
                        		source.getFilePrefix(),
                        		Integer.toString(link.hashCode()),
                        		date))) {
                	log.debug("No new items.");
                	return;
                }
                String text = el.getElementsByTagName(source.getContentTagName()).item(0).getTextContent();
                Item item = new Item(itemSource,date,title,formatString(text));
                log.info("New item: " + getItemFileName(source.getFilePrefix(), Integer.toString(link.hashCode()), item.getTimestamp()));
                writeItem(
                		source.getWorkDirectory(), 
                		getItemFileName(
                				source.getFilePrefix(), 
                				Integer.toString(link.hashCode()), 
                				item.getTimestamp()), 
                		item);
            }
        }
    }

    void writeItem(String workDirectory, String fileName, Item item){
        String file = workDirectory + "\\" + fileName;
        PrintWriter pw = null;
        try{
            pw = new PrintWriter(new FileWriter(new File(file)));
        }catch(IOException e){
            log.error("Can not write the item:\n" + e);
        }
        if(pw != null){
        	log.info("Writing new item: " + fileName);
            pw.print(unicodeToAscii(item.toJson().toString()));
            pw.close();
        }
    }
    
    String getItemFileName(String filePrefix, String uniqueID, Date timestamp){
    	return filePrefix + TO_DATE_FORMAT.format(timestamp) + "_" + uniqueID + ".json";
    }

    Date formatDate(String ts){
        Date date = null;
        try{
            date = FROM_DATE_FORMAT.parse(ts);
        }catch(ParseException e){
            log.error("Can not parse the timestamp:\n" + e);
        }
        return date;
    }

    String formatString(String text){
        return text.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", "")//remove HTML tags
                .replaceAll("&quot;","\"")//remove &quot; - becomes \" in JSON
                .replaceAll("\\n","");//remove \n
    }

    String unicodeToAscii(String s){
        Matcher unicodeMatcher = UNICODE_PATTERN.matcher(s);
        StringBuffer replacementBuf = new StringBuffer(s.length());
        while(unicodeMatcher.find()){
            unicodeMatcher.appendReplacement(replacementBuf,
                    String.valueOf((char) Integer.parseInt(unicodeMatcher.group(1),16))//convert unicode symbol
            );
        }
        unicodeMatcher.appendTail(replacementBuf);
        return replacementBuf.toString();
    }
    
    public void startMonitoring(){
        try{
            db = dbf.newDocumentBuilder();
        }catch(ParserConfigurationException e){
            log.error("Can not create document builder:\n" + e);
        }
        while(true)
            for(RSSSource s : sources) handleItems(s);
    }
    
    public void addSource(RSSSource source){
        sources.add(source);
    }
    
    static void addSources(MediaDownloader downloader, String configPath){
    	BufferedReader config = null;
    	try {
			config = new BufferedReader(new FileReader(new File(configPath)));
		} catch (FileNotFoundException e) {
			log.error("Can not read config file:\n" + e);
		}
    	
    	if(config != null){
    		
    		ArrayList<RSSSource> sources = new ArrayList<RSSSource>();
    		StringBuilder jsonConfig = new StringBuilder();
        	String temp = null;
        	
        	JsonParser parser = Json.createParser(config);
        	if(parser.hasNext()){//config is not empty
        		if(parser.next() == JsonParser.Event.START_ARRAY){//valid RSSSource array
        			while(parser.next() == JsonParser.Event.START_OBJECT){
        				String currentKey = null;
        				String source = null, directory = null, prefix = null, contentTag = null;
        				if(parser.next() == JsonParser.Event.KEY_NAME) currentKey = parser.getString(); else continue;
        				if(currentKey.equals("source") && parser.next() == JsonParser.Event.VALUE_STRING) source = parser.getString();
        				if(parser.next() == JsonParser.Event.KEY_NAME) currentKey = parser.getString();
        				if(currentKey.equals("directory") && parser.next() == JsonParser.Event.VALUE_STRING) directory = parser.getString();
        				if(parser.next() == JsonParser.Event.KEY_NAME) currentKey = parser.getString();
        				if(currentKey.equals("prefix") && parser.next() == JsonParser.Event.VALUE_STRING) prefix = parser.getString();
        				if(parser.next() == JsonParser.Event.KEY_NAME) currentKey = parser.getString();
        				if(currentKey.equals("content_tag") && parser.next() == JsonParser.Event.VALUE_STRING) contentTag = parser.getString();
        				
        				if(source == null || directory == null || prefix == null || contentTag == null) continue; // wrong array element
        				sources.add(new RSSSource(source, directory, prefix, contentTag));
        				parser.next(); //skip END_OBJECT
        			}
        		}
        	}
        	for(int i=0;i<sources.size();i++) {
        		log.debug("Source " + sources.get(i).getSource().toString() + " added.");
        		downloader.addSource(sources.get(i));
        	}
    	}
    }
    
    public static void main(String[] args) {
    	org.apache.log4j.PropertyConfigurator.configure("log4j.properties");
        MediaDownloader downloader = new MediaDownloader();
        addSources(downloader, args[0]);
        downloader.startMonitoring();
    	
    }
}
