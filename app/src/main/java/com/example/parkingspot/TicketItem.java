package com.example.parkingspot;

public class TicketItem {
    private String operator;
    private String plate;
    private String datetime;

    public TicketItem(String operator, String plate, String datetime) {
        this.operator = operator;
        this.plate = plate;
        this.datetime = datetime;
    }

    public String getOperator() {
        return operator;
    }

    public String getPlate() {
        return plate;
    }

    public String getDatetime() {
        return datetime;
    }
}
