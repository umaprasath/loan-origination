package com.loanorigination.common.event;

import com.loanorigination.common.dto.BureauResponse;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class CreditBureauEvent {
    private String eventId;
    private String requestId;
    private String bureauName;
    private BureauResponse response;
    private LocalDateTime timestamp;
    
    // Explicit constructor for Lombok compatibility
    public CreditBureauEvent(String eventId, String requestId, String bureauName, 
                            BureauResponse response, LocalDateTime timestamp) {
        this.eventId = eventId;
        this.requestId = requestId;
        this.bureauName = bureauName;
        this.response = response;
        this.timestamp = timestamp;
    }
    
    // Explicit getters and setters for Lombok compatibility
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getBureauName() {
        return bureauName;
    }
    
    public void setBureauName(String bureauName) {
        this.bureauName = bureauName;
    }
    
    public BureauResponse getResponse() {
        return response;
    }
    
    public void setResponse(BureauResponse response) {
        this.response = response;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

