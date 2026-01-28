package com.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
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