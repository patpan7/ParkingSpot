package com.example.parkingspot;


public class ReportItem {
    private String operator;
    private int ticketCount;

    public ReportItem(String operator, int ticketCount) {
        this.operator = operator;
        this.ticketCount = ticketCount;
    }

    public String getOperator() {
        return operator;
    }

    public int getTicketCount() {
        return ticketCount;
    }
}

