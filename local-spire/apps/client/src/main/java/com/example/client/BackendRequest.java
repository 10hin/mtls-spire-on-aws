package com.example.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackendRequest {
    private String url;
    private String method;
    private String body;
    @JsonProperty("content-type")
    private String contentType;
    
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }
    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }
    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        result = prime * result + ((method == null) ? 0 : method.hashCode());
        result = prime * result + ((body == null) ? 0 : body.hashCode());
        result = prime * result + ((contentType == null) ? 0 : contentType.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BackendRequest other = (BackendRequest) obj;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        if (method == null) {
            if (other.method != null)
                return false;
        } else if (!method.equals(other.method))
            return false;
        if (body == null) {
            if (other.body != null)
                return false;
        } else if (!body.equals(other.body))
            return false;
        if (contentType == null) {
            if (other.contentType != null)
                return false;
        } else if (!contentType.equals(other.contentType))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "BackendRequest [url=" + url + ", method=" + method + ", body=" + body + ", contentType=" + contentType
                + "]";
    }

}
