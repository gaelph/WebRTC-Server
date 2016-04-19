/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server.HTTP;

import java.time.*;
import java.time.format.*;

/**
 *
 * @author gaelph
 */
public class HTTPCookie {
    
    public String name;
    public String value;
    public String domain;
    public String path;
    
    private ZonedDateTime expire;
    
    public boolean secure = false;
    public boolean httpOnly = false;
    
    public HTTPCookie(String name, String value) {
        this.name = name.trim();
        this.value = value;
    }
    
    public HTTPCookie(String name, String value, String domain, String path) {
        this.name = name.trim();
        this.value = value;
        this.domain = domain;
        this.path = path;
    }
    
    public HTTPCookie(String name, String value, String domain, String path, ZonedDateTime expire) {
        this.name = name.trim();
        this.value = value;
        this.domain = domain;
        this.path = path;
        this.expire = expire;
    }
    
    public void setExpire(ZonedDateTime expire) {
        this.expire = expire;
    }
    
    public ZonedDateTime getExpire() {
        return expire;
    }
    
    @Override
    public String toString() {
        String result = "";
        
        result += name + "=" + value + "; ";
        
        if (domain != null) {
            result += "Domain=" + domain + "; ";
        }
        
        if (path != null) {
            result += "Path=" + path + "; ";
        }
        
        if (expire != null) {
            String expireString = expire.format(DateTimeFormatter.RFC_1123_DATE_TIME);
            result += "Expire=" + expireString + "; ";
        }
        
        if (secure) {
            result += "secure; ";
        }
        
        if (httpOnly) {
            result += "httpOnly";
        }
        
        return result;
    }
}
