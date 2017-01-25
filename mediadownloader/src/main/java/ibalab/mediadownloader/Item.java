package ibalab.mediadownloader;

import javax.json.Json;
import javax.json.JsonObject;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Item {
    URL source;
    Date timestamp;
    String topic;
    String text;
    static final DateFormat TO_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    
    public Item(URL source, Date timestamp, String topic, String text){
        this.source = source;
        this.timestamp = timestamp;
        this.topic = topic;
        this.text = text;
    }

    public URL getSource() {
        return source;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getTopic() {
        return topic;
    }

    public String getText() {
        return text;
    }

    public void setSource(URL source) {
        this.source = source;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setText(String text) {
        this.text = text;
    }
    
    public JsonObject toJson(){
        JsonObject jsonItem = Json.createObjectBuilder()
                .add("source",source == null ? "" : source.toString())
                .add("timestamp", TO_DATE_FORMAT.format(timestamp))
                .add("topic",topic)
                .add("text",text)
                .build();
        return jsonItem;
    }
}
