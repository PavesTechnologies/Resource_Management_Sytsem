package com.dto;

import lombok.*;
import org.hibernate.annotations.Comment;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
public class ApiResponse<T> {
    private Boolean success;
    private String message;
    private T data;

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Boolean getSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public ApiResponse<T> getAPIResponse(Boolean result,String msg,T dataa)
    {
        ApiResponse<T> apiResponse=new ApiResponse<>();
        apiResponse.setSuccess(result);
        apiResponse.setMessage(msg);
        apiResponse.setData(dataa);
        return apiResponse;
    }
}