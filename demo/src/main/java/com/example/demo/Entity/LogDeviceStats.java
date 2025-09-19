package com.example.demo.Entity;

public class LogDeviceStats {
    private String deviceCode;
    private String macHc;
    private int countStatus1;
    private int countStatus2;
    private int countTotal;

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getMacHc() {
        return macHc;
    }

    public void setMacHc(String macHc) {
        this.macHc = macHc;
    }

    public int getCountStatus1() {
        return countStatus1;
    }

    public void setCountStatus1(int countStatus1) {
        this.countStatus1 = countStatus1;
    }

    public int getCountStatus2() {
        return countStatus2;
    }

    public void setCountStatus2(int countStatus2) {
        this.countStatus2 = countStatus2;
    }

    public int getCountTotal() {
        return countTotal;
    }

    public void setCountTotal(int countTotal) {
        this.countTotal = countTotal;
    }

    @Override
    public String toString() {
        return "deviceCode=" + deviceCode +
               ", macHc=" + macHc +
               ", countStatus1=" + countStatus1 +
               ", countStatus2=" + countStatus2 +
               ", countTotal=" + countTotal;
    }
}

