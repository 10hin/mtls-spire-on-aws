package com.example.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackendResponse {
    private String url;
    @JsonProperty("content-type")
    private String contentType;
    private String body;
    private int status;

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }
    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        result = prime * result + ((contentType == null) ? 0 : contentType.hashCode());
        result = prime * result + ((body == null) ? 0 : body.hashCode());
        result = prime * result + status;
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
        BackendResponse other = (BackendResponse) obj;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        if (contentType == null) {
            if (other.contentType != null)
                return false;
        } else if (!contentType.equals(other.contentType))
            return false;
        if (body == null) {
            if (other.body != null)
                return false;
        } else if (!body.equals(other.body))
            return false;
        if (status != other.status)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "RecursiveResponse [url=" + url + ", contentType=" + contentType + ", body=" + body + ", status="
                + status + "]";
    }
    
}
