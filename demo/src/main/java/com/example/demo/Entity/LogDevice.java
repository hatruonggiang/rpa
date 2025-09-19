package com.example.demo.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "tbl_log_device")
public class LogDevice {
    @Id
    private String _id;
    private String serial;
    private String deviceCode;
    private String deviceType;
    private String macHc;
    private String fwVersion;
    private int statusTestCase;
    private int type;
    private String cmd;
    private String rqi;
    private String log;
    private Instant createdTime;
    private Instant updatedTime;
    private String _class;

    // getter setter
    public String get_id() { return _id; }
    public void set_id(String _id) { this._id = _id; }

    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }

    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getMacHc() { return macHc; }
    public void setMacHc(String macHc) { this.macHc = macHc; }

    public String getFwVersion() { return fwVersion; }
    public void setFwVersion(String fwVersion) { this.fwVersion = fwVersion; }

    public int getStatusTestCase() { return statusTestCase; }
    public void setStatusTestCase(int statusTestCase) { this.statusTestCase = statusTestCase; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public String getCmd() { return cmd; }
    public void setCmd(String cmd) { this.cmd = cmd; }

    public String getRqi() { return rqi; }
    public void setRqi(String rqi) { this.rqi = rqi; }

    public String getLog() { return log; }
    public void setLog(String log) { this.log = log; }

    public Instant getCreatedTime() { return createdTime; }
    public void setCreatedTime(Instant createdTime) { this.createdTime = createdTime; }

    public Instant getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Instant updatedTime) { this.updatedTime = updatedTime; }

    public String get_class() { return _class; }
    public void set_class(String _class) { this._class = _class; }

    @Override
    public String toString() {
        return "LogDevice{" +
                "id='" + _id + '\'' +
                ", serial='" + serial + '\'' +
                ", deviceCode='" + deviceCode + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", macHc='" + macHc + '\'' +
                ", fwVersion='" + fwVersion + '\'' +
                ", statusTestCase='" + statusTestCase + '\'' +
                ", type='" + type + '\'' +
                ", cmd='" + cmd + '\'' +
                ", rqi='" + rqi + '\'' +
                ", log='" + log + '\'' +
                ", createdTime='" + createdTime + '\'' +
                ", updatedTime='" + updatedTime + '\'' +
                '}';
    }
}
