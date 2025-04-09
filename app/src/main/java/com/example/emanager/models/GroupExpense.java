package com.example.emanager.models;

import java.util.ArrayList;

public class GroupExpense {
    private String groupId;
    private String groupName;
    private double totalAmount;
    private ArrayList<PersonExpense> personExpenses;

    public GroupExpense() {
        // Empty constructor for Firebase
    }

    public GroupExpense(String groupId, String groupName, double totalAmount, ArrayList<PersonExpense> personExpenses) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.totalAmount = totalAmount;
        this.personExpenses = personExpenses;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public ArrayList<PersonExpense> getPersonExpenses() {
        return personExpenses;
    }

    public void setPersonExpenses(ArrayList<PersonExpense> personExpenses) {
        this.personExpenses = personExpenses;
    }
}
