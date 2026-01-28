package com.dto;

import lombok.*;
import org.hibernate.annotations.Comment;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Component
public class ApiResponse<T> {
    private Boolean success;
    private String message;
    private T data;

    public ApiResponse<T> getAPIResponse(Boolean result,String msg,T dataa)
    {
        ApiResponse<T> apiResponse=new ApiResponse<>();
        apiResponse.setSuccess(result);
        apiResponse.setMessage(msg);
        apiResponse.setData(dataa);
        return apiResponse;
    }
}