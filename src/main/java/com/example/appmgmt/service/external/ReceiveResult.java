package com.example.appmgmt.service.external;

/** IF02／IF04 の応答（12. 4.4 節）。 */
public class ReceiveResult {
    private final int httpStatus;
    private final String status;
    private final String errorCode;
    private final String applicationNo;
    private final String newStatusCd;
    private final String message;

    public ReceiveResult(int httpStatus, String status, String errorCode, String applicationNo, String newStatusCd, String message) {
        this.httpStatus = httpStatus;
        this.status = status;
        this.errorCode = errorCode;
        this.applicationNo = applicationNo;
        this.newStatusCd = newStatusCd;
        this.message = message;
    }

    public static ReceiveResult ok(String applicationNo, String newStatusCd, String message) {
        return new ReceiveResult(200, "OK", null, applicationNo, newStatusCd, message);
    }

    public static ReceiveResult duplicate(String applicationNo, String statusCd) {
        return new ReceiveResult(200, "OK", "E200", applicationNo, statusCd, "受信済みの結果です（重複受信）。");
    }

    public static ReceiveResult error(int http, String code, String applicationNo, String statusCd, String message) {
        return new ReceiveResult(http, "ERROR", code, applicationNo, statusCd, message);
    }

    public int getHttpStatus() { return httpStatus; }
    public String getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public String getApplicationNo() { return applicationNo; }
    public String getNewStatusCd() { return newStatusCd; }
    public String getMessage() { return message; }
}
